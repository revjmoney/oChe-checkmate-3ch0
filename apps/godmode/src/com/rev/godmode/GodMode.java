package com.rev.godmode;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class GodMode extends Activity {

    private LinearLayout col;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView sv = new ScrollView(this);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        col.setPadding(pad, pad, pad, pad);
        col.setBackgroundColor(0xFF0E1216);
        sv.addView(col);
        setContentView(sv);

        header("⚙  GOD MODE");
        sub("by Rev J Money    ·    Simian Tactical Toolbox");

        section("QUICK SETTINGS");
        act("All Settings", Settings.ACTION_SETTINGS);
        act("Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        act("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS);
        act("Display", Settings.ACTION_DISPLAY_SETTINGS);
        act("Sound / Volume", Settings.ACTION_SOUND_SETTINGS);
        act("Date & Time", Settings.ACTION_DATE_SETTINGS);
        act("Language & Input", Settings.ACTION_LOCALE_SETTINGS);
        act("Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        act("Battery", Intent.ACTION_POWER_USAGE_SUMMARY);
        act("Security", Settings.ACTION_SECURITY_SETTINGS);
        act("Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS);
        act("Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        act("Device Info", Settings.ACTION_DEVICE_INFO_SETTINGS);

        section("HIDDEN / DEV MENUS");
        act("Developer Options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        act("All Apps (incl. disabled)", Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
        act("Usage Access", Settings.ACTION_USAGE_ACCESS_SETTINGS);
        act("Modify System Settings", Settings.ACTION_MANAGE_WRITE_SETTINGS);
        act("Input Methods", Settings.ACTION_INPUT_METHOD_SETTINGS);
        act("Default Home / Launcher", Settings.ACTION_HOME_SETTINGS);
        comp("Testing Menu (*#*#4636#*#*)", "com.android.settings",
                "com.android.settings.Settings$TestingSettingsActivity");
        comp("MediaTek EngineerMode", "com.mediatek.engineermode",
                "com.mediatek.engineermode.EngineerMode");

        section("ACTIONS");
        plainBtn("🔊  Make BT Discoverable (5 min)", new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) { toast("BT err: " + e.getMessage()); }
            }
        });
        plainBtn("↻  Kill ALL background apps (root)", new View.OnClickListener() {
            public void onClick(View v) {
                runRoot("for p in $(pm list packages -3 | sed 's/package://'); do am force-stop $p; done",
                        "Killed all 3rd-party apps");
            }
        });

        section("APPS  —  tap name = open,  ✕ = kill (root)");
        addApps();
    }

    private void addApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        for (final ResolveInfo ri : list) {
            final String pkg = ri.activityInfo.packageName;
            final String cls = ri.activityInfo.name;
            if (pkg.equals(getPackageName())) continue;
            String lbl;
            try { lbl = ri.loadLabel(pm).toString(); } catch (Exception e) { lbl = pkg; }
            final String label = lbl;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            Button open = new Button(this);
            open.setText(label);
            open.setAllCaps(false);
            open.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            open.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        Intent i = new Intent();
                        i.setComponent(new ComponentName(pkg, cls));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Exception e) { toast("can't open: " + e.getMessage()); }
                }
            });
            LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(open, op);

            Button kill = new Button(this);
            kill.setText("✕");
            kill.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    runRoot("am force-stop " + pkg, "killed " + label);
                }
            });
            row.addView(kill, new LinearLayout.LayoutParams(dp(56),
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.bottomMargin = dp(6);
            col.addView(row, rp);
        }
    }

    private void runRoot(final String cmd, final String okMsg) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                    int rc = p.waitFor();
                    final String msg = (rc == 0) ? okMsg : ("root failed (rc=" + rc + ")");
                    runOnUiThread(new Runnable() { public void run() { toast(msg); } });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() { public void run() { toast("no root: " + e.getMessage()); } });
                }
            }
        }).start();
    }

    private void header(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(0xFF4FC3F7);
        tv.setTextSize(22);
        tv.setPadding(0, dp(6), 0, dp(10));
        col.addView(tv);
    }

    private void sub(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(0xFFFFC400);
        tv.setTextSize(13);
        tv.setPadding(0, 0, 0, dp(8));
        col.addView(tv);
    }

    private void section(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(0xFFFFB300);
        tv.setTextSize(14);
        tv.setPadding(0, dp(16), 0, dp(6));
        col.addView(tv);
    }

    private void act(final String label, final String action) {
        plainBtn(label, new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent i = new Intent(action);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) { toast("N/A: " + label); }
            }
        });
    }

    private void comp(final String label, final String pkg, final String cls) {
        plainBtn(label, new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent i = new Intent();
                    i.setClassName(pkg, cls);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) { toast("N/A: " + label); }
            }
        });
    }

    private void plainBtn(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        col.addView(b, lp);
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    private int dp(int d) { return (int) (d * getResources().getDisplayMetrics().density); }
}
