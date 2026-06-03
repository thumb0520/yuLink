package com.easyconnect.nas.transfer;

import com.easyconnect.nas.data.model.TransferTask;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TransferManager {
    private static final int MAX_CONCURRENT_TRANSFERS = 3;
    private static TransferManager instance;

    private final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor executor;
    private TransferCallback callback;

    private TransferManager() {
        executor = new ThreadPoolExecutor(1, MAX_CONCURRENT_TRANSFERS,
                60L, TimeUnit.SECONDS, workQueue);
    }

    public static synchronized TransferManager getInstance() {
        if (instance == null) {
            instance = new TransferManager();
        }
        return instance;
    }

    public void setCallback(TransferCallback callback) {
        this.callback = callback;
    }

    public void enqueueTransfer(TransferTask task, TransferWorker.TransferCallback workerCallback) {
        TransferWorker worker = new TransferWorker(task, workerCallback);
        executor.execute(worker);
    }

    public void cancelAll() {
        executor.shutdownNow();
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public int getQueueSize() {
        return workQueue.size();
    }

    public interface TransferCallback {
        void onTransferStarted(TransferTask task);
        void onTransferProgress(TransferTask task, long bytesTransferred);
        void onTransferCompleted(TransferTask task);
        void onTransferFailed(TransferTask task, String error);
        void onTransferCancelled(TransferTask task);
    }
}
