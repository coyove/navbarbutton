package com.example.navbarbutton;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;

public class NativeService extends Service {
    private Process process;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1001, createNotification());

        try {
            int exitCode = new ProcessBuilder(
                    "su", "-c", "pkill -TERM -x oh.client"
            ).start().waitFor();
            Log.d("oh.client", "exited: " + exitCode);

            File exe = new File(getFilesDir() + "/oh.client");
            Files.deleteIfExists(exe.toPath());
            Log.d("oh.client", "delete: " + exe.getAbsolutePath());

            // Unpack from assets
            try (InputStream in = getAssets().open("oh.client"); OutputStream out = new FileOutputStream(exe)) {

                byte[] buffer = new byte[64 * 1024];
                int n;

                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
            }

            if (!exe.setExecutable(true, false)) {
                throw new IOException("Cannot make executable");
            }

            // Start native process
            if (process == null || !process.isAlive()) {
                process = new ProcessBuilder("su", "-c", exe.getAbsolutePath())
                        .redirectErrorStream(true)
                        .directory(getFilesDir())
                        .start();
                Log.i(NavbarHook.TAG, "oh.client started");
                if (true) {
                    new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                Log.d("oh.client", line);
                            }
                        } catch (IOException e) {
                            Log.e("oh.client", "read failed", e);
                        }
                    }).start();
                }
            } else {
                Log.i(NavbarHook.TAG, "oh.client already started");
            }
        } catch (Exception e) {
            Log.e(NavbarHook.TAG, "oh.client start exception", e);
            throw new RuntimeException(e);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (process != null) {
            process.destroy();
        }
        super.onDestroy();
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
