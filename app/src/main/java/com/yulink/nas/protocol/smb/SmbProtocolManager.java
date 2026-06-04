package com.yulink.nas.protocol.smb;

import com.yulink.nas.data.model.ConnectionInfo;
import com.yulink.nas.data.model.NasFile;
import com.yulink.nas.protocol.ProtocolException;
import com.yulink.nas.protocol.ProtocolManager;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

public class SmbProtocolManager implements ProtocolManager {
    private SMBClient smbClient;
    private Connection connection;
    private Session session;
    private DiskShare share;
    private ConnectionInfo connectionInfo;
    private boolean connected = false;

    @Override
    public void connect(ConnectionInfo info) throws ProtocolException {
        this.connectionInfo = info;
        try {
            smbClient = new SMBClient();
            connection = smbClient.connect(info.getHost(), info.getPort());

            AuthenticationContext ac = new AuthenticationContext(
                    info.getUsername(),
                    info.getPassword().toCharArray(),
                    ""
            );
            session = connection.authenticate(ac);

            String shareName = info.getShareName();
            if (shareName == null || shareName.isEmpty()) {
                shareName = "C$";
            }

            share = (DiskShare) session.connectShare(shareName);
            connected = true;
        } catch (Exception e) {
            throw new ProtocolException("Failed to connect to SMB server", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (share != null) share.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
            if (smbClient != null) smbClient.close();
        } catch (Exception e) {
            // Ignore
        }
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected && connection != null && connection.isConnected();
    }

    @Override
    public List<NasFile> listFiles(String path) throws ProtocolException {
        checkConnected();
        try {
            List<FileIdBothDirectoryInformation> entries = share.list(path);
            List<NasFile> result = new ArrayList<>();

            for (FileIdBothDirectoryInformation entry : entries) {
                String name = entry.getFileName();
                if (name.equals(".") || name.equals("..")) {
                    continue;
                }

                NasFile nasFile = new NasFile();
                nasFile.setName(name);

                String fullPath = path;
                if (!fullPath.endsWith("\\")) {
                    fullPath += "\\";
                }
                fullPath += name;
                nasFile.setFullPath(fullPath);

                boolean isDir = (entry.getFileAttributes() & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue()) != 0;
                nasFile.setDirectory(isDir);
                nasFile.setSize(entry.getEndOfFile());
                nasFile.setLastModified(entry.getLastWriteTime().toDate());
                result.add(nasFile);
            }

            return result;
        } catch (Exception e) {
            throw new ProtocolException("Failed to list files", e);
        }
    }

    @Override
    public void uploadFile(InputStream source, String remotePath, long totalBytes,
                           ProgressListener listener) throws ProtocolException {
        checkConnected();
        try {
            String smbPath = normalizePath(remotePath);
            File remoteFile = share.openFile(smbPath,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    null);

            try {
                OutputStream outputStream = remoteFile.getOutputStream();
                byte[] buffer = new byte[8192];
                long transferred = 0;
                int bytesRead;

                while ((bytesRead = source.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        listener.onCancelled();
                        return;
                    }

                    outputStream.write(buffer, 0, bytesRead);
                    transferred += bytesRead;
                    listener.onProgress(transferred, totalBytes);
                }

                outputStream.flush();
            } finally {
                remoteFile.close();
            }
        } catch (Exception e) {
            throw new ProtocolException("Upload failed", e);
        }
    }

    @Override
    public void downloadFile(String remotePath, OutputStream destination,
                             ProgressListener listener) throws ProtocolException {
        checkConnected();
        try {
            String smbPath = normalizePath(remotePath);
            File remoteFile = share.openFile(smbPath,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_OPEN,
                    null);

            try {
                InputStream inputStream = remoteFile.getInputStream();
                FileAllInformation info = remoteFile.getFileInformation();
                long totalBytes = info.getStandardInformation().getEndOfFile();

                byte[] buffer = new byte[8192];
                long transferred = 0;
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        listener.onCancelled();
                        return;
                    }

                    destination.write(buffer, 0, bytesRead);
                    transferred += bytesRead;
                    listener.onProgress(transferred, totalBytes);
                }

                destination.flush();
            } finally {
                remoteFile.close();
            }
        } catch (Exception e) {
            throw new ProtocolException("Download failed", e);
        }
    }

    @Override
    public void deleteFile(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            share.rm(normalizePath(remotePath));
        } catch (Exception e) {
            throw new ProtocolException("Failed to delete file: " + remotePath, e);
        }
    }

    @Override
    public void deleteDirectory(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            // Remove all files in directory first
            List<FileIdBothDirectoryInformation> entries = share.list(remotePath);
            for (FileIdBothDirectoryInformation entry : entries) {
                String name = entry.getFileName();
                if (name.equals(".") || name.equals("..")) continue;
                String filePath = remotePath + "\\" + name;
                boolean isDir = (entry.getFileAttributes() & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue()) != 0;
                if (isDir) {
                    deleteDirectory(filePath);
                } else {
                    share.rm(filePath);
                }
            }
            share.rmdir(normalizePath(remotePath), false);
        } catch (Exception e) {
            throw new ProtocolException("Failed to delete directory: " + remotePath, e);
        }
    }

    @Override
    public void createDirectory(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            share.mkdir(normalizePath(remotePath));
        } catch (Exception e) {
            throw new ProtocolException("Failed to create directory: " + remotePath, e);
        }
    }

    @Override
    public void renameFile(String oldPath, String newPath) throws ProtocolException {
        checkConnected();
        try {
            String smbOldPath = normalizePath(oldPath);
            File file = share.openFile(smbOldPath,
                    EnumSet.of(AccessMask.GENERIC_ALL),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ, SMB2ShareAccess.FILE_SHARE_WRITE),
                    SMB2CreateDisposition.FILE_OPEN,
                    null);
            try {
                file.rename(normalizePath(newPath));
            } finally {
                file.close();
            }
        } catch (Exception e) {
            throw new ProtocolException("Failed to rename file", e);
        }
    }

    @Override
    public void moveFile(String sourcePath, String destPath) throws ProtocolException {
        renameFile(sourcePath, destPath);
    }

    @Override
    public NasFile getFileProperties(String path) throws ProtocolException {
        checkConnected();
        try {
            String smbPath = normalizePath(path);
            File file = share.openFile(smbPath,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_OPEN,
                    null);

            try {
                FileAllInformation info = file.getFileInformation();
                NasFile nasFile = new NasFile();
                nasFile.setName(path.substring(path.lastIndexOf('\\') + 1));
                nasFile.setFullPath(path);
                nasFile.setDirectory(info.getBasicInformation().getFileAttributes() != 0 &&
                        (info.getBasicInformation().getFileAttributes() & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue()) != 0);
                nasFile.setSize(info.getStandardInformation().getEndOfFile());
                nasFile.setLastModified(info.getBasicInformation().getLastWriteTime().toDate());
                return nasFile;
            } finally {
                file.close();
            }
        } catch (Exception e) {
            throw new ProtocolException("Failed to get file properties", e);
        }
    }

    @Override
    public InputStream openFileStream(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            String smbPath = normalizePath(remotePath);
            File remoteFile = share.openFile(smbPath,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_OPEN,
                    null);
            return new SmbInputStream(remoteFile);
        } catch (Exception e) {
            throw new ProtocolException("Failed to open file stream", e);
        }
    }

    private String normalizePath(String path) {
        if (path == null) return "";
        return path.replace("/", "\\");
    }

    private void checkConnected() throws ProtocolException {
        if (!isConnected()) {
            throw new ProtocolException("Not connected to SMB server");
        }
    }

    private static class SmbInputStream extends InputStream {
        private final File file;
        private final InputStream inputStream;
        private boolean closed = false;

        SmbInputStream(File file) throws IOException {
            this.file = file;
            this.inputStream = file.getInputStream();
        }

        @Override
        public int read() throws IOException {
            if (closed) throw new IOException("Stream closed");
            return inputStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) throw new IOException("Stream closed");
            return inputStream.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                inputStream.close();
                file.close();
            }
        }
    }
}
