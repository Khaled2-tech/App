package com.focus.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.util.Locale;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private TextView phaseText, timerText, totalText, sessionsText;
    private Button startButton;
    private TimerRing timerRing;
    private CountDownTimer ticker;
    private ImageView wallpaper;
    private LinearLayout progressDots;
    private int phase;
    private boolean running;
    private boolean compact;
    private Typeface jakartaBold = Typeface.DEFAULT_BOLD;
    private Typeface jakartaSemi = Typeface.DEFAULT;
    private Typeface cairoMedium = Typeface.DEFAULT;
    private Typeface cairoSemi = Typeface.DEFAULT_BOLD;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (shared, key) -> {
        if (key == null) return;
        if (key.equals("selected_theme") || key.equals("custom_background_uri")) runOnUiThread(this::loadWallpaper);
        if (key.equals("phase") || key.equals("running") || key.equals("completed_sessions")
                || key.equals("remaining_ms") || key.equals("session_duration_ms") || key.equals("end_at")
                || key.equals("long_after")) {
            runOnUiThread(this::sync);
        }
    };

    private Typeface loadFont(String name, Typeface fallback) {
        try { return Typeface.createFromAsset(getAssets(), name); }
        catch (Exception e) { return fallback; }
    }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private int c(String s) { return Color.parseColor(s); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private TextView label(String s, int size, String color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(c(color));
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        return t;
    }
    private Button pill(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(compact ? 13 : 14);
        b.setTypeface(cairoSemi);
        b.setIncludeFontPadding(false);
        b.setTextColor(c(primary ? "#FFFFFF" : "#4746CB"));
        if (primary) {
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{c("#E65BAF"), c("#7654D8")});
            g.setCornerRadius(dp(28));
            b.setBackground(g);
        } else {
            GradientDrawable glass = shape(Color.argb(90, 255, 255, 255), 24);
            glass.setStroke(dp(1), Color.argb(90, 118, 84, 216));
            b.setBackground(glass);
        }
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(6), 0, dp(6), 0);
        if (Build.VERSION.SDK_INT >= 21) b.setElevation(dp(1));
        return b;
    }

    private void loadWallpaper() {
        if (wallpaper == null || prefs == null) return;
        String custom = prefs.getString("custom_background_uri", null);
        Object source;
        if (custom != null) source = Uri.parse(custom);
        else source = Uri.parse("file:///android_asset/themes/" + fileFor(prefs.getString("selected_theme", "Morning Sun")));
        int w = Math.max(1, getResources().getDisplayMetrics().widthPixels);
        int h = Math.max(1, getResources().getDisplayMetrics().heightPixels);
        Glide.with(this).asBitmap()
                .apply(new RequestOptions().format(DecodeFormat.PREFER_RGB_565).centerCrop().override(w, h))
                .load(source)
                .into(new CustomTarget<Bitmap>(w, h) {
                    @Override public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        wallpaper.setImageBitmap(bitmap);
                        int color = topAverage(bitmap);
                        if (Build.VERSION.SDK_INT >= 21) {
                            getWindow().setStatusBarColor(color);
                            getWindow().setNavigationBarColor(darken(color, .78f));
                        }
                        if (Build.VERSION.SDK_INT >= 23) {
                            boolean light = brightness(color) > .62;
                            int flags = 0;
                            if (light) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                            if (light && Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                            getWindow().getDecorView().setSystemUiVisibility(flags);
                        }
                    }
                    @Override public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {}
                });
    }

    static String fileFor(String selected) {
        String[] names = {"Golden Glow", "Pink Clouds", "Cozy Corner", "Night City", "Morning Sun", "Brise Lames", "BioGlow", "Deep Forst", "Mountain Village", "Twilight Fuji"};
        String[] files = {"Golden Glow.jpg", "Pink Clouds.jpg", "Cozy Corner.jpg", "Night City.jpg", "Morning Sun.jpg", "Brise Lames.jpg", "BioGlow.jpg", "Deep Forst.png", "Mountain Village.jpg", "Twilight Fuji.png"};
        for (int i = 0; i < names.length; i++) if (names[i].equals(selected)) return files[i];
        return "Morning Sun.jpg";
    }

    private int topAverage(Bitmap b) {
        int h = Math.max(1, b.getHeight() / 18), w = b.getWidth(), step = Math.max(1, w / 48);
        long r = 0, g = 0, bl = 0, n = 0;
        for (int y = 0; y < h; y += Math.max(1, h / 6)) {
            for (int x = 0; x < w; x += step) {
                int px = b.getPixel(x, y);
                r += Color.red(px); g += Color.green(px); bl += Color.blue(px); n++;
            }
        }
        if (n == 0) return Color.WHITE;
        return Color.rgb((int) (r / n), (int) (g / n), (int) (bl / n));
    }
    private int darken(int color, float factor) {
        return Color.rgb((int) (Color.red(color) * factor), (int) (Color.green(color) * factor), (int) (Color.blue(color) * factor));
    }
    private double brightness(int color) {
        return (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("focus_prefs", MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int heightDp = Math.round(dm.heightPixels / dm.density);
        compact = dm.widthPixels <= 720 || heightDp <= 700;
        jakartaBold = loadFont("PlusJakartaSans-Bold.ttf", Typeface.DEFAULT_BOLD);
        jakartaSemi = loadFont("PlusJakartaSans-SemiBold.ttf", Typeface.DEFAULT);
        cairoMedium = loadFont("Cairo-Medium.ttf", Typeface.DEFAULT);
        cairoSemi = loadFont("Cairo-SemiBold.ttf", Typeface.DEFAULT_BOLD);

        FrameLayout root = new FrameLayout(this);
        wallpaper = new ImageView(this);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
        loadWallpaper();
        View tint = new View(this);
        tint.setBackgroundColor(Color.argb(28, 255, 255, 255));
        root.addView(tint, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(compact ? 12 : 18), dp(compact ? 4 : 8), dp(compact ? 12 : 18), dp(compact ? 8 : 12));
        scroll.addView(page, new FrameLayout.LayoutParams(-1, -1));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        View edge = new View(this);
        header.addView(edge, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_focus_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brand.addView(logo, new LinearLayout.LayoutParams(dp(compact ? 26 : 31), dp(compact ? 26 : 31)));
        TextView brandText = label("Focus", compact ? 22 : 24, "#4746CB");
        brandText.setTypeface(jakartaSemi);
        brand.addView(brandText);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(44), 1));
        ImageView settings = new ImageView(this);
        settings.setImageResource(R.drawable.ic_settings);
        settings.setPadding(dp(10), dp(10), dp(10), dp(10));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings, new LinearLayout.LayoutParams(dp(42), dp(42)));
        page.addView(header);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(14), dp(compact ? 8 : 12), dp(14), dp(compact ? 8 : 12));
        GradientDrawable card = shape(Color.argb(compact ? 236 : 220, 255, 255, 255), 22);
        panel.setBackground(card);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, 0, 1);
        pp.topMargin = dp(8);
        page.addView(panel, pp);

        phaseText = label("جلسة مذاكرة", compact ? 13 : 14, "#4746CB");
        phaseText.setTypeface(cairoMedium);
        GradientDrawable badge = shape(Color.argb(210, 243, 236, 252), 20);
        phaseText.setBackground(badge);
        phaseText.setPadding(dp(16), 0, dp(16), 0);
        panel.addView(phaseText, new LinearLayout.LayoutParams(-2, dp(34)));

        int reserved = dp(compact ? 250 : 290);
        int status = 0;
        int statusId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (statusId > 0) status = getResources().getDimensionPixelSize(statusId);
        int avail = dm.heightPixels - status - reserved;
        int ringSize = Math.min(dp(compact ? 158 : 184), Math.min((int) (dm.widthPixels * (compact ? 0.46f : 0.52f)), Math.max(dp(120), avail)));
        FrameLayout ringFrame = new FrameLayout(this);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ringSize, ringSize);
        rp.topMargin = dp(4);
        rp.bottomMargin = dp(2);
        panel.addView(ringFrame, rp);
        timerRing = new TimerRing(this);
        ringFrame.addView(timerRing, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        timerText = label("25:00", compact ? 34 : 40, "#4746CB");
        timerText.setTypeface(jakartaBold);
        center.addView(timerText);
        totalText = label("من 25:00", compact ? 12 : 13, "#6A68B4");
        totalText.setTypeface(cairoMedium);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.topMargin = dp(4);
        center.addView(totalText, tp);
        ringFrame.addView(center, new FrameLayout.LayoutParams(-1, -1));

        startButton = pill("ابدأ", true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(compact ? 44 : 48));
        sp.topMargin = dp(6);
        sp.leftMargin = dp(8);
        sp.rightMargin = dp(8);
        panel.addView(startButton, sp);
        startButton.setOnClickListener(v -> toggleTimer());

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] names = {"+ وقت", "تخطّي", "إنهاء"};
        for (String n : names) {
            Button x = pill(n, false);
            x.setTypeface(cairoMedium);
            LinearLayout.LayoutParams xp = new LinearLayout.LayoutParams(0, dp(compact ? 40 : 44), 1);
            xp.setMargins(dp(3), dp(6), dp(3), 0);
            controls.addView(x, xp);
            x.setOnClickListener(v -> {
                if (n.equals("+ وقت")) addFiveMinutes();
                else if (n.equals("تخطّي")) nextPhase();
                else finishPeriod();
            });
        }
        panel.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(10), dp(8), dp(10), dp(8));
        bottom.setBackground(shape(Color.argb(230, 255, 255, 255), 20));
        LinearLayout.LayoutParams botp = new LinearLayout.LayoutParams(-1, -2);
        botp.topMargin = dp(8);
        page.addView(bottom, botp);
        sessionsText = label("0 / 4 جلسات مكتملة في الدورة", compact ? 13 : 14, "#4746CB");
        sessionsText.setTypeface(cairoSemi);
        bottom.addView(sessionsText, new LinearLayout.LayoutParams(-1, -2));
        progressDots = new LinearLayout(this);
        progressDots.setGravity(Gravity.CENTER);
        progressDots.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        LinearLayout.LayoutParams dots = new LinearLayout.LayoutParams(-1, dp(18));
        dots.topMargin = dp(6);
        bottom.addView(progressDots, dots);
        setContentView(root);
        sync();
    }

    private int minutesFor(int p) {
        return prefs.getInt(p == 0 ? "study_minutes" : p == 1 ? "short_minutes" : "long_minutes", p == 0 ? 25 : p == 1 ? 5 : 15);
    }
    private long durationFor(int p) { return minutesFor(p) * 60000L; }
    private long sessionDuration() {
        long stored = prefs.getLong("session_duration_ms", 0L);
        return stored > 0 ? stored : durationFor(phase);
    }
    private void updatePhaseBadge() {
        phaseText.setText(phase == 0 ? "جلسة مذاكرة" : phase == 1 ? "استراحة قصيرة" : "استراحة طويلة");
    }
    private void toggleTimer() {
        if (running) {
            long left = Math.max(0, prefs.getLong("end_at", System.currentTimeMillis()) - System.currentTimeMillis());
            prefs.edit().putBoolean("running", false).putLong("remaining_ms", left).apply();
            stopService(new Intent(this, TimerService.class));
            running = false;
            if (ticker != null) ticker.cancel();
            startButton.setText("استئناف");
            updateRing(left);
            return;
        }
        long total = sessionDuration();
        long left = prefs.getLong("remaining_ms", total);
        if (left <= 0 || left > total) {
            total = durationFor(phase);
            left = total;
            prefs.edit().putLong("session_duration_ms", total).apply();
        }
        long end = System.currentTimeMillis() + left;
        prefs.edit().putLong("end_at", end).putLong("remaining_ms", left).putBoolean("running", true).putInt("phase", phase).apply();
        running = true;
        Intent i = new Intent(this, TimerService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        startButton.setText("إيقاف مؤقت");
        startTicker();
    }
    private void addFiveMinutes() {
        long now = System.currentTimeMillis();
        long left = running ? Math.max(0, prefs.getLong("end_at", now) - now) : prefs.getLong("remaining_ms", durationFor(phase));
        long total = sessionDuration() + 300000L;
        prefs.edit().putLong("session_duration_ms", total).putLong("end_at", now + left + 300000L).putLong("remaining_ms", left + 300000L).apply();
        if (running) {
            Intent i = new Intent(this, TimerService.class);
            i.setAction(TimerService.ACTION_REFRESH);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            startTicker();
        } else updateRing(left + 300000L);
    }
    private void nextPhase() {
        stopCurrent();
        phase = (phase + 1) % 3;
        long d = durationFor(phase);
        prefs.edit().putInt("phase", phase).putLong("remaining_ms", d).putLong("session_duration_ms", d).putBoolean("running", false).apply();
        sync();
    }
    private void finishPeriod() {
        stopCurrent();
        phase = 0;
        long d = durationFor(0);
        prefs.edit().putInt("phase", 0).putLong("remaining_ms", d).putLong("session_duration_ms", d).putBoolean("running", false).apply();
        sync();
    }
    private void stopCurrent() {
        if (running) {
            prefs.edit().putBoolean("running", false).apply();
            stopService(new Intent(this, TimerService.class));
            running = false;
        }
        if (ticker != null) ticker.cancel();
    }
    private boolean inSync;
    private void sync() {
        if (inSync || phaseText == null || prefs == null) return;
        inSync = true;
        try {
            phase = prefs.getInt("phase", 0);
            running = prefs.getBoolean("running", false);
            updatePhaseBadge();
            long d = sessionDuration();
            long left = running ? Math.max(0, prefs.getLong("end_at", System.currentTimeMillis()) - System.currentTimeMillis()) : prefs.getLong("remaining_ms", d);
            if (!running && left <= 0) {
                d = durationFor(phase);
                left = d;
                prefs.edit().putLong("session_duration_ms", d).putLong("remaining_ms", d).apply();
            }
            render(left);
            totalText.setText("من " + clock(d));
            renderProgressDots();
            boolean paused = !running && left + 800 < d;
            startButton.setText(running ? "إيقاف مؤقت" : paused ? "استئناف" : "ابدأ");
            updateRing(left);
            if (running && left > 0) startTicker();
            else if (ticker != null) ticker.cancel();
        } finally {
            inSync = false;
        }
    }
    private void render(long ms) { timerText.setText(clock(ms)); }
    private String clock(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }
    private void updateRing(long left) {
        if (timerRing == null) return;
        float f = sessionDuration() <= 0 ? 1f : Math.max(0f, Math.min(1f, (float) left / sessionDuration()));
        timerRing.setFraction(f);
    }
    private void startTicker() {
        if (ticker != null) ticker.cancel();
        long left = Math.max(0, prefs.getLong("end_at", System.currentTimeMillis()) - System.currentTimeMillis());
        ticker = new CountDownTimer(Math.max(left, 1), 250) {
            public void onTick(long m) { render(m); updateRing(m); }
            public void onFinish() { sync(); }
        };
        ticker.start();
    }
    private void renderProgressDots() {
        if (progressDots == null || sessionsText == null) return;
        progressDots.removeAllViews();
        int total = Math.max(1, Math.min(12, prefs.getInt("long_after", 4)));
        int done = prefs.getInt("completed_sessions", 0) % total;
        sessionsText.setText(String.format(Locale.US, "%d / %d جلسات مكتملة في الدورة", done, total));
        for (int i = 0; i < total; i++) {
            View dot = new View(this);
            dot.setBackground(shape(i < done ? Color.argb(220, 229, 150, 209) : Color.argb(110, 229, 150, 209), 20));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(8), dp(8));
            p.setMargins(dp(3), 0, dp(3), 0);
            progressDots.addView(dot, p);
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if (prefs != null && phaseText != null) { loadWallpaper(); sync(); }
    }
    @Override protected void onDestroy() {
        if (ticker != null) ticker.cancel();
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(listener);
        super.onDestroy();
    }

    private class TimerRing extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float fraction = 1f;
        TimerRing(android.content.Context c) { super(c); }
        void setFraction(float f) { fraction = f; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) * .42f;
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(compact ? 7 : 8));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(Color.argb(40, 226, 133, 159));
            canvas.drawCircle(w / 2, h / 2, r, p);
            p.setShader(new SweepGradient(w / 2, h / 2,
                    new int[]{Color.parseColor("#4746CB"), Color.parseColor("#766BE0"), Color.parseColor("#E596D1"), Color.parseColor("#4746CB")},
                    new float[]{0f, .45f, .88f, 1f}));
            canvas.save();
            canvas.rotate(-90, w / 2, h / 2);
            canvas.drawArc(w / 2 - r, h / 2 - r, w / 2 + r, h / 2 + r, 0, -360 * fraction, false, p);
            canvas.restore();
            p.setShader(null);
        }
    }
}
