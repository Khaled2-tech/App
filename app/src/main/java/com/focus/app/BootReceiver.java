package com.focus.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences p = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE);
            if (p.getBoolean("running", false)) {
                long left = p.getLong("end_at", 0L) - System.currentTimeMillis();
                if (left > 0) {
                    Intent service = new Intent(context, TimerService.class);
                    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service); else context.startService(service);
                } else p.edit().putBoolean("running", false).putLong("remaining_ms", 0L).apply();
            }
        }
    }
}
