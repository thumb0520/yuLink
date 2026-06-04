package com.yulink.nas.transfer;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.yulink.nas.YuLinkApp;
import com.yulink.nas.R;
import com.yulink.nas.data.model.TransferTask;
import com.yulink.nas.data.repository.TransferRepository;

public class TransferService extends Service implements TransferWorker.TransferCallback {
    private static final int NOTIFICATION_ID = 1001;

    private final IBinder binder = new TransferBinder();
    private TransferManager transferManager;
    private TransferRepository transferRepository;
    private TransferNotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        transferManager = TransferManager.getInstance();
        transferRepository = new TransferRepository(getApplication());
        notificationHelper = new TransferNotificationHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createForegroundNotification());
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void enqueueTransfer(TransferTask task) {
        transferRepository.addTask(task);
        transferManager.enqueueTransfer(task, this);
    }

    @Override
    public void onTransferStarted(TransferTask task) {
        updateForegroundNotification(task);
    }

    @Override
    public void onTransferProgress(TransferTask task, long bytesTransferred) {
        transferRepository.updateTaskProgress(task.getTaskId(), bytesTransferred);
        updateForegroundNotification(task);
    }

    @Override
    public void onTransferCompleted(TransferTask task) {
        transferRepository.updateTaskStatus(task.getTaskId(), TransferTask.Status.COMPLETED, null);
        notificationHelper.showCompletionNotification(task);
        checkAndStopService();
    }

    @Override
    public void onTransferFailed(TransferTask task, String error) {
        transferRepository.updateTaskStatus(task.getTaskId(), TransferTask.Status.FAILED, error);
        notificationHelper.showFailureNotification(task, error);
        checkAndStopService();
    }

    @Override
    public void onTransferCancelled(TransferTask task) {
        transferRepository.updateTaskStatus(task.getTaskId(), TransferTask.Status.CANCELLED, null);
        notificationHelper.cancelNotification(task.getNotificationId());
        checkAndStopService();
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, YuLinkApp.CHANNEL_ID_TRANSFER)
                .setSmallIcon(R.drawable.ic_transfer)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("准备传输...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateForegroundNotification(TransferTask task) {
        Notification notification = notificationHelper.createProgressNotification(task);
        startForeground(NOTIFICATION_ID, notification);
    }

    private void checkAndStopService() {
        if (transferManager.getActiveCount() == 0 && transferManager.getQueueSize() == 0) {
            stopForeground(true);
            stopSelf();
        }
    }

    public class TransferBinder extends Binder {
        public TransferService getService() {
            return TransferService.this;
        }
    }
}
