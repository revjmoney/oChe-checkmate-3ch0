package com.rev.displayoff;

import android.app.Activity;
import android.os.Bundle;

// One-tap screen-off tile (Simian Tactical Toolbox).
// No UI: sleeps the display via root, then finishes.
public class DisplayOff extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "input keyevent 223"});
            p.waitFor();
        } catch (Exception e) {
            // ignore
        }
        finish();
    }
}
