package com.focus.app;

import android.app.Activity;
import android.media.RingtoneManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.content.res.ColorStateList;
import com.bumptech.glide.Glide;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private EditText study,shortBreak,longBreak,cycle;
    private TextView soundValue;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int c(String s){return Color.parseColor(s);}
    private GradientDrawable panelShape(){
        GradientDrawable d=new GradientDrawable();d.setColor(Color.argb(168,35,48,86));d.setCornerRadius(dp(23));
        d.setStroke(dp(1),Color.argb(100,205,194,255));return d;
    }
    private GradientDrawable pill(String color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(c(color));d.setCornerRadius(dp(radius));return d;
    }
    private TextView text(String value,int size,String color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(c(color));
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",0));return t;
    }
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void loadThemeImage(String file,ImageView target){
        String assetUri="file:///android_asset/themes/"+Uri.encode(file,"/");
        Glide.with(this).load(Uri.parse(assetUri)).centerCrop().into(target);
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);prefs=getSharedPreferences("focus_prefs",MODE_PRIVATE);
        getWindow().setStatusBarColor(c("#101B3B"));getWindow().setNavigationBarColor(c("#101B3B"));
        FrameLayout root=new FrameLayout(this);
        ImageView wallpaper=new ImageView(this);wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadThemeImage("Brise Lames.jpg",wallpaper);
        root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(this);shade.setBackgroundColor(Color.argb(120,8,17,43));root.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout page=vertical();page.setPadding(dp(18),dp(12),dp(18),dp(24));scroll.addView(page);root.addView(scroll);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView settingsIcon=new ImageView(this);settingsIcon.setImageResource(R.drawable.ic_settings);settingsIcon.setPadding(dp(12),dp(12),dp(12),dp(12));settingsIcon.setBackground(pill("#FFFFFF",18));header.addView(settingsIcon,lp(48,48));
        TextView title=text("الإعدادات",23,"#FFFFFF",true);title.setGravity(Gravity.CENTER);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView save=text("حفظ",16,"#FFFFFF",true);save.setGravity(Gravity.CENTER);save.setBackground(pill("#7056DA",17));header.addView(save,lp(66,46));
        ImageView back=new ImageView(this);back.setImageResource(R.drawable.ic_back);back.setPadding(dp(10),dp(10),dp(10),dp(10));back.setBackground(pill("#35436B",17));
        LinearLayout.LayoutParams backp=lp(44,46);backp.leftMargin=dp(7);header.addView(back,backp);page.addView(header);
        back.setOnClickListener(v->finish());save.setOnClickListener(v->saveSettings());

        LinearLayout durations=card(page);sectionTitle(durations,"◷   مدة الفترات");
        LinearLayout row1=new LinearLayout(this);row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(fieldColumn("مدة المذاكرة","study_minutes",25),new LinearLayout.LayoutParams(0,-2,1));
        row1.addView(fieldColumn("الاستراحة القصيرة","short_minutes",5),new LinearLayout.LayoutParams(0,-2,1));durations.addView(row1);
        LinearLayout row2=new LinearLayout(this);row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(fieldColumn("الاستراحة الطويلة","long_minutes",15),new LinearLayout.LayoutParams(0,-2,1));
        row2.addView(fieldColumn("جلسات قبل الاستراحة الطويلة","long_after",4),new LinearLayout.LayoutParams(0,-2,1));durations.addView(row2);

        LinearLayout auto=card(page);sectionTitle(auto,"⚡   الانتقال التلقائي");
        addSwitch(auto,"من المذاكرة إلى الاستراحة القصيرة","auto_short");
        addSwitch(auto,"من الاستراحة القصيرة إلى المذاكرة","auto_study");
        addSwitch(auto,"من الاستراحة الطويلة إلى المذاكرة","auto_long");

        LinearLayout alert=card(page);sectionTitle(alert,"♧   التنبيه");
        soundValue=text(prefs.getString("alert_sound_name","اختر نغمة من الجهاز"),15,"#F4F1FF",false);
        soundValue.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);soundValue.setPadding(dp(14),0,dp(14),0);
        soundValue.setText("‹   "+soundValue.getText());soundValue.setBackground(pill("#39466F",25));
        alert.addView(soundValue,lp(-1,48));soundValue.setOnClickListener(v->pickRingtone());
        TextView addSound=text("＋   إضافة صوت من الجهاز",16,"#FFFFFF",true);addSound.setGravity(Gravity.CENTER);addSound.setBackground(pill("#6753D1",30));
        LinearLayout.LayoutParams asp=lp(-1,49);asp.topMargin=dp(10);alert.addView(addSound,asp);addSound.setOnClickListener(v->pickAudio());

        LinearLayout theme=card(page);sectionTitle(theme,"▧   السمة");
        TextView addImage=text("صورة من الجهاز",14,"#FFFFFF",true);addImage.setGravity(Gravity.CENTER);addImage.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_add_image),null,null,null);addImage.setBackground(pill("#6753D1",25));
        LinearLayout.LayoutParams aip=lp(145,40);aip.gravity=Gravity.RIGHT;theme.addView(addImage,aip);addImage.setOnClickListener(v->pickImage());
        LinearLayout tiles=new LinearLayout(this);tiles.setOrientation(LinearLayout.HORIZONTAL);
        tiles.addView(themeTile("Morning Sun","Morning Sun.jpg"),new LinearLayout.LayoutParams(0,dp(106),1));
        tiles.addView(themeTile("Brise Lames","Brise Lames.jpg"),new LinearLayout.LayoutParams(0,dp(106),1));
        LinearLayout.LayoutParams tp=lp(-1,106);tp.topMargin=dp(10);theme.addView(tiles,tp);
        setContentView(root);
    }
    private LinearLayout card(LinearLayout parent){
        LinearLayout box=vertical();box.setPadding(dp(13),dp(9),dp(13),dp(12));box.setBackground(panelShape());
        LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(12);parent.addView(box,p);return box;
    }
    private void sectionTitle(LinearLayout parent,String title){
        TextView t=text(title,17,"#FFFFFF",true);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        int icon=title.contains("مدة")?R.drawable.ic_clock:title.contains("الانتقال")?R.drawable.ic_transition:title.contains("التنبيه")?R.drawable.ic_bell:R.drawable.ic_image;
        t.setCompoundDrawablePadding(dp(9));t.setCompoundDrawablesWithIntrinsicBounds(icon,0,0,0);parent.addView(t,lp(-1,37));
    }
    private LinearLayout fieldColumn(String label,String key,int def){
        LinearLayout col=vertical();col.setPadding(dp(3),0,dp(3),0);
        TextView t=text(label,13,"#F1EFFF",false);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);col.addView(t,lp(-1,33));
        EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(18);e.setTextColor(Color.WHITE);e.setGravity(Gravity.CENTER);e.setInputType(2);
        e.setPadding(dp(6),0,dp(6),0);e.setBackground(pill("#39466F",14));e.setText(String.valueOf(prefs.getInt(key,def)));
        LinearLayout.LayoutParams ep=lp(-1,43);ep.topMargin=dp(2);col.addView(e,ep);
        if(key.equals("study_minutes"))study=e;if(key.equals("short_minutes"))shortBreak=e;if(key.equals("long_minutes"))longBreak=e;if(key.equals("long_after"))cycle=e;
        return col;
    }
    private void addSwitch(LinearLayout parent,String label,String key){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t=text(label,14,"#F5F2FF",false);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        Switch sw=new Switch(this);sw.setButtonTintList(ColorStateList.valueOf(c("#A99AFF")));sw.setChecked(prefs.getBoolean(key,true));
        row.addView(sw,lp(58,45));row.addView(t,new LinearLayout.LayoutParams(0,dp(45),1));
        sw.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean(key,checked).apply());parent.addView(row,lp(-1,46));
    }
    private FrameLayout themeTile(String name,String file){
        FrameLayout tile=new FrameLayout(this);tile.setBackground(pill("#34436B",15));tile.setClipToOutline(true);
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);loadThemeImage(file,image);
        tile.addView(image,new FrameLayout.LayoutParams(-1,-1));
        View scrim=new View(this);scrim.setBackgroundColor(Color.argb(80,0,0,0));tile.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
        TextView caption=text(name,14,"#FFFFFF",true);caption.setGravity(Gravity.CENTER);tile.addView(caption,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(106),1);p.setMargins(dp(3),0,dp(3),0);
        tile.setOnClickListener(v->{prefs.edit().putString("selected_theme",name).apply();Toast.makeText(this,"تم اختيار "+name,Toast.LENGTH_SHORT).show();});
        return tile;
    }
    private void pickRingtone(){
        Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"اختر نغمة التنبيه");startActivityForResult(i,201);
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
        super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null)return;
        Uri uri=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if(requestCode==201){if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            prefs.edit().putString("alert_sound_uri",uri.toString()).putString("alert_sound_name","نغمة من الجهاز").apply();soundValue.setText("نغمة من الجهاز");
        }else if(requestCode==202||requestCode==203){uri=data.getData();if(uri!=null){
            try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}
            prefs.edit().putString(requestCode==202?"custom_sound_uri":"custom_background_uri",uri.toString()).apply();
            if(requestCode==202){prefs.edit().putString("alert_sound_name","صوت مخصص من الجهاز").apply();soundValue.setText("صوت مخصص من الجهاز");}
            else Toast.makeText(this,"تم اختيار الصورة؛ ستظهر بعد العودة للشاشة الرئيسية",Toast.LENGTH_SHORT).show();
        }}
    }
    private void saveSettings(){
        try{int a=valid(study),b=valid(shortBreak),d=valid(longBreak),e=valid(cycle);
            int currentPhase=prefs.getInt("phase",0);
            boolean isRunning=prefs.getBoolean("running",false);
            long now=System.currentTimeMillis();
            long oldTotal=prefs.getLong("session_duration_ms",Math.max(1,prefs.getInt(currentPhase==0?"study_minutes":currentPhase==1?"short_minutes":"long_minutes",currentPhase==0?25:currentPhase==1?5:15))*60000L);
            long oldLeft=isRunning?Math.max(0,prefs.getLong("end_at",now)-now):prefs.getLong("remaining_ms",oldTotal);
            long elapsed=Math.max(0,oldTotal-oldLeft);
            long newMinutes=currentPhase==0?a:currentPhase==1?b:d;
            long newTotal=newMinutes*60000L;
            long newLeft=Math.max(0,newTotal-elapsed);
            SharedPreferences.Editor editor=prefs.edit()
                .putInt("study_minutes",a).putInt("short_minutes",b).putInt("long_minutes",d).putInt("long_after",e)
                .putLong("session_duration_ms",newTotal).putLong("remaining_ms",newLeft);
            if(isRunning)editor.putLong("end_at",now+newLeft);
            editor.apply();
            if(isRunning){Intent refresh=new Intent(this,TimerService.class);refresh.setAction(TimerService.ACTION_REFRESH);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(refresh);else startService(refresh);}
            Toast.makeText(this,"تم حفظ الإعدادات وتحديث الفترة الحالية",Toast.LENGTH_SHORT).show();finish();
        }catch(Exception ex){Toast.makeText(this,"أدخل أرقامًا صحيحة من 1 إلى 600",Toast.LENGTH_SHORT).show();}
    }
    private int valid(EditText e){int v=Integer.parseInt(e.getText().toString().trim());if(v<1||v>600)throw new IllegalArgumentException();return v;}
}
