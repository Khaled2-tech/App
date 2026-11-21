package com.focus.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
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
    private MediaPlayer alarmPlayer;
    private MediaPlayer ambientPlayer;
    private String activeAmbientKey;
    private final Runnable ambientStopRunnable = this::stopAmbientPlayback;
    private final SharedPreferences.OnSharedPreferenceChangeListener ambientPrefsListener = (shared, key) -> {
        if ("ambient_play_enabled".equals(key) || "ambient_sound_key".equals(key)
                || "ambient_volume".equals(key) || "running".equals(key) || "phase".equals(key)
                || "end_at".equals(key)) {
            handler.post(this::syncAmbientPlayback);
        }
    };
    private int currentRepeat;
    private int totalRepeats;
    private boolean finishing;

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
        prefs.registerOnSharedPreferenceChangeListener(ambientPrefsListener);
        createChannel();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "focus:timer");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        stopAlarm();
        startForeground(10, notification(runningLabel()));
        holdWake();
        handler.removeCallbacks(tick);
        if (prefs.getBoolean("running", false)) {
            syncAmbientPlayback();
            handler.post(tick);
        } else pauseAndStop();
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
        stopAmbientPlayback();
        long dur = minutesFor(next) * 60000L;
        SharedPreferences.Editor editor = prefs.edit()
                .putInt("completed_sessions", completed)
                .putInt("phase", next)
                .putLong("session_duration_ms", dur)
                .putLong("remaining_ms", dur);

        playAlarmForPhase(phase);
        vibrate();

        if (advance) {
            editor.putBoolean("running", true).putLong("end_at", System.currentTimeMillis() + dur).apply();
            syncAmbientPlayback();
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

    private int rawResForSoundKey(String key) {
        if (key == null) return R.raw.sparkle;
        switch (key) {
            case "chime": return R.raw.chime;
            case "piano": return R.raw.piano;
            case "success": return R.raw.success;
            case "game_show": return R.raw.game_show;
            case "train_arrival": return R.raw.train_arrival;
            case "commuter_jingle": return R.raw.commuter_jingle;
            case "sparkle":
            default:
                return R.raw.sparkle;
        }
    }


    private int rawResForAmbientKey(String key) {
        if (key == null) return 0;
        switch (key) {
            case "rain": return R.raw.rain;
            case "light_rain": return R.raw.light_rain;
            case "heavy_rain": return R.raw.heavy_rain;
            case "light_rain_on_tent": return R.raw.light_rain_on_tent;
            case "thunderstorm": return R.raw.thunderstorm;
            case "ocean": return R.raw.ocean;
            case "waterfall": return R.raw.waterfall;
            case "campfire": return R.raw.campfire;
            case "fireplace": return R.raw.fireplace;
            case "wind": return R.raw.wind;
            case "birds_in_the_woods": return R.raw.birds_in_the_woods;
            case "summer_night": return R.raw.summer_night;
            case "exam_hall": return R.raw.exam_hall;
            default: return 0;
        }
    }

    private void syncAmbientPlayback() {
        if (prefs == null) return;
        boolean shouldPlay = prefs.getBoolean("running", false)
                && prefs.getInt("phase", 0) == 0
                && prefs.getBoolean("ambient_play_enabled", false);
        String key = prefs.getString("ambient_sound_key", "");
        int rawRes = rawResForAmbientKey(key);
        if (!shouldPlay || key.isEmpty() || rawRes == 0) {
            stopAmbientPlayback();
            return;
        }

        if (ambientPlayer == null || !key.equals(activeAmbientKey)) {
            stopAmbientPlayback();
            try {
                ambientPlayer = MediaPlayer.create(getApplicationContext(), rawRes);
                if (ambientPlayer == null) return;
                activeAmbientKey = key;
                ambientPlayer.setLooping(true);
                float volume = prefs.getFloat("ambient_volume", 0.70f);
                ambientPlayer.setVolume(volume, volume);
                ambientPlayer.start();
            } catch (Exception ignored) {
                stopAmbientPlayback();
                return;
            }
        } else {
            try {
                float volume = prefs.getFloat("ambient_volume", 0.70f);
                ambientPlayer.setVolume(volume, volume);
                if (!ambientPlayer.isPlaying()) ambientPlayer.start();
            } catch (Exception ignored) {
                stopAmbientPlayback();
                return;
            }
        }
        scheduleAmbientStop();
    }

    private void scheduleAmbientStop() {
        handler.removeCallbacks(ambientStopRunnable);
        if (prefs == null || ambientPlayer == null) return;
        long stopAt = prefs.getLong("end_at", 0L) - 1000L;
        long delay = stopAt - System.currentTimeMillis();
        if (delay <= 0) {
            stopAmbientPlayback();
        } else {
            handler.postDelayed(ambientStopRunnable, delay);
        }
    }

    private void stopAmbientPlayback() {
        handler.removeCallbacks(ambientStopRunnable);
        if (ambientPlayer != null) {
            try {
                if (ambientPlayer.isPlaying()) ambientPlayer.stop();
                ambientPlayer.release();
            } catch (Exception ignored) {}
            ambientPlayer = null;
        }
        activeAmbientKey = null;
    }

    private void playAlarmForPhase(int endedPhase) {
        stopAlarm();
        try {
            final int repeats = (endedPhase == 0) ? 1 : 3;
            totalRepeats = repeats;
            currentRepeat = 0;

            String soundKey = prefs != null ? prefs.getString("alert_sound_key", "sparkle") : "sparkle";
            int resId = rawResForSoundKey(soundKey);
            final float vol = prefs != null ? prefs.getFloat("alert_volume", 0.8f) : 0.8f;

            alarmPlayer = MediaPlayer.create(getApplicationContext(), resId);
            if (alarmPlayer == null) return;
            alarmPlayer.setVolume(vol, vol);
            alarmPlayer.setOnCompletionListener(mp -> {
                currentRepeat++;
                if (currentRepeat < totalRepeats) {
                    try {
                        mp.seekTo(0);
                        mp.start();
                    } catch (Exception e) {
                        stopAlarm();
                    }
                } else {
                    stopAlarm();
                }
            });
            alarmPlayer.start();
        } catch (Exception ignored) {}
    }

    private void stopAlarm() {
        if (alarmPlayer != null) {
            try {
                if (alarmPlayer.isPlaying()) alarmPlayer.stop();
                alarmPlayer.release();
            } catch (Exception ignored) {}
            alarmPlayer = null;
        }
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
        stopAmbientPlayback();
        stopAlarm();
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
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(ambientPrefsListener);
        stopAmbientPlayback();
        stopAlarm();
        releaseWake();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
