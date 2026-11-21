package com.focus.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SessionAlertActivity extends Activity {
    private int endedPhase = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isDismissed = false;

    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.ACTION_ALERT_STOPPED.equals(intent.getAction())) {
                dismissScreen();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= 21) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(Color.parseColor("#0B0817"));
            w.setNavigationBarColor(Color.parseColor("#0B0817"));
        }

        endedPhase = getIntent().getIntExtra("ended_phase", 0);

        IntentFilter filter = new IntentFilter(TimerService.ACTION_ALERT_STOPPED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stopReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopReceiver, filter);
        }

        buildUi();

        // Fallback safety timeout in case sound doesn't trigger or finishes
        handler.postDelayed(this::dismissScreen, 25000L);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private Typeface loadFont(String name, Typeface fallback) {
        try { return Typeface.createFromAsset(getAssets(), name); }
        catch (Exception e) { return fallback; }
    }

    private void buildUi() {
        Typeface jakartaBold = loadFont("PlusJakartaSans-Bold.ttf", Typeface.DEFAULT_BOLD);
        Typeface cairoBold = loadFont("Cairo-SemiBold.ttf", Typeface.DEFAULT_BOLD);
        Typeface cairoMedium = loadFont("Cairo-Medium.ttf", Typeface.DEFAULT);

        FrameLayout root = new FrameLayout(this);
        GradientDrawable bgGradient = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{ Color.parseColor("#0B0818"), Color.parseColor("#140F27") }
        );
        root.setBackground(bgGradient);

        // Center ambient background glow
        View ambientGlow = new View(this);
        GradientDrawable glowDrawable = new GradientDrawable();
        glowDrawable.setShape(GradientDrawable.OVAL);
        glowDrawable.setColor(Color.argb(45, 110, 75, 230));
        ambientGlow.setBackground(glowDrawable);
        FrameLayout.LayoutParams glowLp = new FrameLayout.LayoutParams(dp(320), dp(320));
        glowLp.gravity = Gravity.CENTER;
        root.addView(ambientGlow, glowLp);

        // Main scrollable content
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(20), dp(48), dp(20), dp(32));

        // Top Logo + Focus title
        ImageView logoView = new ImageView(this);
        logoView.setImageResource(R.drawable.ic_focus_logo);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(54), dp(54));
        content.addView(logoView, logoLp);

        TextView focusTitle = new TextView(this);
        focusTitle.setText("Focus");
        focusTitle.setTextColor(Color.WHITE);
        focusTitle.setTextSize(22);
        focusTitle.setTypeface(jakartaBold);
        if (Build.VERSION.SDK_INT >= 21) focusTitle.setLetterSpacing(0.04f);
        focusTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-2, -2);
        titleLp.topMargin = dp(8);
        titleLp.bottomMargin = dp(28);
        content.addView(focusTitle, titleLp);

        // Glassmorphic Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(24), dp(30), dp(24), dp(30));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(dp(28));
        cardBg.setColor(Color.argb(210, 29, 22, 58)); // Glass effect
        cardBg.setStroke(dp(1.5f), Color.argb(120, 142, 117, 232)); // Subtle purple border glow
        card.setBackground(cardBg);

        if (endedPhase == 0) {
            // Study Session completed: Checkmark circle
            FrameLayout checkCircle = new FrameLayout(this);
            GradientDrawable checkBg = new GradientDrawable();
            checkBg.setShape(GradientDrawable.OVAL);
            checkBg.setColor(Color.parseColor("#4B34A2"));
            checkBg.setStroke(dp(2), Color.parseColor("#8C70E6"));
            checkCircle.setBackground(checkBg);

            ImageView checkIcon = new ImageView(this);
            checkIcon.setImageResource(R.drawable.ic_check_white);
            FrameLayout.LayoutParams cLp = new FrameLayout.LayoutParams(dp(36), dp(36));
            cLp.gravity = Gravity.CENTER;
            checkCircle.addView(checkIcon, cLp);

            LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(dp(72), dp(72));
            checkLp.bottomMargin = dp(18);
            card.addView(checkCircle, checkLp);

            // Title: انتهى الوقت
            TextView mainTitle = new TextView(this);
            mainTitle.setText("انتهى الوقت");
            mainTitle.setTextColor(Color.WHITE);
            mainTitle.setTextSize(28);
            mainTitle.setTypeface(cairoBold);
            mainTitle.setGravity(Gravity.CENTER);
            card.addView(mainTitle, new LinearLayout.LayoutParams(-2, -2));

            // Subtitle: أحسنت! لقد ركزت على ما يهمك.
            TextView subTitle = new TextView(this);
            subTitle.setText("أحسنت! لقد ركزت على ما يهمك.");
            subTitle.setTextColor(Color.parseColor("#C3BCDE"));
            subTitle.setTextSize(15);
            subTitle.setTypeface(cairoMedium);
            subTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
            subLp.topMargin = dp(6);
            subLp.bottomMargin = dp(28);
            card.addView(subTitle, subLp);
        } else {
            // Break completed: انتهت الاستراحه!
            TextView mainTitle = new TextView(this);
            mainTitle.setText("انتهت الاستراحه!");
            mainTitle.setTextColor(Color.WHITE);
            mainTitle.setTextSize(30);
            mainTitle.setTypeface(cairoBold);
            mainTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(-2, -2);
            tLp.topMargin = dp(10);
            card.addView(mainTitle, tLp);

            // Subtitle: انتهت الاستراحه، وقت المذاكرة!
            TextView subTitle = new TextView(this);
            subTitle.setText("انتهت الاستراحه، وقت المذاكرة!");
            subTitle.setTextColor(Color.parseColor("#C3BCDE"));
            subTitle.setTextSize(15);
            subTitle.setTypeface(cairoMedium);
            subTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
            subLp.topMargin = dp(6);
            subLp.bottomMargin = dp(32);
            card.addView(subTitle, subLp);
        }

        // Button 1: وقت إضافي (with clock icon)
        LinearLayout btnExtra = new LinearLayout(this);
        btnExtra.setOrientation(LinearLayout.HORIZONTAL);
        btnExtra.setGravity(Gravity.CENTER);
        btnExtra.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable btnExtraBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{ Color.parseColor("#6A45F8"), Color.parseColor("#4D36D6") }
        );
        btnExtraBg.setCornerRadius(dp(26));
        btnExtra.setBackground(btnExtraBg);

        ImageView clockIcon = new ImageView(this);
        clockIcon.setImageResource(R.drawable.ic_clock_white);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(dp(20), dp(20));
        clockLp.setMargins(0, 0, dp(8), 0);
        btnExtra.addView(clockIcon, clockLp);

        TextView extraText = new TextView(this);
        extraText.setText("وقت إضافي");
        extraText.setTextColor(Color.WHITE);
        extraText.setTextSize(16);
        extraText.setTypeface(cairoBold);
        btnExtra.addView(extraText, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout.LayoutParams btnExtraLp = new LinearLayout.LayoutParams(-1, dp(52));
        btnExtraLp.bottomMargin = dp(14);
        card.addView(btnExtra, btnExtraLp);

        btnExtra.setOnClickListener(v -> onExtraTimeClicked());

        // Button 2: ايقاف
        LinearLayout btnStop = new LinearLayout(this);
        btnStop.setOrientation(LinearLayout.HORIZONTAL);
        btnStop.setGravity(Gravity.CENTER);
        GradientDrawable btnStopBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{ Color.parseColor("#573BD3"), Color.parseColor("#3F26A8") }
        );
        btnStopBg.setCornerRadius(dp(26));
        btnStop.setBackground(btnStopBg);

        TextView stopText = new TextView(this);
        stopText.setText("ايقاف");
        stopText.setTextColor(Color.WHITE);
        stopText.setTextSize(16);
        stopText.setTypeface(cairoBold);
        btnStop.addView(stopText, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout.LayoutParams btnStopLp = new LinearLayout.LayoutParams(-1, dp(52));
        card.addView(btnStop, btnStopLp);

        btnStop.setOnClickListener(v -> onStopAlertClicked());

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.weight = 1f;
        content.addView(card, cardLp);

        // Bottom tagline: FOCUS TODAY / A BETTER TOMORROW
        LinearLayout bottomLayout = new LinearLayout(this);
        bottomLayout.setOrientation(LinearLayout.VERTICAL);
        bottomLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView b1 = new TextView(this);
        b1.setText("FOCUS TODAY");
        b1.setTextColor(Color.parseColor("#6C6287"));
        b1.setTextSize(11);
        b1.setTypeface(jakartaBold);
        if (Build.VERSION.SDK_INT >= 21) b1.setLetterSpacing(0.2f);
        bottomLayout.addView(b1, new LinearLayout.LayoutParams(-2, -2));

        TextView b2 = new TextView(this);
        b2.setText("A BETTER TOMORROW");
        b2.setTextColor(Color.parseColor("#564D70"));
        b2.setTextSize(11);
        b2.setTypeface(jakartaBold);
        if (Build.VERSION.SDK_INT >= 21) b2.setLetterSpacing(0.2f);
        bottomLayout.addView(b2, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(-2, -2);
        bottomLp.topMargin = dp(36);
        content.addView(bottomLayout, bottomLp);

        scroll.addView(content, new FrameLayout.LayoutParams(-1, -1));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        // Top Right Close (X) Button
        FrameLayout closeBtn = new FrameLayout(this);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.OVAL);
        closeBg.setColor(Color.argb(45, 255, 255, 255));
        closeBg.setStroke(dp(1), Color.argb(40, 255, 255, 255));
        closeBtn.setBackground(closeBg);

        ImageView closeIcon = new ImageView(this);
        closeIcon.setImageResource(R.drawable.ic_close_white);
        FrameLayout.LayoutParams ciLp = new FrameLayout.LayoutParams(dp(18), dp(18));
        ciLp.gravity = Gravity.CENTER;
        closeBtn.addView(closeIcon, ciLp);

        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(dp(40), dp(40));
        closeLp.gravity = Gravity.TOP | Gravity.RIGHT;
        closeLp.topMargin = dp(36);
        closeLp.rightMargin = dp(24);
        root.addView(closeBtn, closeLp);

        closeBtn.setOnClickListener(v -> onStopAlertClicked());

        setContentView(root);
    }

    private void onStopAlertClicked() {
        TimerService.stopAlarmNow(this);
        dismissScreen();
    }

    private void onExtraTimeClicked() {
        TimerService.addExtraTime(this, endedPhase);
        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(main);
        dismissScreen();
    }

    private void dismissScreen() {
        if (isDismissed || isFinishing()) return;
        isDismissed = true;
        handler.removeCallbacksAndMessages(null);
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        onStopAlertClicked();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(stopReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
