package com.easyconnect.nas.protocol.sftp;

import com.easyconnect.nas.data.model.ConnectionInfo;
import com.easyconnect.nas.data.model.NasFile;
import com.easyconnect.nas.protocol.ProtocolException;
import com.easyconnect.nas.protocol.ProtocolManager;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SftpProtocolManager implements ProtocolManager {
    private SSHClient sshClient;
    private SFTPClient sftpClient;
    private ConnectionInfo connectionInfo;
    private boolean connected = false;

    @Override
    public void connect(ConnectionInfo info) throws ProtocolException {
        this.connectionInfo = info;
        try {
            sshClient = new SSHClient();
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.setConnectTimeout(30000);
            sshClient.connect(info.getHost(), info.getPort());

            if (info.getSshKeyPath() != null && !info.getSshKeyPath().isEmpty()) {
                sshClient.authPublickey(info.getUsername(), info.getSshKeyPath());
            } else {
                sshClient.authPassword(info.getUsername(), info.getPassword());
            }

            sftpClient = sshClient.newSFTPClient();
            sftpClient.getFileTransfer().setPreserveAttributes(false);

            connected = true;
        } catch (IOException e) {
            throw new ProtocolException("Failed to connect to SFTP server", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (sftpClient != null) {
                sftpClient.close();
            }
            if (sshClient != null) {
                sshClient.disconnect();
            }
        } catch (IOException e) {
            // Ignore
        }
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected && sshClient != null && sshClient.isConnected();
    }

    @Override
    public List<NasFile> listFiles(String path) throws ProtocolException {
        checkConnected();
        try {
            List<RemoteResourceInfo> resources = sftpClient.ls(path);
            List<NasFile> result = new ArrayList<>();

            for (RemoteResourceInfo resource : resources) {
                String name = resource.getName();
                if (name.equals(".") || name.equals("..")) {
                    continue;
                }

                NasFile nasFile = new NasFile();
                nasFile.setName(name);
                nasFile.setFullPath(path + "/" + name);
                nasFile.setDirectory(resource.isDirectory());
                nasFile.setSize(resource.getAttributes().getSize());
                nasFile.setLastModified(new Date(resource.getAttributes().getMtime() * 1000L));
                nasFile.setPermissions(resource.getAttributes().getPermissions() != null ?
                        resource.getAttributes().getPermissions().toString() : null);
                result.add(nasFile);
            }

            return result;
        } catch (IOException e) {
            throw new ProtocolException("Failed to list files", e);
        }
    }

    @Override
    public void uploadFile(InputStream source, String remotePath, long totalBytes,
                           ProgressListener listener) throws ProtocolException {
        checkConnected();
        try {
            RemoteFile remoteFile = sftpClient.open(remotePath,
                    EnumSet.of(OpenMode.CREAT, OpenMode.TRUNC, OpenMode.WRITE));

            byte[] buffer = new byte[8192];
            long transferred = 0;
            int bytesRead;
            long offset = 0;

            while ((bytesRead = source.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    listener.onCancelled();
                    remoteFile.close();
                    return;
                }

                remoteFile.write(offset, buffer, 0, bytesRead);
                offset += bytesRead;
                transferred += bytesRead;
                listener.onProgress(transferred, totalBytes);
            }

            remoteFile.close();
        } catch (IOException e) {
            throw new ProtocolException("Upload failed", e);
        }
    }

    @Override
    public void downloadFile(String remotePath, OutputStream destination,
                             ProgressListener listener) throws ProtocolException {
        checkConnected();
        try {
            long totalBytes = 0;
            try {
                totalBytes = sftpClient.stat(remotePath).getSize();
            } catch (IOException e) {
                // Ignore, totalBytes will be 0
            }

            RemoteFile remoteFile = sftpClient.open(remotePath, EnumSet.of(OpenMode.READ));
            byte[] buffer = new byte[8192];
            long transferred = 0;
            long offset = 0;
            int bytesRead;

            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    listener.onCancelled();
                    remoteFile.close();
                    return;
                }

                bytesRead = remoteFile.read(offset, buffer, 0, buffer.length);
                if (bytesRead <= 0) break;

                destination.write(buffer, 0, bytesRead);
                offset += bytesRead;
                transferred += bytesRead;
                listener.onProgress(transferred, totalBytes);
            }

            remoteFile.close();
        } catch (IOException e) {
            throw new ProtocolException("Download failed", e);
        }
    }

    @Override
    public void deleteFile(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            sftpClient.rm(remotePath);
        } catch (IOException e) {
            throw new ProtocolException("Failed to delete file: " + remotePath, e);
        }
    }

    @Override
    public void deleteDirectory(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            // Remove all files in directory first
            List<RemoteResourceInfo> resources = sftpClient.ls(remotePath);
            for (RemoteResourceInfo resource : resources) {
                String name = resource.getName();
                if (name.equals(".") || name.equals("..")) continue;
                String filePath = remotePath + "/" + name;
                if (resource.isDirectory()) {
                    deleteDirectory(filePath);
                } else {
                    sftpClient.rm(filePath);
                }
            }
            sftpClient.rmdir(remotePath);
        } catch (IOException e) {
            throw new ProtocolException("Failed to delete directory: " + remotePath, e);
        }
    }

    @Override
    public void createDirectory(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            sftpClient.mkdir(remotePath);
        } catch (IOException e) {
            throw new ProtocolException("Failed to create directory: " + remotePath, e);
        }
    }

    @Override
    public void renameFile(String oldPath, String newPath) throws ProtocolException {
        checkConnected();
        try {
            sftpClient.rename(oldPath, newPath);
        } catch (IOException e) {
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
            var attrs = sftpClient.stat(path);
            NasFile nasFile = new NasFile();
            nasFile.setName(path.substring(path.lastIndexOf('/') + 1));
            nasFile.setFullPath(path);
            nasFile.setDirectory(attrs.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY);
            nasFile.setSize(attrs.getSize());
            nasFile.setLastModified(new Date(attrs.getMtime() * 1000L));
            nasFile.setPermissions(attrs.getPermissions() != null ?
                    attrs.getPermissions().toString() : null);
            return nasFile;
        } catch (IOException e) {
            throw new ProtocolException("Failed to get file properties", e);
        }
    }

    @Override
    public InputStream openFileStream(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            RemoteFile remoteFile = sftpClient.open(remotePath, EnumSet.of(OpenMode.READ));
            return new RemoteFileInputStream(remoteFile);
        } catch (IOException e) {
            throw new ProtocolException("Failed to open file stream", e);
        }
    }

    private void checkConnected() throws ProtocolException {
        if (!isConnected()) {
            throw new ProtocolException("Not connected to SFTP server");
        }
    }

    private static class RemoteFileInputStream extends InputStream {
        private final RemoteFile remoteFile;
        private long offset = 0;
        private boolean closed = false;

        RemoteFileInputStream(RemoteFile remoteFile) {
            this.remoteFile = remoteFile;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int result = read(b, 0, 1);
            return result == -1 ? -1 : b[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) throw new IOException("Stream closed");
            int bytesRead = remoteFile.read(offset, b, off, len);
            if (bytesRead > 0) {
                offset += bytesRead;
            }
            return bytesRead;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                remoteFile.close();
            }
        }
    }
}
