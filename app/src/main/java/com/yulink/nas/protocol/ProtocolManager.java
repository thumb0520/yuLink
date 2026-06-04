package com.yulink.nas.protocol;

import com.yulink.nas.data.model.ConnectionInfo;
import com.yulink.nas.data.model.NasFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface ProtocolManager {

    void connect(ConnectionInfo info) throws ProtocolException;
    void disconnect();
    boolean isConnected();

    List<NasFile> listFiles(String path) throws ProtocolException;

    void uploadFile(InputStream source, String remotePath, long totalBytes,
                    ProgressListener listener) throws ProtocolException;

    void downloadFile(String remotePath, OutputStream destination,
                      ProgressListener listener) throws ProtocolException;

    void deleteFile(String remotePath) throws ProtocolException;
    void deleteDirectory(String remotePath) throws ProtocolException;
    void createDirectory(String remotePath) throws ProtocolException;
    void renameFile(String oldPath, String newPath) throws ProtocolException;
    void moveFile(String sourcePath, String destPath) throws ProtocolException;
    NasFile getFileProperties(String path) throws ProtocolException;

    InputStream openFileStream(String remotePath) throws ProtocolException;

    interface ProgressListener {
        void onProgress(long bytesTransferred, long totalBytes);
        void onCancelled();
    }
}
