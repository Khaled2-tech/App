package com.focus.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

public class TimerService extends Service {
    public static final String ACTION_REFRESH = "com.focus.app.REFRESH";
    private static final String CHANNEL = "focus_timer";
    private final Handler handler = new Handler();
    private SharedPreferences prefs;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (prefs == null || !prefs.getBoolean("running", false)) { stopSelf(); return; }
            long left = prefs.getLong("end_at", 0L) - System.currentTimeMillis();
            if (left <= 0) { finishSession(); return; }
            handler.postDelayed(this, 1000);
        }
    };
    @Override public void onCreate() {
        super.onCreate(); prefs = getSharedPreferences("focus_prefs", MODE_PRIVATE); createChannel();
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 26) startForeground(10, notification("المؤقّت يعمل"));
        handler.removeCallbacks(tick); handler.post(tick);
        return START_STICKY;
    }
    private void finishSession() {
        int phase = prefs.getInt("phase", 0);
        SharedPreferences.Editor e = prefs.edit().putBoolean("running", false).putLong("remaining_ms", 0L);
        if (phase == 0) e.putInt("completed_sessions", prefs.getInt("completed_sessions", 0)+1);
        e.apply();
        try {
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone ring = RingtoneManager.getRingtone(getApplicationContext(), sound);
            if (ring != null) ring.play();
        } catch (Exception ignored) {}
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(11, notification("انتهت الجلسة — أحسنت، خذ لحظة للراحة"));
        stopForeground(true); stopSelf();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "مؤقّت Focus", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(c);
        }
    }
    private Notification notification(String message) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 26) return new Notification.Builder(this, CHANNEL).setContentTitle("Focus").setContentText(message).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentIntent(pi).setOngoing(prefs.getBoolean("running", false)).build();
        return new Notification.Builder(this).setContentTitle("Focus").setContentText(message).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentIntent(pi).setOngoing(prefs.getBoolean("running", false)).build();
    }
    @Override public void onDestroy() { handler.removeCallbacks(tick); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
