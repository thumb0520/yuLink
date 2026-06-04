package com.yulink.nas.transfer;

import com.yulink.nas.data.model.ConnectionInfo;
import com.yulink.nas.data.model.TransferTask;
import com.yulink.nas.protocol.ProtocolException;
import com.yulink.nas.protocol.ProtocolFactory;
import com.yulink.nas.protocol.ProtocolManager;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class TransferWorker implements Runnable {
    private static final String TAG = "TransferWorker";
    private final TransferTask task;
    private final TransferCallback callback;
    private final Context context;
    private ProtocolManager protocolManager;

    public TransferWorker(TransferTask task, TransferCallback callback, Context context) {
        this.task = task;
        this.callback = callback;
        this.context = context.getApplicationContext();
    }

    @Override
    public void run() {
        Log.d(TAG, "Transfer started: " + task.getFileName() + " direction=" + task.getDirection());
        try {
            task.setStatus(TransferTask.Status.RUNNING);
            callback.onTransferStarted(task);

            // Get connection info and create protocol manager
            Log.d(TAG, "Loading connection info for id=" + task.getConnectionId());
            ConnectionInfo connectionInfo = getConnectionInfo(task.getConnectionId());
            Log.d(TAG, "Connecting to " + connectionInfo.getHost() + " share=" + connectionInfo.getShareName());
            protocolManager = ProtocolFactory.create(connectionInfo.getProtocol());
            protocolManager.connect(connectionInfo);
            Log.d(TAG, "Connected successfully");

            if (task.getDirection() == TransferTask.Direction.UPLOAD) {
                doUpload();
            } else {
                Log.d(TAG, "Starting download: " + task.getSourcePath() + " -> " + task.getDestinationPath());
                doDownload();
            }

            Log.d(TAG, "Transfer completed: " + task.getFileName());
            task.setStatus(TransferTask.Status.COMPLETED);
            callback.onTransferCompleted(task);

        } catch (ProtocolException e) {
            Log.e(TAG, "Transfer failed (ProtocolException): " + e.getMessage(), e);
            task.setStatus(TransferTask.Status.FAILED);
            task.setErrorMessage(e.getMessage());
            callback.onTransferFailed(task, e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Transfer failed (Exception): " + e.getMessage(), e);
            task.setStatus(TransferTask.Status.FAILED);
            task.setErrorMessage(e.getMessage());
            callback.onTransferFailed(task, e.getMessage());
        } finally {
            if (protocolManager != null) {
                protocolManager.disconnect();
            }
        }
    }

    private void doUpload() throws Exception {
        try (InputStream source = new FileInputStream(task.getSourcePath())) {
            protocolManager.uploadFile(source, task.getDestinationPath(), task.getTotalBytes(),
                    new ProtocolManager.ProgressListener() {
                        @Override
                        public void onProgress(long bytesTransferred, long totalBytes) {
                            task.setTransferredBytes(bytesTransferred);
                            callback.onTransferProgress(task, bytesTransferred);
                        }

                        @Override
                        public void onCancelled() {
                            task.setStatus(TransferTask.Status.CANCELLED);
                            callback.onTransferCancelled(task);
                        }
                    });
        }
    }

    private void doDownload() throws Exception {
        try (OutputStream destination = new FileOutputStream(task.getDestinationPath())) {
            protocolManager.downloadFile(task.getSourcePath(), destination,
                    new ProtocolManager.ProgressListener() {
                        @Override
                        public void onProgress(long bytesTransferred, long totalBytes) {
                            task.setTransferredBytes(bytesTransferred);
                            callback.onTransferProgress(task, bytesTransferred);
                        }

                        @Override
                        public void onCancelled() {
                            task.setStatus(TransferTask.Status.CANCELLED);
                            callback.onTransferCancelled(task);
                        }
                    });
        }
    }

    private ConnectionInfo getConnectionInfo(long connectionId) throws Exception {
        com.yulink.nas.data.db.entity.ConnectionEntity entity =
                com.yulink.nas.data.db.AppDatabase.getInstance(context)
                        .connectionDao().getConnectionById(connectionId);
        if (entity == null) {
            throw new ProtocolException("Connection not found: " + connectionId);
        }
        ConnectionInfo info = new ConnectionInfo();
        info.setId(entity.id);
        info.setName(entity.name);
        info.setProtocol(entity.protocol);
        info.setHost(entity.host);
        info.setPort(entity.port);
        info.setUsername(entity.username);
        info.setPassword(com.yulink.nas.util.CryptoUtils.decrypt(entity.encryptedPassword));
        info.setShareName(entity.shareName);
        info.setDefaultPath(entity.defaultPath);
        info.setPassiveMode(entity.passiveMode);
        info.setUseFtps(entity.useFtps);
        info.setUseSmbEncryption(entity.useSmbEncryption);
        return info;
    }

    public void cancel() {
        if (protocolManager != null) {
            protocolManager.disconnect();
        }
    }

    public interface TransferCallback {
        void onTransferStarted(TransferTask task);
        void onTransferProgress(TransferTask task, long bytesTransferred);
        void onTransferCompleted(TransferTask task);
        void onTransferFailed(TransferTask task, String error);
        void onTransferCancelled(TransferTask task);
    }
}
