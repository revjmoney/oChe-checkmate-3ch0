package com.rev.webctl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Starts the web control server on boot.
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        try { c.startService(new Intent(c, WebService.class)); } catch (Exception e) {}
    }
}
