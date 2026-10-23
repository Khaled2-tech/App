package com.focus.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "focus_prefs";
    private SharedPreferences prefs;
    private TextView phaseText, timerText, quoteText, sessionsText, stateText;
    private Button startButton, modeButton;
    private CountDownTimer ticker;
    private int phase = 0; // 0 study, 1 short break, 2 long break
    private boolean running;

    private int color(String hex) { return Color.parseColor(hex); }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setPadding(dp(24), dp(22), dp(24), dp(24));
        l.setBackgroundColor(color("#F6F4EF"));
        return l;
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, String tint) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size);
        t.setTextColor(color(tint)); t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return t;
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false);
        b.setTextColor(color("#FFFFFF")); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color("#49685E")));
        return b;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(color("#F6F4EF"));
        getWindow().setNavigationBarColor(color("#F6F4EF"));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        LinearLayout root = column();
        TextView brand = text("Focus", 27, "#304A42");
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        brand.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        root.addView(brand, new LinearLayout.LayoutParams(-1, dp(56)));

        phaseText = text("وقت الدراسة", 18, "#64756D");
        LinearLayout.LayoutParams phaseParams = new LinearLayout.LayoutParams(-1, dp(54));
        phaseParams.topMargin = dp(28); root.addView(phaseText, phaseParams);

        timerText = text("25:00", 64, "#263C35");
        timerText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        root.addView(timerText, new LinearLayout.LayoutParams(-1, dp(130)));

        stateText = text("خذ وقتك، وركّز على الخطوة الحالية.", 14, "#7A817B");
        root.addView(stateText, new LinearLayout.LayoutParams(-1, dp(38)));

        startButton = button("ابدأ الدراسة");
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, dp(54));
        startParams.topMargin = dp(25); root.addView(startButton, startParams);
        startButton.setOnClickListener(v -> toggleTimer());

        Button extra = button("+ 5 دقائق");
        LinearLayout.LayoutParams extraParams = new LinearLayout.LayoutParams(-1, dp(48));
        extraParams.topMargin = dp(10); root.addView(extra, extraParams);
        extra.setOnClickListener(v -> addFiveMinutes());

        modeButton = button("تغيير نوع الجلسة");
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(-1, dp(48));
        modeParams.topMargin = dp(10); root.addView(modeButton, modeParams);
        modeButton.setOnClickListener(v -> cyclePhase());

        sessionsText = text("", 13, "#7A817B");
        LinearLayout.LayoutParams sessionParams = new LinearLayout.LayoutParams(-1, dp(42));
        sessionParams.topMargin = dp(12); root.addView(sessionsText, sessionParams);

        quoteText = text("كل خطوة صغيرة تقرّبك من هدفك.", 16, "#49685E");
        LinearLayout.LayoutParams quoteParams = new LinearLayout.LayoutParams(-1, dp(72));
        quoteParams.topMargin = dp(30); root.addView(quoteText, quoteParams);

        Button settings = button("الإعدادات");
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(-1, dp(46));
        settingsParams.topMargin = dp(10); root.addView(settings, settingsParams);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        setContentView(root);
        syncFromPrefs();
    }
    private int durationForPhase() {
        String key = phase == 0 ? "study_minutes" : (phase == 1 ? "short_minutes" : "long_minutes");
        return prefs.getInt(key, phase == 0 ? 25 : (phase == 1 ? 5 : 15));
    }
    private void toggleTimer() {
        if (running) {
            long remaining = Math.max(0L, prefs.getLong("end_at", System.currentTimeMillis()) - System.currentTimeMillis());
            prefs.edit().putBoolean("running", false).putLong("remaining_ms", remaining).apply();
            stopService(new Intent(this, TimerService.class));
            running = false; if (ticker != null) ticker.cancel();
            startButton.setText("استئناف");
            stateText.setText("المؤقّت متوقف مؤقتًا");
            return;
        }
        long end = System.currentTimeMillis() + Math.max(1, prefs.getLong("remaining_ms", durationForPhase() * 60000L));
        prefs.edit().putLong("end_at", end).putLong("remaining_ms", end-System.currentTimeMillis()).putBoolean("running", true).putInt("phase", phase).apply();
        running = true;
        Intent i = new Intent(this, TimerService.class);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        startButton.setText("إيقاف مؤقت");
        stateText.setText("الجلسة مستمرة حتى عند إغلاق التطبيق");
        startTicker();
    }
    private void addFiveMinutes() {
        long base = running ? prefs.getLong("end_at", System.currentTimeMillis()) : prefs.getLong("remaining_ms", durationForPhase()*60000L) + System.currentTimeMillis();
        long updated = base + 5*60000L;
        prefs.edit().putLong("end_at", updated).putLong("remaining_ms", Math.max(0, updated-System.currentTimeMillis())).apply();
        if (running) { Intent i = new Intent(this, TimerService.class); i.setAction(TimerService.ACTION_REFRESH); if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); startTicker(); }
        else { prefs.edit().putLong("remaining_ms", Math.max(0, updated-System.currentTimeMillis())).apply(); renderTime(Math.max(0, updated-System.currentTimeMillis())); }
    }
    private void cyclePhase() {
        if (running) { stateText.setText("أوقف المؤقّت مؤقتًا قبل تغيير نوع الجلسة."); return; }
        phase = (phase + 1) % 3;
        prefs.edit().putInt("phase", phase).putLong("remaining_ms", durationForPhase()*60000L).apply();
        syncFromPrefs();
    }
    private void syncFromPrefs() {
        phase = prefs.getInt("phase", 0);
        running = prefs.getBoolean("running", false);
        phaseText.setText(phase == 0 ? "وقت الدراسة" : phase == 1 ? "استراحة قصيرة" : "استراحة طويلة");
        sessionsText.setText("جلسات الدراسة المكتملة: " + prefs.getInt("completed_sessions", 0));
        long remaining = running ? Math.max(0, prefs.getLong("end_at", System.currentTimeMillis()) - System.currentTimeMillis()) : prefs.getLong("remaining_ms", durationForPhase()*60000L);
        if (!running && remaining <= 0) remaining = durationForPhase()*60000L;
        renderTime(remaining);
        startButton.setText(running ? "إيقاف مؤقت" : "ابدأ الجلسة");
        if (running) startTicker();
    }
    private void renderTime(long ms) {
        long total = Math.max(0, (ms + 999) / 1000);
        timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", total/60, total%60));
    }
    private void startTicker() {
        if (ticker != null) ticker.cancel();
        long left = Math.max(0, prefs.getLong("end_at", System.currentTimeMillis()) - System.currentTimeMillis());
        ticker = new CountDownTimer(left, 500) {
            public void onTick(long ms) { renderTime(ms); }
            public void onFinish() { syncFromPrefs(); }
        }.start();
    }
    @Override protected void onResume() { super.onResume(); if (prefs != null) syncFromPrefs(); }
    @Override protected void onDestroy() { if (ticker != null) ticker.cancel(); super.onDestroy(); }
}
