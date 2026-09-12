package com.example.navbarbutton;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

public class NativeService extends Service {
    private Process process;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1001, createNotification());

        try {
            String relay = this.getSharedPreferences("data", Context.MODE_PRIVATE).getString("relay", "23.149.36.195:80");
            String db = new File(getFilesDir(), "oh.db").getAbsolutePath();
            db += ";" + getExternalFilesDir(null);
            int res = MainActivity.startOhClient(db, ":9999", relay, "", 60);
            Log.i(NavbarHook.TAG, "oh.client started " + relay + " [" + db + ": " + res);
        } catch (Exception e) {
            Log.e(NavbarHook.TAG, "oh.client start exception", e);
            throw new RuntimeException(e);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        String channelId = "oh_client_process";

        NotificationManager manager = getSystemService(NotificationManager.class);

        NotificationChannel channel = new NotificationChannel(
                channelId,
                "Native Process",
                NotificationManager.IMPORTANCE_LOW
        );

        manager.createNotificationChannel(channel);

        return new Notification.Builder(this, channelId)
                .setContentTitle("OpenHTTP")
                .setContentText("oh.client is running in the background")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .build();
    }
}
