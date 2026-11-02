package com.focus.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import android.content.SharedPreferences;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private EditText study, shortBreak, longBreak, cycle;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int c(String s){return Color.parseColor(s);}
    private GradientDrawable shape(String color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(c(color));d.setCornerRadius(dp(radius));return d;}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);prefs=getSharedPreferences("focus_prefs",MODE_PRIVATE);
        getWindow().setStatusBarColor(c("#F8D9E0"));getWindow().setNavigationBarColor(c("#F8D9E0"));
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll=new ScrollView(this);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(24),dp(18),dp(24),dp(26));
        page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{c("#F8D9E0"),c("#E7D6E8"),c("#C4D1D8")}));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=new TextView(this);back.setText("‹");back.setTextSize(38);back.setTextColor(c("#984BC0"));back.setGravity(Gravity.CENTER);back.setBackground(shape("#FFF7FB",50));
        header.addView(back,new LinearLayout.LayoutParams(dp(54),dp(54)));back.setOnClickListener(v->finish());
        TextView title=new TextView(this);title.setText("الإعدادات");title.setTextSize(26);title.setTypeface(null,Typeface.BOLD);title.setTextColor(c("#30358F"));title.setGravity(Gravity.CENTER);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(58),1));page.addView(header);
        TextView intro=new TextView(this);intro.setText("خصّص أوقات جلساتك وفترات الراحة");intro.setTextSize(15);intro.setTextColor(c("#68699D"));intro.setGravity(Gravity.CENTER);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(54));ip.topMargin=dp(10);page.addView(intro,ip);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(14),dp(18),dp(20));card.setBackground(shape("#F8F2F8",30));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(12);page.addView(card,cp);
        TextView section=new TextView(this);section.setText("مدد الجلسات");section.setTextSize(19);section.setTypeface(null,Typeface.BOLD);section.setTextColor(c("#34378F"));section.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);card.addView(section,new LinearLayout.LayoutParams(-1,dp(48)));
        study=field(card,"مدة جلسة المذاكرة (دقيقة)","study_minutes",25);
        shortBreak=field(card,"مدة الاستراحة القصيرة (دقيقة)","short_minutes",5);
        longBreak=field(card,"مدة الاستراحة الطويلة (دقيقة)","long_minutes",15);
        cycle=field(card,"جلسات المذاكرة قبل الاستراحة الطويلة","long_after",4);
        Button save=new Button(this);save.setText("حفظ الإعدادات");save.setAllCaps(false);save.setTextSize(17);save.setTextColor(Color.WHITE);save.setBackground(shape("#C34FC0",40));
        LinearLayout.LayoutParams savep=new LinearLayout.LayoutParams(-1,dp(58));savep.topMargin=dp(22);page.addView(save,savep);
        save.setOnClickListener(v->{try{int a=valid(study),b1=valid(shortBreak),d=valid(longBreak),e=valid(cycle);prefs.edit().putInt("study_minutes",a).putInt("short_minutes",b1).putInt("long_minutes",d).putInt("long_after",e).apply();Toast.makeText(this,"تم حفظ الإعدادات",Toast.LENGTH_SHORT).show();finish();}catch(Exception ex){Toast.makeText(this,"أدخل أرقامًا صحيحة أكبر من صفر",Toast.LENGTH_SHORT).show();}});
        scroll.addView(page);setContentView(scroll);
    }
    private EditText field(LinearLayout parent,String label,String key,int def){
        TextView t=new TextView(this);t.setText(label);t.setTextSize(15);t.setTextColor(c("#41418F"));t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(34));tp.topMargin=dp(9);parent.addView(t,tp);
        EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(17);e.setTextColor(c("#34378F"));e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);e.setInputType(2);e.setPadding(dp(14),0,dp(14),0);e.setBackground(shape("#FFFFFF",16));e.setText(String.valueOf(prefs.getInt(key,def)));LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(48));ep.topMargin=dp(4);parent.addView(e,ep);return e;
    }
    private int valid(EditText e){int v=Integer.parseInt(e.getText().toString().trim());if(v<1||v>600)throw new IllegalArgumentException();return v;}
}
