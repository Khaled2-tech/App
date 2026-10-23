package com.khaled2tech.focus;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private TextView timeView;
    private Button startPauseButton;
    private CountDownTimer timer;
    private long remainingMs = 25 * 60 * 1000L;
    private boolean running = false;
    private long endAtMs = 0L;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        remainingMs = getPreferences(MODE_PRIVATE).getLong("remaining_ms", 25 * 60 * 1000L);
        running = getPreferences(MODE_PRIVATE).getBoolean("running", false);
        endAtMs = getPreferences(MODE_PRIVATE).getLong("end_at_ms", 0L);
        if (running && endAtMs > 0) {
            remainingMs = Math.max(0, endAtMs - System.currentTimeMillis());
            if (remainingMs == 0) running = false;
        }
        buildUi();
        if (running) startTimer(); else renderTime();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(28, 28, 28, 24);
        root.setBackgroundColor(Color.rgb(250, 247, 253));

        TextView title = new TextView(this);
        title.setText("✿ Focus");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(49, 48, 145));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 70));

        TextView subtitle = new TextView(this);
        subtitle.setText("مساحتك الهادئة للتركيز");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(92, 84, 154));
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, 48));

        timeView = new TextView(this);
        timeView.setTextSize(42);
        timeView.setTypeface(Typeface.DEFAULT_BOLD);
        timeView.setTextColor(Color.rgb(49, 48, 145));
        timeView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(250, 250);
        timerParams.setMargins(0, 30, 0, 24);
        timeView.setBackgroundColor(Color.rgb(242, 226, 246));
        root.addView(timeView, timerParams);

        startPauseButton = button("ابدأ المذاكرة");
        startPauseButton.setOnClickListener(v -> {
            if (running) pauseTimer(); else startTimer();
        });
        root.addView(startPauseButton, buttonParams());

        Button addTime = button("+  إضافة 5 دقائق");
        addTime.setOnClickListener(v -> {
            if (running) {
                remainingMs += 5 * 60 * 1000L;
                endAtMs = System.currentTimeMillis() + remainingMs;
                saveState();
                startTimer();
            } else {
                remainingMs += 5 * 60 * 1000L;
                saveState(); renderTime();
            }
        });
        root.addView(addTime, buttonParams());

        Button finish = button("إنهاء الجلسة");
        finish.setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            running = false; remainingMs = 25 * 60 * 1000L; endAtMs = 0L;
            saveState(); renderTime(); startPauseButton.setText("ابدأ المذاكرة");
        });
        root.addView(finish, buttonParams());
        setContentView(root);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setTextColor(Color.rgb(49, 48, 145));
        return b;
    }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 54);
        p.setMargins(0, 7, 0, 7); return p;
    }
    private void startTimer() {
        if (timer != null) timer.cancel();
        if (remainingMs <= 0) remainingMs = 25 * 60 * 1000L;
        running = true;
        endAtMs = System.currentTimeMillis() + remainingMs;
        startPauseButton.setText("إيقاف مؤقت");
        saveState();
        timer = new CountDownTimer(remainingMs, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished; renderTime(); saveState();
            }
            @Override public void onFinish() {
                remainingMs = 0; running = false; endAtMs = 0L;
                renderTime(); startPauseButton.setText("ابدأ المذاكرة"); saveState();
            }
        }.start();
    }
    private void pauseTimer() {
        if (timer != null) timer.cancel();
        if (endAtMs > 0) remainingMs = Math.max(0, endAtMs - System.currentTimeMillis());
        running = false; endAtMs = 0L;
        startPauseButton.setText("متابعة المذاكرة"); saveState(); renderTime();
    }
    private void renderTime() {
        if (timeView == null) return;
        long totalSeconds = Math.max(0, remainingMs / 1000);
        timeView.setText(String.format(Locale.getDefault(), "%02d:%02d\n\nمن 25:00", totalSeconds / 60, totalSeconds % 60));
    }
    private void saveState() {
        getPreferences(MODE_PRIVATE).edit().putLong("remaining_ms", remainingMs)
            .putBoolean("running", running).putLong("end_at_ms", endAtMs).apply();
    }
    @Override protected void onStop() { super.onStop(); saveState(); }
}
