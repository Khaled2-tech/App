package com.focus.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private EditText study, shortBreak, longBreak, cycle;
    private TextView soundValue, moreThemes;
    private LinearLayout themeGrid;
    private boolean expanded;
    private boolean compact;
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
    private TextView txt(String value, int size, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(c(color));
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }

    private RequestOptions decoded(int w, int h) {
        return new RequestOptions().format(DecodeFormat.PREFER_RGB_565).centerCrop().override(Math.max(1, w), Math.max(1, h));
    }
    private void loadImage(String file, ImageView target) {
        Glide.with(this).load(Uri.parse("file:///android_asset/themes/" + file))
                .apply(decoded(dp(240), dp(140)))
                .into(target);
    }
    private void loadWallpaper(ImageView target) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        String custom = prefs.getString("custom_background_uri", null);
        Object source = custom != null ? Uri.parse(custom) : Uri.parse("file:///android_asset/themes/" + MainActivity.fileFor(prefs.getString("selected_theme", "Morning Sun")));
        Glide.with(this).load(source).apply(decoded(dm.widthPixels, dm.heightPixels)).into(target);
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        prefs = getSharedPreferences("focus_prefs", MODE_PRIVATE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        compact = dm.widthPixels <= 720 || Math.round(dm.heightPixels / dm.density) <= 700;
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(c("#14213F"));
            getWindow().setNavigationBarColor(c("#14213F"));
        }
        FrameLayout root = new FrameLayout(this);
        ImageView wallpaper = new ImageView(this);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadWallpaper(wallpaper);
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(150, 8, 17, 43));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = vertical();
        page.setPadding(dp(compact ? 12 : 18), dp(10), dp(compact ? 12 : 18), dp(24));
        scroll.addView(page);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = horizontal();
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        TextView save = txt("حفظ", 16, "#FFFFFF", true);
        save.setGravity(Gravity.CENTER);
        save.setBackground(rounded(c("#5146D8"), 17, 0));
        header.addView(save, lp(64, 46));
        TextView title = txt("الإعدادات", compact ? 20 : 24, "#FFFFFF", true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(46), 1));
        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_chevron_left);
        back.setPadding(dp(10), dp(10), dp(10), dp(10));
        back.setBackground(rounded(Color.argb(95, 255, 255, 255), 17, 1));
        header.addView(back, lp(44, 46));
        page.addView(header);
        save.setOnClickListener(v -> saveSettings());
        back.setOnClickListener(v -> finish());

        LinearLayout durations = card(page);
        sectionTitle(durations, "مدة الفترات", R.drawable.ic_clock);
        LinearLayout r1 = horizontal();
        r1.addView(fieldColumn("مدة المذاكرة", "study_minutes", 25, R.drawable.ic_book_open, true), new LinearLayout.LayoutParams(0, -2, 1));
        r1.addView(fieldColumn("مدة الاستراحة القصيرة", "short_minutes", 5, R.drawable.ic_coffee, true), new LinearLayout.LayoutParams(0, -2, 1));
        durations.addView(r1);
        LinearLayout r2 = horizontal();
        r2.addView(fieldColumn("عدد جلسات المذاكرة قبل الاستراحة الطويلة", "long_after", 4, R.drawable.ic_sessions_cycle, false), new LinearLayout.LayoutParams(0, -2, 1));
        r2.addView(fieldColumn("مدة الاستراحة الطويلة", "long_minutes", 15, R.drawable.ic_bed, true), new LinearLayout.LayoutParams(0, -2, 1));
        durations.addView(r2);

        LinearLayout auto = card(page);
        sectionTitle(auto, "الانتقال التلقائي", R.drawable.ic_zap);
        addSwitch(auto, "من المذاكرة إلى الاستراحة القصيرة", "auto_short");
        addSwitch(auto, "من الاستراحة القصيرة إلى المذاكرة", "auto_study");
        addSwitch(auto, "من الاستراحة الطويلة إلى المذاكرة", "auto_long");

        LinearLayout alert = card(page);
        sectionTitle(alert, "التنبيه", R.drawable.ic_bell);
        soundValue = txt(prefs.getString("alert_sound_name", "اختر نغمة من الجهاز"), 14, "#F5F2FF", false);
        soundValue.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        soundValue.setPadding(dp(14), 0, dp(14), 0);
        soundValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_chevron_left, 0, 0, 0);
        soundValue.setBackground(rounded(Color.argb(112, 255, 255, 255), 25, 1));
        alert.addView(soundValue, lp(-1, 48));
        soundValue.setOnClickListener(v -> pickRingtone());
        TextView addSound = txt("＋   إضافة صوت من الجهاز", 15, "#FFFFFF", true);
        addSound.setGravity(Gravity.CENTER);
        addSound.setBackground(rounded(c("#6252D0"), 28, 0));
        LinearLayout.LayoutParams asp = lp(-1, 48);
        asp.topMargin = dp(10);
        alert.addView(addSound, asp);
        addSound.setOnClickListener(v -> pickAudio());

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
        page.requestFocus();
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout box = vertical();
        box.setPadding(dp(12), dp(8), dp(12), dp(12));
        box.setBackground(rounded(Color.argb(56, 255, 255, 255), 20, 1));
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
        parent.addView(row, lp(-1, 36));
    }
    private LinearLayout fieldColumn(String label, String key, int def, int icon, boolean minutes) {
        LinearLayout col = vertical();
        col.setPadding(dp(3), dp(2), dp(3), dp(4));
        LinearLayout heading = horizontal();
        heading.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView t = txt(label, compact ? 11 : 13, "#F1EFFF", false);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setMaxLines(3);
        heading.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView im = new ImageView(this);
        im.setImageResource(icon);
        im.setColorFilter(c("#B39AFF"));
        LinearLayout.LayoutParams ip = lp(20, 20);
        ip.leftMargin = dp(6);
        heading.addView(im, ip);
        col.addView(heading);
        int initial = prefs.getInt(key, def);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(rounded(Color.argb(100, 255, 255, 255), 14, 1));
        TextView unit = txt(minutes ? minuteUnit(initial) : sessionUnit(initial), 11, "#E6E0FF", false);
        unit.setGravity(Gravity.CENTER_VERTICAL);
        unit.setPadding(dp(8), 0, dp(4), 0);
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(compact ? 16 : 18);
        e.setTextColor(Color.WHITE);
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        e.setInputType(2);
        e.setIncludeFontPadding(false);
        e.setPadding(dp(8), 0, dp(8), 0);
        e.setBackgroundColor(Color.TRANSPARENT);
        e.setText(String.valueOf(initial));
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                int n = 0;
                try { n = Integer.parseInt(s.toString().trim()); } catch (Exception ignored) {}
                unit.setText(minutes ? minuteUnit(n) : sessionUnit(n));
            }
        });
        box.addView(e, new FrameLayout.LayoutParams(-1, -1));
        box.addView(unit, new FrameLayout.LayoutParams(-2, -1, Gravity.LEFT | Gravity.CENTER_VERTICAL));
        col.addView(box, lp(-1, compact ? 42 : 46));
        if (key.equals("study_minutes")) study = e;
        if (key.equals("short_minutes")) shortBreak = e;
        if (key.equals("long_minutes")) longBreak = e;
        if (key.equals("long_after")) cycle = e;
        return col;
    }
    private String minuteUnit(int n) {
        if (n == 2) return "دقيقتان";
        if (n >= 3 && n <= 10) return "دقائق";
        return "دقيقة";
    }
    private String sessionUnit(int n) {
        if (n == 2) return "جلستان";
        if (n >= 3 && n <= 10) return "جلسات";
        return "جلسة";
    }
    private void addSwitch(LinearLayout parent, String label, String key) {
        LinearLayout row = horizontal();
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        Switch sw = new Switch(this);
        int[][] states = new int[][] {
            new int[] { android.R.attr.state_checked },
            new int[] {}
        };
        int[] thumbColors = new int[] { c("#5146D8"), Color.LTGRAY };
        int[] trackColors = new int[] { Color.argb(128, 81, 70, 216), Color.argb(60, 255, 255, 255) };
        sw.setThumbTintList(new ColorStateList(states, thumbColors));
        sw.setTrackTintList(new ColorStateList(states, trackColors));
        sw.setChecked(prefs.getBoolean(key, true));
        row.addView(sw, lp(52, 44));
        TextView t = txt(label, compact ? 12 : 14, "#F5F2FF", false);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(t, new LinearLayout.LayoutParams(0, dp(44), 1));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_transition);
        icon.setColorFilter(c("#B39AFF"));
        LinearLayout.LayoutParams ip = lp(22, 22);
        ip.leftMargin = dp(8);
        row.addView(icon, ip);
        sw.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(key, checked).apply());
        parent.addView(row, lp(-1, 46));
    }
    private void renderThemes() {
        themeGrid.removeAllViews();
        int count = expanded ? themeNames.length : 4;
        int tileH = compact ? 92 : 108;
        for (int i = 0; i < count; i += 2) {
            LinearLayout row = horizontal();
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(tileH), 1);
            p1.setMargins(dp(3), 0, dp(3), 0);
            row.addView(themeTile(i, tileH), p1);
            if (i + 1 < count) {
                LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(tileH), 1);
                p2.setMargins(dp(3), 0, dp(3), 0);
                row.addView(themeTile(i + 1, tileH), p2);
            }
            LinearLayout.LayoutParams rp = lp(-1, tileH);
            rp.topMargin = dp(6);
            themeGrid.addView(row, rp);
        }
    }
    private FrameLayout themeTile(int index, int tileH) {
        String name = themeNames[index];
        String file = themeFiles[index];
        FrameLayout tile = new FrameLayout(this);
        tile.setBackground(rounded(Color.DKGRAY, 15, 1));
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            tile.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(15));
                }
            });
            tile.setClipToOutline(true);
        }
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadImage(file, image);
        tile.addView(image, new FrameLayout.LayoutParams(-1, -1));
        TextView caption = txt(name, 13, "#FFFFFF", true);
        caption.setGravity(Gravity.CENTER);
        caption.setShadowLayer(dp(3), 0, dp(1), Color.BLACK);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, dp(28), Gravity.BOTTOM);
        tile.addView(caption, cp);
        if (name.equals(prefs.getString("selected_theme", "Morning Sun"))) {
            View selected = new View(this);
            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.TRANSPARENT);
            border.setCornerRadius(dp(15));
            border.setStroke(dp(2), c("#A991FF"));
            selected.setBackground(border);
            tile.addView(selected, new FrameLayout.LayoutParams(-1, -1));
        }
        tile.setOnClickListener(v -> {
            prefs.edit().putString("selected_theme", name).remove("custom_background_uri").apply();
            renderThemes();
            sendBroadcast(new Intent("com.focus.app.THEME_CHANGED"));
            Toast.makeText(this, "تم اختيار " + name, Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(tileH), 1);
        p.setMargins(dp(3), 0, dp(3), 0);
        tile.setLayoutParams(p);
        return tile;
    }
    private void pickRingtone() {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة التنبيه");
        String current = prefs.getString("alert_sound_uri", null);
        if (current != null) i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current));
        startActivityForResult(i, 201);
    }
    private void pickAudio() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("audio/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(i, 202); }
        catch (Exception e) { Toast.makeText(this, "تعذر فتح ملفات الصوت", Toast.LENGTH_SHORT).show(); }
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
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (requestCode == 201) {
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            prefs.edit().putString("alert_sound_uri", uri.toString()).putString("alert_sound_name", "نغمة من الجهاز").apply();
            soundValue.setText("نغمة من الجهاز");
        } else if (requestCode == 202 || requestCode == 203) {
            uri = data.getData();
            if (uri == null) return;
            try {
                getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            } catch (Exception ignored) {}
            if (requestCode == 202) {
                prefs.edit().putString("custom_sound_uri", uri.toString()).putString("alert_sound_name", "صوت مخصص من الجهاز").apply();
                soundValue.setText("صوت مخصص من الجهاز");
            } else {
                prefs.edit().putString("custom_background_uri", uri.toString()).apply();
                sendBroadcast(new Intent("com.focus.app.THEME_CHANGED"));
                Toast.makeText(this, "تم تحديث الخلفية", Toast.LENGTH_SHORT).show();
            }
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
}
