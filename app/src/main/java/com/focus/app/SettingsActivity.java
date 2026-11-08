package com.focus.app;

import android.app.Activity;
import android.media.RingtoneManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private EditText study, shortBreak, longBreak, cycle;
    private TextView soundValue, moreThemes;
    private LinearLayout themeGrid;
    private boolean expanded=false;
    private final String[] themeNames={"Morning Sun","Brise Lames","BioGlow","Deep Forst","Mountain Village","Twilight Fuji"};
    private final String[] themeFiles={"Morning Sun.jpg","Brise Lames.jpg","BioGlow.jpg","Deep Forst.png","Mountain Village.jpg","Twilight Fuji.png"};
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int c(String s){return Color.parseColor(s);}
    private GradientDrawable rounded(int color,int radius,int stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        if(stroke>0)d.setStroke(dp(1),Color.argb(105,205,194,255));return d;
    }
    private GradientDrawable gradient(int a,int b,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{c(a==0?"#6655D7":"#6655D7"),c("#5145C7")});
        d.setCornerRadius(dp(radius));return d;
    }
    private TextView txt(String value,int size,String color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(c(color));
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));return t;
    }
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void loadImage(String file,ImageView target){
        Glide.with(this).load(Uri.parse("file:///android_asset/themes/"+Uri.encode(file,"/"))).centerCrop().into(target);
    }
    private void loadWallpaper(ImageView target){
        String custom=prefs.getString("custom_background_uri",null);
        if(custom!=null)Glide.with(this).load(Uri.parse(custom)).centerCrop().into(target);
        else{
            String selected=prefs.getString("selected_theme","Morning Sun");
            String file="Morning Sun.jpg";
            for(int i=0;i<themeNames.length;i++)if(themeNames[i].equals(selected))file=themeFiles[i];
            if(selected.equals("Cozy Corner"))file="Cozy Corner.jpg";
            if(selected.equals("Golden Glow"))file="Golden Glow.jpg";
            if(selected.equals("Pink Clouds"))file="Pink Clouds.jpg";
            if(selected.equals("Night City"))file="Night City.jpg";
            loadImage(file,target);
        }
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);prefs=getSharedPreferences("focus_prefs",MODE_PRIVATE);
        getWindow().setStatusBarColor(c("#14213F"));getWindow().setNavigationBarColor(c("#14213F"));
        FrameLayout root=new FrameLayout(this);
        ImageView wallpaper=new ImageView(this);wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);loadWallpaper(wallpaper);
        root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(this);shade.setBackgroundColor(Color.argb(112,8,17,43));root.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        LinearLayout page=vertical();page.setPadding(dp(18),dp(12),dp(18),dp(24));scroll.addView(page);
        root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header=horizontal();header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        TextView moon=txt("☾",30,"#C7B5FF",true);moon.setGravity(Gravity.CENTER);moon.setBackground(rounded(Color.argb(90,255,255,255),18,1));
        header.addView(moon,lp(58,48));
        TextView title=txt("الإعدادات",24,"#FFFFFF",true);title.setGravity(Gravity.CENTER);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView save=txt("حفظ",16,"#FFFFFF",true);save.setGravity(Gravity.CENTER);save.setBackground(rounded(c("#5146D8"),17,0));header.addView(save,lp(66,46));
        ImageView back=new ImageView(this);back.setImageResource(R.drawable.ic_back);back.setPadding(dp(10),dp(10),dp(10),dp(10));back.setBackground(rounded(Color.argb(95,255,255,255),17,1));
        LinearLayout.LayoutParams bp=lp(44,46);bp.leftMargin=dp(7);header.addView(back,bp);page.addView(header);
        save.setOnClickListener(v->saveSettings());back.setOnClickListener(v->finish());
        moon.setOnClickListener(v->Toast.makeText(this,"يتبع التطبيق مظهره الحالي",Toast.LENGTH_SHORT).show());

        LinearLayout durations=card(page);sectionTitle(durations,"مدة الفترات",R.drawable.ic_clock);
        LinearLayout r1=horizontal();
        r1.addView(fieldColumn("مدة المذاكرة","study_minutes",25,R.drawable.ic_book_open),new LinearLayout.LayoutParams(0,-2,1));
        r1.addView(fieldColumn("مدة الاستراحة القصيرة","short_minutes",5,R.drawable.ic_coffee),new LinearLayout.LayoutParams(0,-2,1));durations.addView(r1);
        LinearLayout r2=horizontal();
        r2.addView(fieldColumn("عدد جلسات المذاكرة قبل الاستراحة الطويلة","long_after",4,R.drawable.ic_sessions_cycle),new LinearLayout.LayoutParams(0,-2,1));
        r2.addView(fieldColumn("مدة الاستراحة الطويلة","long_minutes",15,R.drawable.ic_bed),new LinearLayout.LayoutParams(0,-2,1));durations.addView(r2);

        LinearLayout auto=card(page);sectionTitle(auto,"الانتقال التلقائي",R.drawable.ic_zap);
        addSwitch(auto,"من المذاكرة إلى الاستراحة القصيرة","auto_short");
        addSwitch(auto,"من الاستراحة القصيرة إلى المذاكرة","auto_study");
        addSwitch(auto,"من الاستراحة الطويلة إلى المذاكرة","auto_long");

        LinearLayout alert=card(page);sectionTitle(alert,"التنبيه",R.drawable.ic_bell);
        soundValue=txt(prefs.getString("alert_sound_name","اختر نغمة من الجهاز"),15,"#F5F2FF",false);
        soundValue.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);soundValue.setPadding(dp(14),0,dp(14),0);
        soundValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_chevron_left,0,0,0);
        soundValue.setBackground(rounded(Color.argb(112,255,255,255),25,1));alert.addView(soundValue,lp(-1,48));
        soundValue.setOnClickListener(v->pickRingtone());
        TextView addSound=txt("＋   إضافة صوت من الجهاز",16,"#FFFFFF",true);addSound.setGravity(Gravity.CENTER);addSound.setBackground(rounded(c("#6252D0"),28,0));
        LinearLayout.LayoutParams asp=lp(-1,49);asp.topMargin=dp(10);alert.addView(addSound,asp);addSound.setOnClickListener(v->pickAudio());

        LinearLayout theme=card(page);sectionTitle(theme,"السمة",R.drawable.ic_image);
        TextView addImage=txt("＋  صورة من الجهاز",14,"#FFFFFF",true);addImage.setGravity(Gravity.CENTER);addImage.setBackground(rounded(c("#6252D0"),24,0));
        LinearLayout.LayoutParams aip=lp(145,40);aip.gravity=Gravity.LEFT;theme.addView(addImage,aip);addImage.setOnClickListener(v->pickImage());
        themeGrid=vertical();LinearLayout.LayoutParams gp=lp(-1,-2);gp.topMargin=dp(8);theme.addView(themeGrid,gp);
        moreThemes=txt("إظهار المزيد   ﹀",16,"#FFFFFF",true);moreThemes.setGravity(Gravity.CENTER);moreThemes.setBackground(rounded(c("#6252D0"),26,0));
        LinearLayout.LayoutParams mp=lp(-1,45);mp.topMargin=dp(10);theme.addView(moreThemes,mp);
        moreThemes.setOnClickListener(v->{expanded=!expanded;renderThemes();moreThemes.setText(expanded?"إظهار أقل   ﹀":"إظهار المزيد   ﹀");});
        renderThemes();
        setContentView(root);
        study.clearFocus();shortBreak.clearFocus();longBreak.clearFocus();cycle.clearFocus();page.setFocusableInTouchMode(true);page.requestFocus();
    }
    private LinearLayout card(LinearLayout parent){
        LinearLayout box=vertical();box.setPadding(dp(13),dp(9),dp(13),dp(12));
        box.setBackground(rounded(Color.argb(46,255,255,255),20,1));
        LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(12);parent.addView(box,p);return box;
    }
    private void sectionTitle(LinearLayout parent,String title,int icon){
        LinearLayout row=horizontal();row.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        TextView t=txt(title,18,"#FFFFFF",true);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        row.addView(t);ImageView im=new ImageView(this);im.setImageResource(icon);im.setColorFilter(c("#A88BFF"));
        LinearLayout.LayoutParams ip=lp(27,27);ip.leftMargin=dp(8);row.addView(im,ip);
        parent.addView(row,lp(-1,38));
    }
    private LinearLayout fieldColumn(String label,String key,int def,int icon){
        LinearLayout col=vertical();col.setPadding(dp(3),dp(2),dp(3),dp(4));
        LinearLayout heading=horizontal();heading.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        TextView t=txt(label,13,"#F1EFFF",false);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        heading.addView(t,new LinearLayout.LayoutParams(0,dp(34),1));
        ImageView im=new ImageView(this);im.setImageResource(icon);im.setColorFilter(c("#B39AFF"));heading.addView(im,lp(23,23));col.addView(heading);
        EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(18);e.setTextColor(Color.WHITE);e.setGravity(Gravity.CENTER);e.setInputType(2);
        e.setPadding(dp(6),0,dp(6),0);e.setBackground(rounded(Color.argb(100,255,255,255),14,1));e.setText(String.valueOf(prefs.getInt(key,def)));
        col.addView(e,lp(-1,43));
        if(key.equals("study_minutes"))study=e;if(key.equals("short_minutes"))shortBreak=e;if(key.equals("long_minutes"))longBreak=e;if(key.equals("long_after"))cycle=e;
        return col;
    }
    private void addSwitch(LinearLayout parent,String label,String key){
        LinearLayout row=horizontal();TextView t=txt(label,14,"#F5F2FF",false);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        Switch sw=new Switch(this);sw.setButtonTintList(ColorStateList.valueOf(c("#A99AFF")));sw.setChecked(prefs.getBoolean(key,true));
        row.addView(sw,lp(58,45));row.addView(t,new LinearLayout.LayoutParams(0,dp(45),1));
        sw.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean(key,checked).apply());parent.addView(row,lp(-1,46));
    }
    private void renderThemes(){
        themeGrid.removeAllViews();int count=expanded?themeNames.length:4;
        for(int i=0;i<count;i+=2){
            LinearLayout row=horizontal();
            row.addView(themeTile(i),new LinearLayout.LayoutParams(0,dp(108),1));
            if(i+1<count)row.addView(themeTile(i+1),new LinearLayout.LayoutParams(0,dp(108),1));
            LinearLayout.LayoutParams rp=lp(-1,108);rp.topMargin=dp(5);themeGrid.addView(row,rp);
        }
    }
    private FrameLayout themeTile(int index){
        String name=themeNames[index],file=themeFiles[index];
        FrameLayout tile=new FrameLayout(this);tile.setBackground(rounded(Color.DKGRAY,15,1));tile.setClipToOutline(true);
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);loadImage(file,image);tile.addView(image,new FrameLayout.LayoutParams(-1,-1));
        TextView caption=txt(name,13,"#FFFFFF",true);caption.setGravity(Gravity.CENTER);caption.setShadowLayer(dp(3),0,dp(1),Color.BLACK);
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,dp(29),Gravity.BOTTOM);tile.addView(caption,cp);
        if(name.equals(prefs.getString("selected_theme","Morning Sun"))){View selected=new View(this);GradientDrawable border=rounded(Color.TRANSPARENT,15,1);border.setStroke(dp(2),c("#A991FF"));selected.setBackground(border);tile.addView(selected,new FrameLayout.LayoutParams(-1,-1));}
        tile.setOnClickListener(v->{prefs.edit().putString("selected_theme",name).remove("custom_background_uri").apply();renderThemes();sendBroadcast(new Intent("com.focus.app.THEME_CHANGED"));Toast.makeText(this,"تم اختيار "+name,Toast.LENGTH_SHORT).show();});
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(108),1);p.setMargins(dp(3),0,dp(3),0);
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
            else {sendBroadcast(new Intent("com.focus.app.THEME_CHANGED"));Toast.makeText(this,"تم تحديث الخلفية",Toast.LENGTH_SHORT).show();}
        }}
    }
    private void saveSettings(){
        try{
            int a=valid(study),b=valid(shortBreak),d=valid(longBreak),e=valid(cycle);
            int currentPhase=prefs.getInt("phase",0);boolean isRunning=prefs.getBoolean("running",false);long now=System.currentTimeMillis();
            long oldTotal=prefs.getLong("session_duration_ms",Math.max(1,prefs.getInt(currentPhase==0?"study_minutes":currentPhase==1?"short_minutes":"long_minutes",currentPhase==0?25:currentPhase==1?5:15))*60000L);
            long oldLeft=isRunning?Math.max(0,prefs.getLong("end_at",now)-now):prefs.getLong("remaining_ms",oldTotal);
            long elapsed=Math.max(0,oldTotal-oldLeft);long newTotal=(currentPhase==0?a:currentPhase==1?b:d)*60000L;long newLeft=Math.max(0,newTotal-elapsed);
            SharedPreferences.Editor editor=prefs.edit().putInt("study_minutes",a).putInt("short_minutes",b).putInt("long_minutes",d).putInt("long_after",e).putLong("session_duration_ms",newTotal).putLong("remaining_ms",newLeft);
            if(isRunning)editor.putLong("end_at",now+newLeft);editor.apply();
            if(isRunning){Intent refresh=new Intent(this,TimerService.class);refresh.setAction(TimerService.ACTION_REFRESH);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(refresh);else startService(refresh);}
            Toast.makeText(this,"تم حفظ الإعدادات وتحديث الفترة الحالية",Toast.LENGTH_SHORT).show();finish();
        }catch(Exception ex){Toast.makeText(this,"أدخل أرقامًا صحيحة من 1 إلى 600",Toast.LENGTH_SHORT).show();}
    }
    private int valid(EditText e){int v=Integer.parseInt(e.getText().toString().trim());if(v<1||v>600)throw new IllegalArgumentException();return v;}
}
