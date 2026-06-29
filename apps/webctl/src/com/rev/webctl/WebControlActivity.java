package com.rev.webctl;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WebControlActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        startService(new Intent(this, WebService.class));
        final String url = "http://" + WebService.ip() + ":" + WebService.PORT;

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        col.setPadding(pad, pad, pad, pad);
        col.setBackgroundColor(0xFF0E1216);

        TextView t = new TextView(this);
        t.setText("🐒 Simian Web Control\n\nServer is RUNNING at:\n\n" + url
                + "\n\nPIN: " + WebService.readPin(this)
                + "\n\nOpen that URL from any phone/PC on the same WiFi, enter the PIN once, and control this Echo.\n\n"
                + "(First command pops a Magisk Grant prompt — tap Grant. Starts automatically on boot. "
                + "Change the PIN by editing files/pin.txt.)");
        t.setTextColor(0xFFE6E6E6);
        t.setTextSize(18);
        col.addView(t);

        Button open = new Button(this);
        open.setText("Open control panel here");
        open.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) {}
            }
        });
        col.addView(open);

        Button stop = new Button(this);
        stop.setText("Stop server");
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(WebControlActivity.this, WebService.class));
                finish();
            }
        });
        col.addView(stop);

        setContentView(col);
    }
}
