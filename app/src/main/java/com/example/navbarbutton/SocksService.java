package com.example.navbarbutton;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.ipc.RootService;

import java.io.File;

public class SocksService extends RootService {
    public static VpnService vpnService = null;
    public native int startOhClient(String db, String listen, String relay, String relay6, int timeout);
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        System.loadLibrary("ohclient");
        try {
            String relay = intent.getStringExtra("relay");
            String db = intent.getStringExtra("db");
            int res = startOhClient(db, "0.0.0.0:9999", relay, "", 60);
            Log.i(NavbarHook.TAG, "oh.client started " + relay + " [" + db + ": " + res);
        } catch (Exception e) {
            Log.e(NavbarHook.TAG, "oh.client start exception", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    public void vpnProtect(int fd) {
        if (vpnService != null)
            vpnService.protect(fd);
    }
}
