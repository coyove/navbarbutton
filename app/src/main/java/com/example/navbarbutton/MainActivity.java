package com.example.navbarbutton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The APK has a tiny launcher activity only so Android/LSPosed can install it normally.
 * The actual feature is implemented by NavbarHook.
 */
public class MainActivity extends Activity {
    static {
        System.loadLibrary("ohclient");
    }

    public static native int startOhClient(String db, String listen, String relay, String relay6, int timeout);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Intent intent = new Intent(this, NativeService.class);
        startForegroundService(intent);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView v = new TextView(this);
        v.setText("LSPosed 101 Navbar Shortcut");
        v.setPadding(48, 128, 48, 48);

        SharedPreferences pref = this.getSharedPreferences("data", Context.MODE_PRIVATE);
        TextView vl = new TextView(this);
        vl.setText(pref.getString("launcher", ""));
        vl.setTypeface(null, Typeface.BOLD);
        vl.setPadding(48, 0, 48, 0);

        EditText relay = new EditText(this);
        relay.setText(pref.getString("relay", "23.149.36.195:80"));
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

        root.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(relay, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(vl, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView listView = new ListView(this);
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        apps.removeIf(app -> pm.getLaunchIntentForPackage(app.packageName) == null);

        // Sort by app name
        apps.sort((a, b) ->
                pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));

        List<String> names = new ArrayList<>();
        List<String> packages = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            names.add(pm.getApplicationLabel(app).toString());
            packages.add(app.packageName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                names
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                text1.setText(names.get(position));
                return view;
            }
        };

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String packageName = packages.get(position);
            pref.edit().putString("launcher", packageName).apply();
            vl.setText(packageName);
        });
        listView.setPadding(0, 0, 0, 256);

        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);
    }
}
