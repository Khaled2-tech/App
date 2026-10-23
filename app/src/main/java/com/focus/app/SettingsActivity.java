package com.focus.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private android.content.SharedPreferences prefs;
    private EditText study, shortBreak, longBreak, cycle;
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("focus_prefs", MODE_PRIVATE);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24),dp(24),dp(24),dp(24)); root.setBackgroundColor(Color.parseColor("#F6F4EF"));
        TextView title = new TextView(this); title.setText("الإعدادات"); title.setTextSize(27); title.setTextColor(Color.parseColor("#304A42"));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(64)));
        study = field(root, "مدة الدراسة بالدقائق", "study_minutes", 25);
        shortBreak = field(root, "مدة الاستراحة القصيرة بالدقائق", "short_minutes", 5);
        longBreak = field(root, "مدة الاستراحة الطويلة بالدقائق", "long_minutes", 15);
        cycle = field(root, "عدد جلسات الدراسة قبل الاستراحة الطويلة", "long_after", 4);
        Button save = new Button(this); save.setText("حفظ الإعدادات"); save.setAllCaps(false);
        save.setTextColor(Color.WHITE); save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#49685E")));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52)); p.topMargin = dp(22); root.addView(save,p);
        save.setOnClickListener(v -> {
            try {
                int a = valid(study), c = valid(shortBreak), d = valid(longBreak), e = valid(cycle);
                prefs.edit().putInt("study_minutes",a).putInt("short_minutes",c).putInt("long_minutes",d).putInt("long_after",e).apply();
                Toast.makeText(this, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show(); finish();
            } catch (Exception ex) { Toast.makeText(this, "أدخل أرقامًا صحيحة أكبر من صفر", Toast.LENGTH_SHORT).show(); }
        });
        setContentView(root);
    }
    private EditText field(LinearLayout root, String label, String key, int def) {
        TextView t = new TextView(this); t.setText(label); t.setTextSize(15); t.setTextColor(Color.parseColor("#49685E"));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(34)); tp.topMargin = dp(12); root.addView(t,tp);
        EditText e = new EditText(this); e.setSingleLine(true); e.setInputType(2); e.setText(String.valueOf(prefs.getInt(key,def)));
        root.addView(e,new LinearLayout.LayoutParams(-1,dp(48))); return e;
    }
    private int valid(EditText e) { int v = Integer.parseInt(e.getText().toString().trim()); if (v < 1 || v > 600) throw new IllegalArgumentException(); return v; }
}
