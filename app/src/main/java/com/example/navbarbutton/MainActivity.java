package com.example.navbarbutton;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.topjohnwu.superuser.ipc.RootService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import hev.sockstun.TProxyService;

/**
 * The APK has a tiny launcher activity only so Android/LSPosed can install it normally.
 * The actual feature is implemented by NavbarHook.
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        VpnService.prepare(this);

        Intent intent = new Intent(this, NativeService.class);
        startForegroundService(intent);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView v = new TextView(this);
        v.setText("Navbarbutton");
        v.setPadding(48, 128, 48, 48);

        SharedPreferences pref = this.getSharedPreferences("data", Context.MODE_PRIVATE);
        TextView vl = new TextView(this);
        vl.setTypeface(null, Typeface.BOLD);
        vl.setPadding(48, 0, 48, 0);

        EditText relay = new EditText(this);
        relay.setText(pref.getString("relay", "rigid.top:80"));
        relay.setPadding(48, 0, 48, 48);
        relay.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                pref.edit().putString("relay", s.toString()).apply();
            }
        });

        Switch vpnToggle = new Switch(this);
        vpnToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent i = new Intent(this, TProxyService.class);
            if (isChecked) {
                Intent i2 = VpnService.prepare(this);
                if (i2 != null) {
                    startActivityForResult(i2, 100);
                } else {
                    startForegroundService(i.setAction(TProxyService.ACTION_CONNECT));
                }
            } else {
                startForegroundService(i.setAction(TProxyService.ACTION_DISCONNECT));
            }
        });
        vpnToggle.setChecked(pref.getBoolean("vpn", false));

        root.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(relay, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(vpnToggle, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(vl, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView listView = new ListView(this);
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        // apps.removeIf(app -> pm.getLaunchIntentForPackage(app.packageName) == null);

        // Sort by app name
        apps.sort((a, b) ->
                pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));

        List<String> names = new ArrayList<>();
        List<String> packages = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            names.add(pm.getApplicationLabel(app).toString());
            packages.add(app.packageName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, names);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String packageName = packages.get(position);
            new Thread(() -> {
                Looper.prepare();
                if (listView.isItemChecked(position)) {
                    TProxyService.requestWhitelist("add", packageName);
                } else {
                    TProxyService.requestWhitelist("delete", packageName);
                }
                Toast.makeText(this, "OK " + TProxyService.whitelistApps.size() + " apps", Toast.LENGTH_SHORT).show();
                TProxyService.saveVpnWhitelist(getExternalFilesDir(null), TProxyService.whitelistApps);
            }).start();
        });
        listView.setPadding(0, 0, 0, 256);
        new Thread(() -> {
            Looper.prepare();
            TProxyService.requestWhitelist("", "");
            TProxyService.saveVpnWhitelist(getExternalFilesDir(null), TProxyService.whitelistApps);
            new Handler(Looper.getMainLooper()).post(() -> {
                for (int i = 0; i < packages.size(); i++) {
                    if (TProxyService.whitelistApps.contains(packages.get(i))) {
                        listView.setItemChecked(i, true);
                    }
                }
            });
            Toast.makeText(this, "OK " + TProxyService.whitelistApps.size() + " apps", Toast.LENGTH_SHORT).show();
        }).start();

        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);
    }
}
