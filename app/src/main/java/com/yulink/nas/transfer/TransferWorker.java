package com.yulink.nas.transfer;

import com.yulink.nas.data.model.ConnectionInfo;
import com.yulink.nas.data.model.TransferTask;
import com.yulink.nas.protocol.ProtocolException;
import com.yulink.nas.protocol.ProtocolFactory;
import com.yulink.nas.protocol.ProtocolManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class TransferWorker implements Runnable {
    private final TransferTask task;
    private final TransferCallback callback;
    private ProtocolManager protocolManager;

    public TransferWorker(TransferTask task, TransferCallback callback) {
        this.task = task;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            task.setStatus(TransferTask.Status.RUNNING);
            callback.onTransferStarted(task);

            // Get connection info and create protocol manager
            ConnectionInfo connectionInfo = getConnectionInfo(task.getConnectionId());
            protocolManager = ProtocolFactory.create(connectionInfo.getProtocol());
            protocolManager.connect(connectionInfo);

            if (task.getDirection() == TransferTask.Direction.UPLOAD) {
                doUpload();
            } else {
                doDownload();
            }

            task.setStatus(TransferTask.Status.COMPLETED);
            callback.onTransferCompleted(task);

        } catch (ProtocolException e) {
            task.setStatus(TransferTask.Status.FAILED);
            task.setErrorMessage(e.getMessage());
            callback.onTransferFailed(task, e.getMessage());
        } catch (Exception e) {
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

    private ConnectionInfo getConnectionInfo(long connectionId) {
        // TODO: Get from database
        // For now, return a placeholder
        ConnectionInfo info = new ConnectionInfo();
        info.setId(connectionId);
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
