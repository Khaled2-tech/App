package com.focus.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;

public class SettingsActivity extends Activity {
    public static class AlertSoundItem {
        public final String key;
        public final String name;
        public final int iconRes;
        public final int rawRes;

        public AlertSoundItem(String key, String name, int iconRes, int rawRes) {
            this.key = key;
            this.name = name;
            this.iconRes = iconRes;
            this.rawRes = rawRes;
        }
    }

    public static final AlertSoundItem[] SOUND_ITEMS = new AlertSoundItem[] {
            new AlertSoundItem("sparkle", "Sparkle", R.drawable.sparkle, R.raw.sparkle),
            new AlertSoundItem("chime", "Chime", R.drawable.chime, R.raw.chime),
            new AlertSoundItem("piano", "Piano", R.drawable.piano, R.raw.piano),
            new AlertSoundItem("success", "Success", R.drawable.success, R.raw.success),
            new AlertSoundItem("game_show", "Game Show", R.drawable.game_show, R.raw.game_show),
            new AlertSoundItem("train_arrival", "Train Arrival", R.drawable.train_arrival, R.raw.train_arrival),
            new AlertSoundItem("commuter_jingle", "Commuter Jingle", R.drawable.commuter_jingle, R.raw.commuter_jingle)
    };

    public static int rawResForSoundKey(String key) {
        if (key == null) return R.raw.sparkle;
        for (AlertSoundItem item : SOUND_ITEMS) {
            if (item.key.equals(key)) return item.rawRes;
        }
        return R.raw.sparkle;
    }

    public static int iconResForSoundKey(String key) {
        if (key == null) return R.drawable.sparkle;
        for (AlertSoundItem item : SOUND_ITEMS) {
            if (item.key.equals(key)) return item.iconRes;
        }
        return R.drawable.sparkle;
    }

    public static String nameForSoundKey(String key) {
        if (key == null) return "Sparkle";
        for (AlertSoundItem item : SOUND_ITEMS) {
            if (item.key.equals(key)) return item.name;
        }
        return "Sparkle";
    }

    private SharedPreferences prefs;
    private EditText study, shortBreak, longBreak, cycle;
    private TextView moreThemes;
    private LinearLayout themeGrid;
    private boolean expanded;
    private boolean compact;
    private MediaPlayer previewPlayer;

    private final String[] themeNames = {
            "Golden Glow", "Pink Clouds", "Cozy Corner", "Night City",
            "Morning Sun", "Brise Lames", "BioGlow", "Deep Forst", "Mountain Village", "Twilight Fuji"
    };
    private final String[] themeFiles = {
            "Golden Glow.jpg", "Pink Clouds.jpg", "Cozy Corner.jpg", "Night City.jpg",
            "Morning Sun.jpg", "Brise Lames.jpg", "BioGlow.jpg", "Deep Forst.png", "Mountain Village.jpg", "Twilight Fuji.png"
    };

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private int c(String s) { return Color.parseColor(s); }

