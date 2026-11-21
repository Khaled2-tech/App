package com.focus.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.RemoteViews;

import java.util.Locale;

public class TimerService extends Service {
    public static final String ACTION_REFRESH = "com.focus.app.REFRESH";
    public static final String ACTION_PAUSE = "com.focus.app.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.focus.app.ACTION_RESUME";
    public static final String ACTION_STOP_ALARM = "com.focus.app.ACTION_STOP_ALARM";
    public static final String ACTION_EXTRA_TIME = "com.focus.app.ACTION_EXTRA_TIME";
    public static final String ACTION_ALERT_STOPPED = "com.focus.app.ACTION_ALERT_STOPPED";

    private static final String CHANNEL_TIMER = "focus_timer";
    private static final String CHANNEL_ALERTS = "focus_alerts";

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
            if (prefs == null || !prefs.getBoolean("running", false)) {
                return;
            }
            long left = prefs.getLong("end_at", 0L) - System.currentTimeMillis();
            if (left <= 0) {
                finishSession();
                return;
            }
            updateTimerNotification(clock(left), true);
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("focus_prefs", MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(ambientPrefsListener);
        createChannels();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "focus:timer");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        holdWake();

        if (intent != null && intent.getAction() != null) {
            String act = intent.getAction();
            if (ACTION_STOP_ALARM.equals(act)) {
                stopAlarmAndNotify();
                if (!prefs.getBoolean("running", false)) {
                    // keep notification visible if timer paused
                    long rem = prefs.getLong("remaining_ms", sessionDuration());
                    updateTimerNotification(clock(rem), false);
                }
                return START_STICKY;
            } else if (ACTION_EXTRA_TIME.equals(act)) {
                stopAlarmAndNotify();
                int ep = intent.getIntExtra("ended_phase", 0);
                long addMs = 5 * 60 * 1000L;
                long total = sessionDuration() + addMs;
                long endAt = System.currentTimeMillis() + addMs;
                prefs.edit()
                        .putInt("phase", ep)
                        .putBoolean("running", true)
                        .putLong("session_duration_ms", total)
                        .putLong("remaining_ms", addMs)
                        .putLong("end_at", endAt)
                        .apply();
                handler.removeCallbacks(tick);
                syncAmbientPlayback();
                startForeground(10, buildTimerNotification(clock(addMs), true));
                handler.post(tick);
                return START_STICKY;
            } else if (ACTION_PAUSE.equals(act)) {
                long now = System.currentTimeMillis();
                long left = Math.max(0, prefs.getLong("end_at", now) - now);
                prefs.edit().putBoolean("running", false).putLong("remaining_ms", left).apply();
                handler.removeCallbacks(tick);
                stopAmbientPlayback();
                updateTimerNotification(clock(left), false);
                return START_STICKY;
            } else if (ACTION_RESUME.equals(act)) {
                long total = sessionDuration();
                long left = prefs.getLong("remaining_ms", total);
                if (left <= 0 || left > total) left = total;
                long endAt = System.currentTimeMillis() + left;
                prefs.edit().putBoolean("running", true).putLong("end_at", endAt).apply();
                handler.removeCallbacks(tick);
                syncAmbientPlayback();
                startForeground(10, buildTimerNotification(clock(left), true));
                handler.post(tick);
                return START_STICKY;
            }
        }

        stopAlarm();
        boolean isRunning = prefs.getBoolean("running", false);
        long now = System.currentTimeMillis();
        long left = isRunning ? Math.max(0, prefs.getLong("end_at", now) - now) : prefs.getLong("remaining_ms", sessionDuration());
        startForeground(10, buildTimerNotification(clock(left), isRunning));

