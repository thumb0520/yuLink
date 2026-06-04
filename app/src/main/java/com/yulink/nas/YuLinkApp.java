package com.yulink.nas;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class YuLinkApp extends Application {

    public static final String CHANNEL_ID_TRANSFER = "transfer_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationChannel transferChannel = new NotificationChannel(
                CHANNEL_ID_TRANSFER,
                getString(R.string.transfer_notification_channel),
                NotificationManager.IMPORTANCE_LOW
        );
        transferChannel.setDescription("File transfer progress notifications");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(transferChannel);
        }
    }
}
