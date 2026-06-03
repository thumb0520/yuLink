package com.easyconnect.nas.protocol.ftp;

import com.easyconnect.nas.data.model.ConnectionInfo;
import com.easyconnect.nas.data.model.NasFile;
import com.easyconnect.nas.protocol.ProtocolException;
import com.easyconnect.nas.protocol.ProtocolManager;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FtpProtocolManager implements ProtocolManager {
    private FTPClient ftpClient;
    private ConnectionInfo connectionInfo;
    private boolean connected = false;

    @Override
    public void connect(ConnectionInfo info) throws ProtocolException {
        this.connectionInfo = info;
        try {
            if (info.isUseFtps()) {
                ftpClient = new FTPSClient();
            } else {
                ftpClient = new FTPClient();
            }

            ftpClient.setConnectTimeout(30000);
            ftpClient.connect(info.getHost(), info.getPort());

            int reply = ftpClient.getReplyCode();
            if (!org.apache.commons.net.ftp.FTPReply.isPositiveCompletion(reply)) {
                throw new ProtocolException("FTP server refused connection: " + reply);
            }

            if (!ftpClient.login(info.getUsername(), info.getPassword())) {
                throw new ProtocolException("FTP login failed");
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setBufferSize(8192);

            connected = true;
        } catch (IOException e) {
            throw new ProtocolException("Failed to connect to FTP server", e);
        }
    }

    @Override
    public void disconnect() {
        if (ftpClient != null && connected) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                // Ignore
            }
            connected = false;
        }
    }

    @Override
    public boolean isConnected() {
        return connected && ftpClient != null && ftpClient.isConnected();
    }

    @Override
    public List<NasFile> listFiles(String path) throws ProtocolException {
        checkConnected();
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(path);
            List<NasFile> result = new ArrayList<>();

            for (FTPFile file : ftpFiles) {
                if (file.getName().equals(".") || file.getName().equals("..")) {
                    continue;
                }

                NasFile nasFile = new NasFile();
                nasFile.setName(file.getName());
                nasFile.setFullPath(path + "/" + file.getName());
                nasFile.setDirectory(file.isDirectory());
                nasFile.setSize(file.getSize());
                nasFile.setLastModified(file.getTimestamp() != null ? file.getTimestamp().getTime() : new Date());
                nasFile.setPermissions(file.toString());
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
            OutputStream outputStream = ftpClient.storeFileStream(remotePath);
            if (outputStream == null) {
                throw new ProtocolException("Failed to open remote file for writing");
            }

            byte[] buffer = new byte[8192];
            long transferred = 0;
            int bytesRead;

            while ((bytesRead = source.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    listener.onCancelled();
                    outputStream.close();
                    ftpClient.completePendingCommand();
                    return;
                }

                outputStream.write(buffer, 0, bytesRead);
                transferred += bytesRead;
                listener.onProgress(transferred, totalBytes);
            }

            outputStream.close();
            if (!ftpClient.completePendingCommand()) {
                throw new ProtocolException("Failed to complete upload");
            }
        } catch (IOException e) {
            throw new ProtocolException("Upload failed", e);
        }
    }

    @Override
    public void downloadFile(String remotePath, OutputStream destination,
                             ProgressListener listener) throws ProtocolException {
        checkConnected();
        try {
            FTPFile file = ftpClient.mlistFile(remotePath);
            long totalBytes = file != null ? file.getSize() : 0;

            InputStream inputStream = ftpClient.retrieveFileStream(remotePath);
            if (inputStream == null) {
                throw new ProtocolException("Failed to open remote file for reading");
            }

            byte[] buffer = new byte[8192];
            long transferred = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    listener.onCancelled();
                    inputStream.close();
                    ftpClient.completePendingCommand();
                    return;
                }

                destination.write(buffer, 0, bytesRead);
                transferred += bytesRead;
                listener.onProgress(transferred, totalBytes);
            }

            inputStream.close();
            if (!ftpClient.completePendingCommand()) {
                throw new ProtocolException("Failed to complete download");
            }
        } catch (IOException e) {
            throw new ProtocolException("Download failed", e);
        }
    }

    @Override
    public void deleteFile(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            if (!ftpClient.deleteFile(remotePath)) {
                throw new ProtocolException("Failed to delete file: " + remotePath);
            }
        } catch (IOException e) {
            throw new ProtocolException("Delete failed", e);
        }
    }

    @Override
    public void deleteDirectory(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            // Remove all files in directory first
            FTPFile[] files = ftpClient.listFiles(remotePath);
            if (files != null) {
                for (FTPFile file : files) {
                    if (file.getName().equals(".") || file.getName().equals("..")) continue;
                    String filePath = remotePath + "/" + file.getName();
                    if (file.isDirectory()) {
                        deleteDirectory(filePath);
                    } else {
                        ftpClient.deleteFile(filePath);
                    }
                }
            }
            if (!ftpClient.removeDirectory(remotePath)) {
                throw new ProtocolException("Failed to delete directory: " + remotePath);
            }
        } catch (IOException e) {
            throw new ProtocolException("Delete directory failed", e);
        }
    }

    @Override
    public void createDirectory(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            if (!ftpClient.makeDirectory(remotePath)) {
                throw new ProtocolException("Failed to create directory: " + remotePath);
            }
        } catch (IOException e) {
            throw new ProtocolException("Create directory failed", e);
        }
    }

    @Override
    public void renameFile(String oldPath, String newPath) throws ProtocolException {
        checkConnected();
        try {
            if (!ftpClient.rename(oldPath, newPath)) {
                throw new ProtocolException("Failed to rename file");
            }
        } catch (IOException e) {
            throw new ProtocolException("Rename failed", e);
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
            FTPFile file = ftpClient.mlistFile(path);
            if (file == null) {
                throw new ProtocolException("File not found: " + path);
            }

            NasFile nasFile = new NasFile();
            nasFile.setName(file.getName());
            nasFile.setFullPath(path);
            nasFile.setDirectory(file.isDirectory());
            nasFile.setSize(file.getSize());
            nasFile.setLastModified(file.getTimestamp() != null ? file.getTimestamp().getTime() : new Date());
            nasFile.setPermissions(file.toString());
            return nasFile;
        } catch (IOException e) {
            throw new ProtocolException("Failed to get file properties", e);
        }
    }

    @Override
    public InputStream openFileStream(String remotePath) throws ProtocolException {
        checkConnected();
        try {
            InputStream stream = ftpClient.retrieveFileStream(remotePath);
            if (stream == null) {
                throw new ProtocolException("Failed to open file stream: " + remotePath);
            }
            return stream;
        } catch (IOException e) {
            throw new ProtocolException("Failed to open file stream", e);
        }
    }

    private void checkConnected() throws ProtocolException {
        if (!isConnected()) {
            throw new ProtocolException("Not connected to FTP server");
        }
    }
}