        handler.removeCallbacks(tick);
        if (isRunning) {
            syncAmbientPlayback();
            handler.post(tick);
        }
        return START_STICKY;
    }

    public static void stopAlarmNow(Context context) {
        try {
            Intent i = new Intent(context, TimerService.class);
            i.setAction(ACTION_STOP_ALARM);
            context.startService(i);
        } catch (Exception ignored) {}
    }

    public static void addExtraTime(Context context, int endedPhase) {
        try {
            Intent i = new Intent(context, TimerService.class);
            i.setAction(ACTION_EXTRA_TIME);
            i.putExtra("ended_phase", endedPhase);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
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
        showCompletionAlert(phase);

        if (advance) {
            editor.putBoolean("running", true).putLong("end_at", System.currentTimeMillis() + dur).apply();
            syncAmbientPlayback();
            updateTimerNotification(clock(dur), true);
            finishing = false;
            handler.post(tick);
            return;
        }

        editor.putBoolean("running", false).apply();
        updateTimerNotification(clock(dur), false);
        handler.removeCallbacks(tick);
        finishing = false;
    }

    private void showCompletionAlert(int endedPhase) {
        Intent alertIntent = new Intent(this, SessionAlertActivity.class);
        alertIntent.putExtra("ended_phase", endedPhase);
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) piFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent fullScreenPending = PendingIntent.getActivity(this, 100, alertIntent, piFlags);

        String title = (endedPhase == 0) ? "انتهى الوقت" : "انتهت الاستراحه!";
        String text = (endedPhase == 0) ? "أحسنت! لقد ركزت على ما يهمك." : "انتهت الاستراحه، وقت المذاكرة!";

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CHANNEL_ALERTS);
        } else {
            b = new Notification.Builder(this);
        }

        b.setSmallIcon(R.drawable.ic_focus_logo)
         .setContentTitle(title)
         .setContentText(text)
         .setContentIntent(fullScreenPending)
         .setFullScreenIntent(fullScreenPending, true)
         .setAutoCancel(true)
         .setPriority(Notification.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= 21) {
            b.setCategory(Notification.CATEGORY_ALARM);
            b.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(11, b.build());
        }

        try {
            startActivity(alertIntent);
        } catch (Exception ignored) {}
    }

    private int minutesFor(int phase) {
        if (phase == 0) return prefs.getInt("study_minutes", 25);
        if (phase == 1) return prefs.getInt("short_minutes", 5);
        return prefs.getInt("long_minutes", 15);
    }

    private long sessionDuration() {
        if (prefs == null) return 25 * 60000L;
        int p = prefs.getInt("phase", 0);
        long stored = prefs.getLong("session_duration_ms", 0L);
        return stored > 0 ? stored : (minutesFor(p) * 60000L);
    }

    private String phaseName(int phase) {
        if (phase == 0) return "مذاكرة";
        if (phase == 1) return "استراحة قصيرة";
        return "استراحة طويلة";
    }

    private String clock(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
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
            if (alarmPlayer == null) {
                notifyAlertStopped();
                return;
            }
            alarmPlayer.setVolume(vol, vol);
            alarmPlayer.setOnCompletionListener(mp -> {
                currentRepeat++;
                if (currentRepeat < totalRepeats) {
                    try {
                        mp.seekTo(0);
                        mp.start();
                    } catch (Exception e) {
                        stopAlarmAndNotify();
                    }
                } else {
                    stopAlarmAndNotify();
                }
            });
            alarmPlayer.start();
        } catch (Exception ignored) {
            notifyAlertStopped();
        }
    }

    private void stopAlarmAndNotify() {
        stopAlarm();
        notifyAlertStopped();
    }

    private void notifyAlertStopped() {
        try {
            sendBroadcast(new Intent(ACTION_ALERT_STOPPED));
        } catch (Exception ignored) {}
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(11);
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

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel cTimer = new NotificationChannel(CHANNEL_TIMER, "مؤقّت Focus", NotificationManager.IMPORTANCE_LOW);
            cTimer.setDescription("العداد التنازلي لحالة المؤقت");
            cTimer.setSound(null, null);
            cTimer.enableVibration(false);
            cTimer.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(cTimer);

            NotificationChannel cAlert = new NotificationChannel(CHANNEL_ALERTS, "تنبيهات انتهاء الجلسات", NotificationManager.IMPORTANCE_HIGH);
            cAlert.setDescription("تنبيه عند انتهاء وقت الجلسة أو الاستراحة");
            cAlert.enableVibration(true);
            cAlert.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            cAlert.setBypassDnd(true);
            nm.createNotificationChannel(cAlert);
        }
    }

    private void updateTimerNotification(String countdownStr, boolean isRunning) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(10, buildTimerNotification(countdownStr, isRunning));
        }
    }

    private Notification buildTimerNotification(String countdownStr, boolean isRunning) {
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) piFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent piOpen = PendingIntent.getActivity(this, 0, open, piFlags);

        Intent toggleIntent = new Intent(this, TimerService.class);
        toggleIntent.setAction(isRunning ? ACTION_PAUSE : ACTION_RESUME);
        PendingIntent piToggle = PendingIntent.getService(this, 1, toggleIntent, piFlags);

        int phase = prefs != null ? prefs.getInt("phase", 0) : 0;
        String phaseTitle = phaseName(phase);

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_timer);
        views.setTextViewText(R.id.notif_phase_title, phaseTitle);
        views.setTextViewText(R.id.notif_countdown, countdownStr);
        views.setTextViewText(R.id.notif_action_text, isRunning ? "إيقاف مؤقت" : "متابعة");
        views.setImageViewResource(R.id.notif_action_icon, isRunning ? R.drawable.ic_pause : R.drawable.ic_play);
        views.setImageViewResource(R.id.notif_logo, R.drawable.ic_focus_logo);

        views.setOnClickPendingIntent(R.id.notif_action_btn, piToggle);
        views.setOnClickPendingIntent(R.id.notif_action_text, piToggle);
        views.setOnClickPendingIntent(R.id.notif_action_icon, piToggle);
        views.setOnClickPendingIntent(R.id.notif_phase_title, piOpen);
        views.setOnClickPendingIntent(R.id.notif_countdown, piOpen);
        views.setOnClickPendingIntent(R.id.notif_logo, piOpen);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CHANNEL_TIMER);
        } else {
            b = new Notification.Builder(this);
        }

        b.setSmallIcon(R.drawable.ic_focus_logo)
         .setContentTitle(phaseTitle)
         .setContentText(countdownStr)
         .setContentIntent(piOpen)
         .setOngoing(isRunning)
         .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT >= 24) {
            b.setCustomContentView(views);
            b.setCustomBigContentView(views);
            b.setStyle(new Notification.DecoratedCustomViewStyle());
        } else {
            b.setContent(views);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            b.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        return b.build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(ambientPrefsListener);
        stopAmbientPlayback();
        stopAlarm();
        releaseWake();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
