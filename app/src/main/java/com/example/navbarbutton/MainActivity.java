package com.example.navbarbutton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

        TextView vl = new TextView(this);
        vl.setText(this.getSharedPreferences("data", Context.MODE_PRIVATE).getString("launcher", ""));
        vl.setTypeface(null, Typeface.BOLD);
        vl.setPadding(48, 0, 48, 0);

        root.addView(v, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        root.addView(vl, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

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
            this.getSharedPreferences("data", Context.MODE_PRIVATE).edit().putString("launcher", packageName).apply();
            vl.setText(packageName);
        });
        listView.setPadding(48, 0, 48, 256);

        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);
    }
}
