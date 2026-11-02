package com.focus.app;

import android.app.Activity;
import android.media.RingtoneManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.content.res.ColorStateList;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private EditText study, shortBreak, longBreak, cycle;
    private TextView soundValue, selectedTheme;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int c(String s){return Color.parseColor(s);}
    private GradientDrawable panelShape(){
        GradientDrawable d=new GradientDrawable();
        d.setColor(Color.argb(145,54,66,112));
        d.setCornerRadius(dp(25));
        d.setStroke(dp(1),Color.argb(105,190,174,255));
        return d;
    }
    private GradientDrawable pill(String color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(c(color));d.setCornerRadius(dp(radius));return d;
    }
    private TextView text(String value,int size,String color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(c(color));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("focus_prefs",MODE_PRIVATE);
        getWindow().setStatusBarColor(c("#101B3B"));
        getWindow().setNavigationBarColor(c("#101B3B"));
        getWindow().getDecorView().setSystemUiVisibility(0);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page=vertical();
        page.setPadding(dp(20),dp(14),dp(20),dp(28));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{c("#101B3B"),c("#202B55"),c("#594B78"),c("#172A4A")});
        page.setBackground(bg);

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView moon=text("☾",32,"#D8C7FF",false);moon.setGravity(Gravity.CENTER);moon.setBackground(pill("#27365D",18));
        header.addView(moon,lp(62,54));
        TextView title=text("الإعدادات",25,"#FFFFFF",true);title.setGravity(Gravity.CENTER);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(56),1));
        TextView save=text("▣  حفظ",17,"#FFFFFF",true);save.setGravity(Gravity.CENTER);save.setBackground(pill("#6550D8",18));
        header.addView(save,lp(82,54));
        TextView back=text("‹‹",27,"#FFFFFF",true);back.setGravity(Gravity.CENTER);back.setBackground(pill("#27365D",18));
        LinearLayout.LayoutParams backp=lp(56,54);backp.leftMargin=dp(8);header.addView(back,backp);
        page.addView(header);
        back.setOnClickListener(v->finish());
        save.setOnClickListener(v->saveSettings());

        LinearLayout durations=card(page);
        sectionTitle(durations,"◷   مدة الفترات");
        LinearLayout row1=new LinearLayout(this);row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(fieldColumn("مدة المذاكرة  ◧","study_minutes",25),new LinearLayout.LayoutParams(0,-2,1));
        row1.addView(fieldColumn("مدة الاستراحة القصيرة  ☕","short_minutes",5),new LinearLayout.LayoutParams(0,-2,1));
        durations.addView(row1);
        LinearLayout row2=new LinearLayout(this);row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(fieldColumn("مدة الاستراحة الطويلة  ▰","long_minutes",15),new LinearLayout.LayoutParams(0,-2,1));
        row2.addView(fieldColumn("عدد جلسات المذاكرة قبل الاستراحة الطويلة  ⟳","long_after",4),new LinearLayout.LayoutParams(0,-2,1));
        durations.addView(row2);

        LinearLayout auto=card(page);
        sectionTitle(auto,"⚡   الانتقال التلقائي");
        addSwitch(auto,"من المذاكرة إلى الاستراحة القصيرة","auto_short");
        addSwitch(auto,"من الاستراحة القصيرة إلى المذاكرة","auto_study");
        addSwitch(auto,"من الاستراحة الطويلة إلى المذاكرة","auto_long");

        LinearLayout alert=card(page);
        sectionTitle(alert,"♧   التنبيه");
        soundValue=text(prefs.getString("alert_sound_name","اختر نغمة من الجهاز"),16,"#F2F0FF",false);
        soundValue.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);soundValue.setPadding(dp(16),0,dp(16),0);soundValue.setText("‹   "+soundValue.getText());
        soundValue.setBackground(pill("#39466F",30));
        alert.addView(soundValue,lp(-1,52));
        soundValue.setOnClickListener(v->pickRingtone());
        TextView addSound=text("＋   إضافة صوت من الجهاز",17,"#FFFFFF",true);addSound.setGravity(Gravity.CENTER);
        addSound.setBackground(pill("#6250D2",35));LinearLayout.LayoutParams asp=lp(-1,54);asp.topMargin=dp(12);alert.addView(addSound,asp);
        addSound.setOnClickListener(v->pickAudio());

        LinearLayout theme=card(page);
        sectionTitle(theme,"▧   السمة");
        TextView addImage=text("＋   صورة من الجهاز",15,"#FFFFFF",true);addImage.setGravity(Gravity.CENTER);
        addImage.setBackground(pill("#6250D2",28));LinearLayout.LayoutParams aip=lp(150,42);aip.gravity=Gravity.RIGHT;theme.addView(addImage,aip);
        addImage.setOnClickListener(v->pickImage());
        LinearLayout themes1=new LinearLayout(this);themes1.setOrientation(LinearLayout.HORIZONTAL);
        themes1.addView(themeTile("Golden Glow",new int[]{c("#F7C78E"),c("#415B83")}),new LinearLayout.LayoutParams(0,dp(92),1));
        themes1.addView(themeTile("Pink Clouds",new int[]{c("#F5A6C6"),c("#66568C")}),new LinearLayout.LayoutParams(0,dp(92),1));
        LinearLayout themes2=new LinearLayout(this);themes2.setOrientation(LinearLayout.HORIZONTAL);
        themes2.addView(themeTile("Cozy Corner",new int[]{c("#B88C70"),c("#4B3B43")}),new LinearLayout.LayoutParams(0,dp(92),1));
        themes2.addView(themeTile("Night City",new int[]{c("#5676A9"),c("#101B3B")}),new LinearLayout.LayoutParams(0,dp(92),1));
        LinearLayout.LayoutParams t1=lp(-1,92);t1.topMargin=dp(10);theme.addView(themes1,t1);
        LinearLayout.LayoutParams t2=lp(-1,92);t2.topMargin=dp(9);theme.addView(themes2,t2);
        TextView more=text("إظهار المزيد  ﹀",16,"#FFFFFF",true);more.setGravity(Gravity.CENTER);
        more.setBackground(pill("#6250D2",30));LinearLayout.LayoutParams mp=lp(-1,48);mp.topMargin=dp(12);theme.addView(more,mp);

        scroll.addView(page);setContentView(scroll);
    }

    private LinearLayout card(LinearLayout parent){
        LinearLayout box=vertical();box.setPadding(dp(15),dp(12),dp(15),dp(15));box.setBackground(panelShape());
        LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(14);parent.addView(box,p);return box;
    }
    private void sectionTitle(LinearLayout parent,String title){
        TextView t=text(title,19,"#FFFFFF",true);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        parent.addView(t,lp(-1,42));
    }
    private LinearLayout fieldColumn(String label,String key,int def){
        LinearLayout col=vertical();col.setPadding(dp(3),0,dp(3),0);
        TextView t=text(label,14,"#F1EFFF",false);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        col.addView(t,lp(-1,36));
        EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(19);e.setTextColor(Color.WHITE);
        e.setGravity(Gravity.CENTER);e.setInputType(2);e.setPadding(dp(8),0,dp(8),0);
        e.setBackground(pill("#39466F",16));e.setText(String.valueOf(prefs.getInt(key,def)));
        LinearLayout.LayoutParams ep=lp(-1,48);ep.topMargin=dp(3);col.addView(e,ep);
        if(key.equals("study_minutes"))study=e;
        if(key.equals("short_minutes"))shortBreak=e;
        if(key.equals("long_minutes"))longBreak=e;
        if(key.equals("long_after"))cycle=e;
        return col;
    }
    private void addSwitch(LinearLayout parent,String label,String key){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t=text(label,15,"#F5F2FF",false);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        Switch sw=new Switch(this);sw.setButtonTintList(ColorStateList.valueOf(c("#9B8BFF")));
        sw.setChecked(prefs.getBoolean(key,true));
        row.addView(sw,lp(64,48));
        row.addView(t,new LinearLayout.LayoutParams(0,dp(48),1));
        sw.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean(key,checked).apply());
        parent.addView(row,lp(-1,50));
    }
    private TextView themeTile(String name,int[] colors){
        TextView tile=text(name,14,"#FFFFFF",true);tile.setGravity(Gravity.BOTTOM|Gravity.CENTER);
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,colors);d.setCornerRadius(dp(15));tile.setBackground(d);
        LinearLayout.LayoutParams p=lp(-1,92);p.setMargins(dp(3),0,dp(3),0);
        tile.setOnClickListener(v->{prefs.edit().putString("selected_theme",name).apply();Toast.makeText(this,"تم اختيار "+name,Toast.LENGTH_SHORT).show();});
        return tile;
    }
    private void pickRingtone(){
        Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"اختر نغمة التنبيه");
        startActivityForResult(i,201);
    }
    private void pickAudio(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.addCategory(Intent.CATEGORY_OPENABLE);
        try{startActivityForResult(i,202);}catch(Exception e){Toast.makeText(this,"تعذر فتح ملفات الصوت",Toast.LENGTH_SHORT).show();}
    }
    private void pickImage(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);
        try{startActivityForResult(i,203);}catch(Exception e){Toast.makeText(this,"تعذر فتح الصور",Toast.LENGTH_SHORT).show();}
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null)return;
        Uri uri=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if(requestCode==201){
            if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            prefs.edit().putString("alert_sound_uri",uri.toString()).putString("alert_sound_name","نغمة من الجهاز").apply();
            soundValue.setText("نغمة من الجهاز");
        }else if(requestCode==202||requestCode==203){
            uri=data.getData();if(uri!=null){
                try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()& (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}
                prefs.edit().putString(requestCode==202?"custom_sound_uri":"custom_background_uri",uri.toString()).apply();
                if(requestCode==202){prefs.edit().putString("alert_sound_name","صوت مخصص من الجهاز").apply();soundValue.setText("صوت مخصص من الجهاز");}
                else Toast.makeText(this,"تم اختيار الصورة",Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void saveSettings(){
        try{
            int a=valid(study),b=valid(shortBreak),d=valid(longBreak),e=valid(cycle);
            prefs.edit().putInt("study_minutes",a).putInt("short_minutes",b).putInt("long_minutes",d).putInt("long_after",e).apply();
            Toast.makeText(this,"تم حفظ الإعدادات",Toast.LENGTH_SHORT).show();finish();
        }catch(Exception ex){Toast.makeText(this,"أدخل أرقامًا صحيحة من 1 إلى 600",Toast.LENGTH_SHORT).show();}
    }
    private int valid(EditText e){int v=Integer.parseInt(e.getText().toString().trim());if(v<1||v>600)throw new IllegalArgumentException();return v;}
}
