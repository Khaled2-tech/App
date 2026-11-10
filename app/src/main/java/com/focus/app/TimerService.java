package com.focus.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class TimerService extends Service {
    public static final String ACTION_REFRESH = "com.focus.app.REFRESH";
    private static final String CHANNEL = "focus_timer";
    private final Handler handler = new Handler();
    private SharedPreferences prefs;
    private PowerManager.WakeLock wakeLock;
    private Ringtone alarm;
    private boolean finishing;
    private final Runnable stopAlarm = new Runnable() {
        @Override public void run() {
            try { if (alarm != null && alarm.isPlaying()) alarm.stop(); } catch (Exception ignored) {}
            alarm = null;
        }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (prefs == null || !prefs.getBoolean("running", false)) { pauseAndStop(); return; }
            long left = prefs.getLong("end_at", 0L) - System.currentTimeMillis();
            if (left <= 0) { finishSession(); return; }
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(10, notification(clock(left)));
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("focus_prefs", MODE_PRIVATE);
        createChannel();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "focus:timer");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(10, notification(runningLabel()));
        holdWake();
        handler.removeCallbacks(tick);
        if (prefs.getBoolean("running", false)) handler.post(tick);
        else pauseAndStop();
        return START_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        if (prefs != null && prefs.getBoolean("running", false)) {
            Intent again = new Intent(this, TimerService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(again); else startService(again);
        }
        super.onTaskRemoved(rootIntent);
    }

    private void finishSession() {
        if (finishing || prefs == null) return;
        finishing = true;
        int phase = prefs.getInt("phase", 0);
        int completed = prefs.getInt("completed_sessions", 0);
        int cycle = Math.max(1, prefs.getInt("long_after", 4));
        boolean advance;
        int next;
        if (phase == 0) {
            completed += 1;
            next = completed % cycle == 0 ? 2 : 1;
            advance = prefs.getBoolean("auto_short", true);
        } else if (phase == 1) {
            next = 0;
            advance = prefs.getBoolean("auto_study", true);
        } else {
            next = 0;
            advance = prefs.getBoolean("auto_long", true);
        }
        long dur = minutesFor(next) * 60000L;
        SharedPreferences.Editor editor = prefs.edit()
                .putInt("completed_sessions", completed)
                .putInt("phase", next)
                .putLong("session_duration_ms", dur)
                .putLong("remaining_ms", dur);
        playAlarm(advance ? 4000L : 60000L);
        vibrate();
        if (advance) {
            editor.putBoolean("running", true).putLong("end_at", System.currentTimeMillis() + dur).apply();
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(10, notification(phaseName(next)));
            finishing = false;
            handler.post(tick);
            return;
        }
        editor.putBoolean("running", false).apply();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(11, notification("انتهت الجلسة — أحسنت"));
        pauseAndStop();
        finishing = false;
    }

    private int minutesFor(int phase) {
        if (phase == 0) return prefs.getInt("study_minutes", 25);
        if (phase == 1) return prefs.getInt("short_minutes", 5);
        return prefs.getInt("long_minutes", 15);
    }
    private String phaseName(int phase) {
        if (phase == 0) return "جلسة مذاكرة";
        if (phase == 1) return "استراحة قصيرة";
        return "استراحة طويلة";
    }
    private String runningLabel() {
        if (prefs == null || !prefs.getBoolean("running", false)) return "المؤقّت يعمل";
        long left = prefs.getLong("end_at", 0L) - System.currentTimeMillis();
        return left > 0 ? clock(left) : phaseName(prefs.getInt("phase", 0));
    }
    private String clock(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return String.format(java.util.Locale.US, "%02d:%02d", s / 60, s % 60);
    }
    private void playAlarm(long limitMs) {
        try {
            handler.removeCallbacks(stopAlarm);
            if (alarm != null && alarm.isPlaying()) alarm.stop();
            String custom = prefs.getString("custom_sound_uri", null);
            String selected = prefs.getString("alert_sound_uri", null);
            Uri sound = custom != null ? Uri.parse(custom) : selected != null ? Uri.parse(selected) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            alarm = RingtoneManager.getRingtone(getApplicationContext(), sound);
            if (alarm != null) {
                alarm.play();
                handler.postDelayed(stopAlarm, limitMs);
            }
        } catch (Exception ignored) {}
    }
    private void vibrate() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator == null) return;
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(400);
        } catch (Exception ignored) {}
    }
    private void holdWake() {
        try {
            if (wakeLock != null && !wakeLock.isHeld()) wakeLock.acquire(4 * 60 * 60 * 1000L);
        } catch (Exception ignored) {}
    }
    private void releaseWake() {
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) {}
    }
    private void pauseAndStop() {
        handler.removeCallbacks(tick);
        releaseWake();
        try { stopForeground(true); } catch (Exception ignored) {}
        stopSelf();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "مؤقّت Focus", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(c);
        }
    }
    private Notification notification(String message) {
        Intent open = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, flags);
        boolean ongoing = prefs != null && prefs.getBoolean("running", false);
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(this, CHANNEL)
                    .setContentTitle("Focus").setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentIntent(pi).setOngoing(ongoing).build();
        }
        return new Notification.Builder(this)
                .setContentTitle("Focus").setContentText(message)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pi).setOngoing(ongoing).build();
    }
    @Override public void onDestroy() {
        handler.removeCallbacks(tick);
        releaseWake();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
