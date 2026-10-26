package com.ztai.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class BotService extends Service {

    private static final String CH = "ztai_ch";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                CH, "ZTai", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) b = new Notification.Builder(this, CH);
        else b = new Notification.Builder(this);

        b.setContentTitle("ZTai")
         .setContentText("Активен")
         .setSmallIcon(android.R.drawable.stat_notify_sync);

        if (Build.VERSION.SDK_INT >= 29) {
            try {
                startForeground(1, b.build(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    | android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } catch (Throwable t) {
                startForeground(1, b.build());
            }
        } else {
            startForeground(1, b.build());
        }

        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
