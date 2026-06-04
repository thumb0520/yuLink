package com.yulink.nas.transfer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.yulink.nas.YuLinkApp;
import com.yulink.nas.R;
import com.yulink.nas.data.model.TransferTask;
import com.yulink.nas.ui.main.MainActivity;

public class TransferNotificationHelper {
    private final Context context;
    private final NotificationManager notificationManager;

    public TransferNotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public Notification createProgressNotification(TransferTask task) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = task.getDirection() == TransferTask.Direction.UPLOAD ?
                context.getString(R.string.transfer_uploading) :
                context.getString(R.string.transfer_downloading);

        return new NotificationCompat.Builder(context, YuLinkApp.CHANNEL_ID_TRANSFER)
                .setSmallIcon(R.drawable.ic_transfer)
                .setContentTitle(title)
                .setContentText(task.getFileName())
                .setProgress(100, task.getProgressPercent(), false)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    public void showCompletionNotification(TransferTask task) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, YuLinkApp.CHANNEL_ID_TRANSFER)
                .setSmallIcon(R.drawable.ic_transfer)
                .setContentTitle(context.getString(R.string.transfer_completed))
                .setContentText(task.getFileName())
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(task.getNotificationId(), notification);
    }

    public void showFailureNotification(TransferTask task, String errorMessage) {
        Notification notification = new NotificationCompat.Builder(context, YuLinkApp.CHANNEL_ID_TRANSFER)
                .setSmallIcon(R.drawable.ic_transfer)
                .setContentTitle(context.getString(R.string.transfer_failed))
                .setContentText(task.getFileName() + ": " + errorMessage)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(task.getNotificationId(), notification);
    }

    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }
}