    private GradientDrawable rounded(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke > 0) d.setStroke(dp(1), Color.argb(105, 205, 194, 255));
        return d;
    }

    private GradientDrawable roundedPart(int color, boolean isRight) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        float r = dp(14);
        if (isRight) {
            d.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, r, r});
        } else {
            d.setCornerRadii(new float[]{0, 0, r, r, r, r, 0, 0});
        }
        return d;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private RequestOptions decoded(int w, int h) {
        return new RequestOptions().format(DecodeFormat.PREFER_RGB_565).centerCrop().override(Math.max(1, w), Math.max(1, h));
    }

    private void loadImage(String file, ImageView target) {
        Glide.with(this).load(Uri.parse("file:///android_asset/themes/" + file))
                .apply(decoded(dp(240), dp(140)))
                .into(target);
    }

    private void playAlertPreview(int rawRes) {
        if (previewPlayer != null) {
            try { previewPlayer.stop(); previewPlayer.release(); } catch (Exception ignored) {}
            previewPlayer = null;
        }
        try {
            previewPlayer = MediaPlayer.create(this, rawRes);
            if (previewPlayer == null) return;
            float vol = prefs.getFloat("alert_volume", 0.8f);
            previewPlayer.setVolume(vol, vol);
            previewPlayer.setOnCompletionListener(mp -> {
                try { mp.release(); } catch (Exception ignored) {}
                if (previewPlayer == mp) previewPlayer = null;
            });
            previewPlayer.start();
        } catch (Exception ignored) {}
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        prefs = getSharedPreferences("focus_prefs", MODE_PRIVATE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        compact = dm.widthPixels <= 720 || Math.round(dm.heightPixels / dm.density) <= 700;

        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        int bgColor = isDark ? Color.parseColor("#0C0E1A") : Color.parseColor("#ECEEF8");

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }
        if (Build.VERSION.SDK_INT >= 23) {
            View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (!isDark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decor.setSystemUiVisibility(flags);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(bgColor);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = vertical();
        page.setPadding(dp(compact ? 12 : 18), dp(10), dp(compact ? 12 : 18), dp(24));
        scroll.addView(page);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout header = new FrameLayout(this);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        TextView title = txt("الإعدادات", compact ? 22 : 26, isDark ? "#FFFFFF" : "#1B1E38", true);
        title.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(-2, -2);
        titleLp.gravity = Gravity.CENTER;
        header.addView(title, titleLp);

        TextView save = txt("حفظ", 16, "#FFFFFF", true);
        save.setGravity(Gravity.CENTER);
        save.setBackground(rounded(c("#5146D8"), 17, 0));
        FrameLayout.LayoutParams saveLp = new FrameLayout.LayoutParams(dp(64), dp(46));
        saveLp.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        header.addView(save, saveLp);

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_chevron_left);
        back.setPadding(dp(10), dp(10), dp(10), dp(10));
        if (isDark) {
            back.setBackground(rounded(Color.argb(95, 255, 255, 255), 17, 1));
            back.setColorFilter(Color.WHITE);
        } else {
            back.setBackground(rounded(Color.argb(160, 255, 255, 255), 17, 1));
            back.setColorFilter(Color.parseColor("#1B1E38"));
        }
        FrameLayout.LayoutParams backLp = new FrameLayout.LayoutParams(dp(44), dp(46));
        backLp.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        header.addView(back, backLp);

        page.addView(header, new LinearLayout.LayoutParams(-1, dp(46)));
        save.setOnClickListener(v -> saveSettings());
        back.setOnClickListener(v -> finish());

        // 1. مدة الفترات
        LinearLayout durations = card(page);
        sectionTitle(durations, "مدة الفترات", R.drawable.ic_clock);
        LinearLayout r1 = horizontal();
        r1.addView(fieldColumn("مدة المذاكرة", "study_minutes", 25, R.drawable.ic_book_open, true), new LinearLayout.LayoutParams(0, -2, 1));
        r1.addView(fieldColumn("مدة الاستراحة القصيرة", "short_minutes", 5, R.drawable.ic_coffee, true), new LinearLayout.LayoutParams(0, -2, 1));
        durations.addView(r1);
        LinearLayout r2 = horizontal();
        r2.addView(fieldColumn("عدد جلسات المذاكرة\nقبل الاستراحة الطويلة", "long_after", 4, R.drawable.ic_sessions_cycle, false), new LinearLayout.LayoutParams(0, -2, 1));
        r2.addView(fieldColumn("مدة الاستراحة الطويلة", "long_minutes", 15, R.drawable.ic_bed, true), new LinearLayout.LayoutParams(0, -2, 1));
        durations.addView(r2);

        // 2. الانتقال التلقائي
        LinearLayout auto = card(page);
        sectionTitle(auto, "الانتقال التلقائي", R.drawable.ic_zap);
        addSwitch(auto, "من المذاكرة إلى الاستراحة القصيرة", "auto_short");
        addSwitch(auto, "من الاستراحة القصيرة إلى المذاكرة", "auto_study");
        addSwitch(auto, "من الاستراحة الطويلة إلى المذاكرة", "auto_long");

        // 3. صوت التنبيه ومستوى التنبيه (مماثل لـ Flocus تماماً وقبل السمة)
        LinearLayout alertCard = card(page);
        sectionTitle(alertCard, "صوت التنبيه", R.drawable.ic_bell);

        TextView alertDesc = txt("يعمل عند انتهاء فترة المؤقت. اضغط للمعاينة.", 13, "#C4B8FF", false);
        alertDesc.setGravity(Gravity.RIGHT);
        alertCard.addView(alertDesc, lp(-1, -2));

        String currentSoundKey = prefs.getString("alert_sound_key", "sparkle");
        String currentSoundName = nameForSoundKey(currentSoundKey);
        int currentSoundIcon = iconResForSoundKey(currentSoundKey);

        // شريط الصوت المختار (Picker Summary)
        LinearLayout summaryBox = horizontal();
        summaryBox.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        summaryBox.setGravity(Gravity.CENTER_VERTICAL);
        summaryBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        summaryBox.setBackground(rounded(Color.argb(85, 81, 70, 216), 16, 1));
        LinearLayout.LayoutParams sbp = lp(-1, -2);
        sbp.topMargin = dp(10);
        alertCard.addView(summaryBox, sbp);

        TextView selectedLabel = txt("محدد:", 14, "#C4B8FF", false);
        summaryBox.addView(selectedLabel);

        ImageView currentIconView = new ImageView(this);
        currentIconView.setImageResource(currentSoundIcon);
        LinearLayout.LayoutParams civp = lp(24, 24);
        civp.rightMargin = dp(8);
        civp.leftMargin = dp(6);
        summaryBox.addView(currentIconView, civp);

        TextView selectedNameView = txt(currentSoundName, 15, "#FFFFFF", true);
        summaryBox.addView(selectedNameView, new LinearLayout.LayoutParams(0, -2, 1));

        TextView changeBtn = txt("تغيير", 13, "#FFFFFF", true);
        changeBtn.setGravity(Gravity.CENTER);
        changeBtn.setPadding(dp(14), dp(6), dp(14), dp(6));
        changeBtn.setBackground(rounded(Color.argb(70, 255, 255, 255), 14, 1));
        summaryBox.addView(changeBtn);

        // شبكة الأصوات المنسدلة (Expandable Grid)
        LinearLayout soundGrid = vertical();
        soundGrid.setVisibility(View.GONE);
        LinearLayout.LayoutParams sgp = lp(-1, -2);
        sgp.topMargin = dp(12);
        alertCard.addView(soundGrid, sgp);

        List<LinearLayout> soundTiles = new ArrayList<>();
        Runnable refreshSoundTiles = () -> {
            String activeKey = prefs.getString("alert_sound_key", "sparkle");
            for (int idx = 0; idx < soundTiles.size(); idx++) {
                LinearLayout tile = soundTiles.get(idx);
                AlertSoundItem item = SOUND_ITEMS[idx];
                boolean isSel = item.key.equals(activeKey);
                GradientDrawable gd = new GradientDrawable();
                if (isSel) {
                    gd.setColor(Color.argb(135, 81, 70, 216));
                    gd.setCornerRadius(dp(14));
                    gd.setStroke(dp(2), Color.WHITE);
                } else {
                    gd.setColor(Color.argb(85, 20, 20, 38));
                    gd.setCornerRadius(dp(14));
                    gd.setStroke(dp(1), Color.argb(45, 255, 255, 255));
                }
                tile.setBackground(gd);
            }
        };

        LinearLayout currentRow = null;
        for (int i = 0; i < SOUND_ITEMS.length; i++) {
            if (i % 3 == 0) {
                currentRow = horizontal();
                currentRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                LinearLayout.LayoutParams crp = lp(-1, -2);
                crp.topMargin = dp(i == 0 ? 0 : 8);
                soundGrid.addView(currentRow, crp);
            }
            AlertSoundItem item = SOUND_ITEMS[i];
            LinearLayout tile = vertical();
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(6), dp(10), dp(6), dp(10));

            ImageView itemIcon = new ImageView(this);
            itemIcon.setImageResource(item.iconRes);
            LinearLayout.LayoutParams iip = lp(32, 32);
            iip.bottomMargin = dp(6);
            tile.addView(itemIcon, iip);

            TextView itemName = txt(item.name, 12, "#FFFFFF", true);
            itemName.setGravity(Gravity.CENTER);
            itemName.setSingleLine(true);
            tile.addView(itemName);

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(82), 1);
            tlp.setMargins(dp(3), 0, dp(3), 0);
            currentRow.addView(tile, tlp);
            soundTiles.add(tile);

            tile.setOnClickListener(v -> {
                prefs.edit().putString("alert_sound_key", item.key)
                        .putString("alert_sound_name", item.name).apply();
                selectedNameView.setText(item.name);
                currentIconView.setImageResource(item.iconRes);
                refreshSoundTiles.run();
                playAlertPreview(item.rawRes);
            });
        }
        if (SOUND_ITEMS.length % 3 != 0 && currentRow != null) {
            int remaining = 3 - (SOUND_ITEMS.length % 3);
            for (int r = 0; r < remaining; r++) {
                View placeholder = new View(this);
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, 0, 1);
                plp.setMargins(dp(3), 0, dp(3), 0);
                currentRow.addView(placeholder, plp);
            }
        }
        refreshSoundTiles.run();

        changeBtn.setOnClickListener(v -> {
            boolean isVisible = soundGrid.getVisibility() == View.VISIBLE;
            soundGrid.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            changeBtn.setText(isVisible ? "تغيير" : "إغلاق");
        });

        // سلايدر مستوى التنبيه
        LinearLayout volumeSection = vertical();
        LinearLayout.LayoutParams vsp = lp(-1, -2);
        vsp.topMargin = dp(16);
        alertCard.addView(volumeSection, vsp);

        LinearLayout volHeader = horizontal();
        volHeader.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        volHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView volTitle = txt("مستوى التنبيه", 14, "#FFFFFF", true);
        volHeader.addView(volTitle);
        View volSpacer = new View(this);
        volHeader.addView(volSpacer, new LinearLayout.LayoutParams(0, 0, 1));
        int currentVol = prefs.getInt("alert_volume_percent", 80);
        TextView volPercentText = txt(currentVol + "%", 13, "#C4B8FF", false);
        volHeader.addView(volPercentText);
        volumeSection.addView(volHeader, lp(-1, -2));

        SeekBar volumeBar = new SeekBar(this);
        volumeBar.setMax(100);
        volumeBar.setProgress(currentVol);
        if (Build.VERSION.SDK_INT >= 21) {
            volumeBar.setProgressTintList(ColorStateList.valueOf(c("#7432FF")));
            volumeBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.argb(90, 110, 110, 150)));
            volumeBar.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        }
        LinearLayout.LayoutParams vbp = lp(-1, -2);
        vbp.topMargin = dp(8);
        volumeSection.addView(volumeBar, vbp);

        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    volPercentText.setText(progress + "%");
                    prefs.edit().putInt("alert_volume_percent", progress)
                            .putFloat("alert_volume", progress / 100f).apply();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                String key = prefs.getString("alert_sound_key", "sparkle");
                int raw = rawResForSoundKey(key);
                playAlertPreview(raw);
            }
        });

        // 4. السمة
        LinearLayout theme = card(page);
        LinearLayout themeHeader = horizontal();
        themeHeader.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        themeHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView addImage = txt("＋  صورة من الجهاز", 14, "#FFFFFF", true);
        addImage.setGravity(Gravity.CENTER);
        addImage.setBackground(rounded(c("#6252D0"), 20, 0));
        themeHeader.addView(addImage, lp(145, 38));
        addImage.setOnClickListener(v -> pickImage());
        View themeSpacer = new View(this);
        themeHeader.addView(themeSpacer, new LinearLayout.LayoutParams(0, 0, 1));
        TextView themeTitle = txt("السمة", compact ? 16 : 18, "#FFFFFF", true);
        themeTitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        themeHeader.addView(themeTitle);
        ImageView themeIcon = new ImageView(this);
        themeIcon.setImageResource(R.drawable.ic_image);
        themeIcon.setColorFilter(c("#A88BFF"));
        LinearLayout.LayoutParams tip = lp(24, 24);
        tip.leftMargin = dp(8);
        themeHeader.addView(themeIcon, tip);
        theme.addView(themeHeader, lp(-1, 40));

        themeGrid = vertical();
        LinearLayout.LayoutParams gp = lp(-1, -2);
        gp.topMargin = dp(8);
        theme.addView(themeGrid, gp);

        moreThemes = txt("إظهار المزيد", 16, "#FFFFFF", true);
        moreThemes.setGravity(Gravity.CENTER);
        moreThemes.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_chevron_down, 0, 0, 0);
        moreThemes.setCompoundDrawablePadding(dp(8));
        moreThemes.setBackground(rounded(c("#6252D0"), 26, 0));
        LinearLayout.LayoutParams mp = lp(-1, 46);
        mp.topMargin = dp(10);
        theme.addView(moreThemes, mp);
        moreThemes.setOnClickListener(v -> {
            expanded = !expanded;
            renderThemes();
            moreThemes.setText(expanded ? "إظهار أقل" : "إظهار المزيد");
        });

        renderThemes();
        setContentView(root);
        study.clearFocus();
        shortBreak.clearFocus();
        longBreak.clearFocus();
        cycle.clearFocus();
        page.setFocusableInTouchMode(true);
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout box = vertical();
        box.setPadding(dp(12), dp(8), dp(12), dp(12));
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        if (isDark) {
            box.setBackground(rounded(Color.argb(56, 255, 255, 255), 20, 1));
        } else {
            box.setBackground(rounded(Color.parseColor("#202542"), 20, 0));
        }
        LinearLayout.LayoutParams p = lp(-1, -2);
        p.topMargin = dp(12);
        parent.addView(box, p);
        return box;
    }

    private void sectionTitle(LinearLayout parent, String title, int icon) {
        LinearLayout row = horizontal();
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView t = txt(title, compact ? 16 : 18, "#FFFFFF", true);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(t);
        ImageView im = new ImageView(this);
        im.setImageResource(icon);
        im.setColorFilter(c("#A88BFF"));
        LinearLayout.LayoutParams ip = lp(24, 24);
        ip.leftMargin = dp(8);
        row.addView(im, ip);
        LinearLayout.LayoutParams rp = lp(-1, 38);
        rp.bottomMargin = dp(6);
        parent.addView(row, rp);
    }

    private LinearLayout fieldColumn(String title, String key, int def, int icon, boolean showMin) {
        LinearLayout col = vertical();
        col.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout titleRow = horizontal();
        titleRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        titleRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView t = txt(title, compact ? 12 : 13, "#C4B8FF", false);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setSingleLine(false);
        t.setMaxLines(2);
        titleRow.addView(t, new LinearLayout.LayoutParams(-2, -2));
        if (icon != 0) {
            ImageView ic = new ImageView(this);
            ic.setImageResource(icon);
            ic.setColorFilter(c("#A88BFF"));
            LinearLayout.LayoutParams icLp = lp(18, 18);
            icLp.leftMargin = dp(6);
            titleRow.addView(ic, icLp);
        }
        col.addView(titleRow, lp(-1, 36));

        int initial = prefs.getInt(key, def);
        EditText e = new EditText(this);
        e.setText(String.valueOf(initial));
        e.setTextSize(compact ? 20 : 22);
        e.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        e.setTextColor(Color.WHITE);
        e.setGravity(Gravity.CENTER);
        e.setBackground(null);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        e.setSelectAllOnFocus(true);

        if (key.equals("study_minutes")) study = e;
        else if (key.equals("short_minutes")) shortBreak = e;
        else if (key.equals("long_minutes")) longBreak = e;
        else cycle = e;

        LinearLayout split = horizontal();
        split.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        split.setBackground(rounded(c("#5146D8"), 14, 0));

        if (showMin) {
            TextView minLabel = txt("دقيقة", 13, "#FFFFFF", false);
            minLabel.setGravity(Gravity.CENTER);
            minLabel.setBackground(roundedPart(Color.argb(90, 0, 0, 0), false));
            split.addView(minLabel, lp(42, 48));

            FrameLayout inputWrap = new FrameLayout(this);
            inputWrap.setBackground(roundedPart(Color.argb(45, 255, 255, 255), true));
            inputWrap.addView(e, new FrameLayout.LayoutParams(-1, -1));
            split.addView(inputWrap, new LinearLayout.LayoutParams(0, dp(48), 1));
        } else {
            FrameLayout inputWrap = new FrameLayout(this);
            inputWrap.addView(e, new FrameLayout.LayoutParams(-1, -1));
            split.addView(inputWrap, lp(-1, 48));
        }

        col.addView(split, lp(-1, 48));

        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    int val = Integer.parseInt(s.toString().trim());
                    if (val >= 1 && val <= 600) prefs.edit().putInt(key, val).apply();
                } catch (Exception ignored) {}
            }
        });
        return col;
    }

    private void addSwitch(LinearLayout parent, String label, String key) {
        LinearLayout row = horizontal();
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(key, true));
        int purple = c("#5146D8");
        int trackColor = Color.argb(120, 81, 70, 216);
        if (Build.VERSION.SDK_INT >= 23) {
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
            };
            int[] thumbColors = new int[]{purple, Color.parseColor("#E0E0E0")};
            int[] trackColors = new int[]{trackColor, Color.argb(90, 255, 255, 255)};
            sw.setThumbTintList(new ColorStateList(states, thumbColors));
            sw.setTrackTintList(new ColorStateList(states, trackColors));
        }
        row.addView(sw);
        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1));
        TextView t = txt(label, compact ? 13 : 14, "#FFFFFF", false);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(t);
        sw.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(key, checked).apply());
        parent.addView(row, lp(-1, 44));
    }

    private void renderThemes() {
        themeGrid.removeAllViews();
        int tileH = compact ? 110 : 124;
        List<FrameLayout> tiles = new ArrayList<>();
        String deviceImage = prefs.getString("device_image_uri", prefs.getString("custom_background_uri", null));
        if (deviceImage != null) {
            tiles.add(deviceImageTile(deviceImage, tileH));
        }
        for (int i = 0; i < themeNames.length; i++) {
            tiles.add(themeTile(i, tileH));
        }
        int max = expanded ? tiles.size() : Math.min(compact ? 4 : 4, tiles.size());
        for (int i = 0; i < max; i += 2) {
            LinearLayout row = horizontal();
            LinearLayout.LayoutParams rp = lp(-1, -2);
            rp.topMargin = dp(i == 0 ? 0 : 8);
            row.addView(tiles.get(i), new LinearLayout.LayoutParams(0, dp(tileH), 1));
            View spacer = new View(this);
            row.addView(spacer, lp(8, tileH));
            if (i + 1 < max) {
                row.addView(tiles.get(i + 1), new LinearLayout.LayoutParams(0, dp(tileH), 1));
            } else {
                View empty = new View(this);
                row.addView(empty, new LinearLayout.LayoutParams(0, dp(tileH), 1));
            }
            themeGrid.addView(row, rp);
        }
    }

    private FrameLayout deviceImageTile(String uriString, int tileH) {
        FrameLayout tile = new FrameLayout(this);
        tile.setClipToOutline(true);
        tile.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        tile.setBackground(rounded(Color.argb(90, 20, 20, 36), 14, 0));

        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(Uri.parse(uriString))
                .apply(decoded(dp(240), dp(140)))
                .into(iv);
        tile.addView(iv, new FrameLayout.LayoutParams(-1, -1));

        View scrim = new View(this);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.argb(190, 10, 10, 22)});
        scrim.setBackground(gd);
        tile.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        TextView tv = txt("صورة الجهاز", compact ? 12 : 13, "#FFFFFF", true);
        tv.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(-1, -2);
        tvLp.gravity = Gravity.BOTTOM;
        tvLp.bottomMargin = dp(8);
        tile.addView(tv, tvLp);

        boolean isSelected = prefs.getString("custom_background_uri", null) != null;
        if (isSelected) {
            View selected = new View(this);
            GradientDrawable stroke = new GradientDrawable();
            stroke.setColor(Color.TRANSPARENT);
            stroke.setCornerRadius(dp(14));
            stroke.setStroke(dp(3), c("#FFFFFF"));
            selected.setBackground(stroke);
            tile.addView(selected, new FrameLayout.LayoutParams(-1, -1));
        }

        tile.setOnClickListener(v -> {
            prefs.edit().putString("custom_background_uri", uriString).apply();
            renderThemes();
            sendBroadcast(new Intent("com.focus.app.THEME_CHANGED"));
            Toast.makeText(this, "تم تطبيق صورة الجهاز", Toast.LENGTH_SHORT).show();
        });
        return tile;
    }

    private FrameLayout themeTile(int index, int tileH) {
        String name = themeNames[index];
        String file = themeFiles[index];
        FrameLayout tile = new FrameLayout(this);
        tile.setClipToOutline(true);
        tile.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        tile.setBackground(rounded(Color.argb(90, 20, 20, 36), 14, 0));

        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadImage(file, iv);
        tile.addView(iv, new FrameLayout.LayoutParams(-1, -1));

        View scrim = new View(this);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.argb(190, 10, 10, 22)});
        scrim.setBackground(gd);
        tile.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        TextView tv = txt(name, compact ? 12 : 13, "#FFFFFF", true);
        tv.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(-1, -2);
        tvLp.gravity = Gravity.BOTTOM;
        tvLp.bottomMargin = dp(8);
        tile.addView(tv, tvLp);

        boolean isSelected = prefs.getString("custom_background_uri", null) == null
                && name.equals(prefs.getString("selected_theme", "Morning Sun"));
        if (isSelected) {
            View selected = new View(this);
            GradientDrawable stroke = new GradientDrawable();
            stroke.setColor(Color.TRANSPARENT);
            stroke.setCornerRadius(dp(14));
            stroke.setStroke(dp(3), c("#FFFFFF"));
            selected.setBackground(stroke);
            tile.addView(selected, new FrameLayout.LayoutParams(-1, -1));
        }
        tile.setOnClickListener(v -> {
            prefs.edit().putString("selected_theme", name).remove("custom_background_uri").apply();
            renderThemes();
            sendBroadcast(new Intent("com.focus.app.THEME_CHANGED"));
            Toast.makeText(this, "تم تطبيق " + name, Toast.LENGTH_SHORT).show();
        });
        return tile;
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(i, 203); }
        catch (Exception e) { Toast.makeText(this, "تعذر فتح الصور", Toast.LENGTH_SHORT).show(); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == 203) {
            Uri uri = data.getData();
            if (uri == null) return;
            try {
                getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            } catch (Exception ignored) {}
            prefs.edit()
                    .putString("device_image_uri", uri.toString())
                    .putString("custom_background_uri", uri.toString())
                    .apply();
            renderThemes();
            sendBroadcast(new Intent("com.focus.app.THEME_CHANGED"));
            Toast.makeText(this, "تم تطبيق صورة الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSettings() {
        try {
            int a = valid(study), b = valid(shortBreak), d = valid(longBreak), e = valid(cycle);
            int currentPhase = prefs.getInt("phase", 0);
            boolean isRunning = prefs.getBoolean("running", false);
            long now = System.currentTimeMillis();
            long oldTotal = prefs.getLong("session_duration_ms", Math.max(1, prefs.getInt(currentPhase == 0 ? "study_minutes" : currentPhase == 1 ? "short_minutes" : "long_minutes", currentPhase == 0 ? 25 : currentPhase == 1 ? 5 : 15)) * 60000L);
            long oldLeft = isRunning ? Math.max(0, prefs.getLong("end_at", now) - now) : prefs.getLong("remaining_ms", oldTotal);
            long elapsed = Math.max(0, oldTotal - oldLeft);
            long newTotal = (currentPhase == 0 ? a : currentPhase == 1 ? b : d) * 60000L;
            long newLeft = Math.max(0, newTotal - elapsed);
            SharedPreferences.Editor editor = prefs.edit()
                    .putInt("study_minutes", a).putInt("short_minutes", b).putInt("long_minutes", d).putInt("long_after", e)
                    .putLong("session_duration_ms", newTotal).putLong("remaining_ms", newLeft);
            if (isRunning) editor.putLong("end_at", now + newLeft);
            editor.apply();
            if (isRunning) {
                Intent refresh = new Intent(this, TimerService.class);
                refresh.setAction(TimerService.ACTION_REFRESH);
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(refresh); else startService(refresh);
            }
            Toast.makeText(this, "تم حفظ الإعدادات وتحديث الفترة الحالية", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception ex) {
            Toast.makeText(this, "أدخل أرقامًا صحيحة من 1 إلى 600", Toast.LENGTH_SHORT).show();
        }
    }

    private int valid(EditText e) {
        int v = Integer.parseInt(e.getText().toString().trim());
        if (v < 1 || v > 600) throw new IllegalArgumentException();
        return v;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView txt(String value, int size, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(c(color));
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }

    @Override protected void onDestroy() {
        if (previewPlayer != null) {
            try { previewPlayer.stop(); previewPlayer.release(); } catch (Exception ignored) {}
            previewPlayer = null;
        }
        super.onDestroy();
    }
}
