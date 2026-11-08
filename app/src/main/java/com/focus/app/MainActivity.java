package com.focus.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.*;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.Bitmap;
import java.util.Locale;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private TextView phaseText,timerText,sessionsText;
    private Button startButton;
    private TimerRing timerRing;
    private CountDownTimer ticker;
    private int phase=0;
    private boolean running=false;
    private ImageView wallpaper;
    private LinearLayout progressDots;
    private Typeface jakartaBold=Typeface.DEFAULT_BOLD,jakartaSemi=Typeface.DEFAULT,cairoMedium=Typeface.DEFAULT,cairoSemi=Typeface.DEFAULT_BOLD;
    private Typeface loadFont(String name,Typeface fallback){try{return Typeface.createFromAsset(getAssets(),name);}catch(Exception e){return fallback;}}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int c(String s){return Color.parseColor(s);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String s,int size,String color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(c(color));t.setGravity(Gravity.CENTER);return t;}
    private Button pill(String s,boolean primary){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(primary?14:13);
        b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));
        b.setTextColor(c(primary?"#FFFFFF":"#4746CB"));
        if(primary){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{c("#E65BAF"),c("#7654D8")});g.setCornerRadius(dp(28));b.setBackground(g);}
        else {GradientDrawable glass=shape(Color.argb(38,255,255,255),20);glass.setStroke(dp(1),Color.argb(77,139,92,246));b.setBackground(glass);}
        b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(5),0,dp(5),0);
        if(android.os.Build.VERSION.SDK_INT>=21)b.setElevation(dp(1));
        return b;
    }
    private void loadWallpaper(ImageView target){
        String custom=prefs.getString("custom_background_uri",null);
        Object source;
        if(custom!=null)source=Uri.parse(custom);
        else {
            String selected=prefs.getString("selected_theme","Morning Sun");
            String[] names={"Morning Sun","Brise Lames","BioGlow","Deep Forst","Mountain Village","Twilight Fuji","Golden Glow","Pink Clouds","Cozy Corner","Night City"};
            String[] files={"Morning Sun.jpg","Brise Lames.jpg","BioGlow.jpg","Deep Forst.png","Mountain Village.jpg","Twilight Fuji.png","Golden Glow.jpg","Pink Clouds.jpg","Cozy Corner.jpg","Night City.jpg"};
            String file="Morning Sun.jpg";for(int i=0;i<names.length;i++)if(names[i].equals(selected)){file=files[i];break;}
            source="file:///android_asset/themes/"+Uri.encode(file,"/");
        }
        Glide.with(this).asBitmap().load(source).centerCrop().into(new CustomTarget<Bitmap>(getResources().getDisplayMetrics().widthPixels,getResources().getDisplayMetrics().heightPixels){
            @Override public void onResourceReady(Bitmap bitmap,Transition<? super Bitmap> transition){
                target.setImageBitmap(bitmap);
                int color=topAverage(bitmap);
                getWindow().setStatusBarColor(color);
                getWindow().setNavigationBarColor(darken(color,.78f));
                boolean light=brightness(color)>.62;
                getWindow().getDecorView().setSystemUiVisibility(light?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);
            }
            @Override public void onLoadCleared(android.graphics.drawable.Drawable placeholder){}
        });
    }
    private int topAverage(Bitmap b){
        int h=Math.max(1,b.getHeight()/18),w=b.getWidth(),step=Math.max(1,w/80);
        long r=0,g=0,bl=0,n=0;
        for(int y=0;y<h;y+=Math.max(1,h/8))for(int x=0;x<w;x+=step){int px=b.getPixel(x,y);r+=Color.red(px);g+=Color.green(px);bl+=Color.blue(px);n++;}
        return Color.rgb((int)(r/n),(int)(g/n),(int)(bl/n));
    }
    private int darken(int color,float factor){return Color.rgb((int)(Color.red(color)*factor),(int)(Color.green(color)*factor),(int)(Color.blue(color)*factor));}
    private double brightness(int color){return (0.299*Color.red(color)+0.587*Color.green(color)+0.114*Color.blue(color))/255.0;}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);prefs=getSharedPreferences("focus_prefs",MODE_PRIVATE);
        jakartaBold=loadFont("PlusJakartaSans-Bold.ttf",Typeface.DEFAULT_BOLD);jakartaSemi=loadFont("PlusJakartaSans-SemiBold.ttf",Typeface.DEFAULT);cairoMedium=loadFont("Cairo-Medium.ttf",Typeface.DEFAULT);cairoSemi=loadFont("Cairo-SemiBold.ttf",Typeface.DEFAULT_BOLD);
        jakartaBold=loadFont("PlusJakartaSans-Bold.ttf",Typeface.DEFAULT_BOLD);jakartaSemi=loadFont("PlusJakartaSans-SemiBold.ttf",Typeface.DEFAULT);cairoMedium=loadFont("Cairo-Medium.ttf",Typeface.DEFAULT);cairoSemi=loadFont("Cairo-SemiBold.ttf",Typeface.DEFAULT_BOLD);
        FrameLayout root=new FrameLayout(this);
        wallpaper=new ImageView(this);wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));loadWallpaper(wallpaper);
        View tint=new View(this);tint.setBackgroundColor(Color.argb(24,255,255,255));root.addView(tint,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(18),dp(5),dp(18),dp(9));
        root.addView(page,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ImageView settings=new ImageView(this);settings.setImageResource(R.drawable.ic_settings);settings.setPadding(dp(10),dp(10),dp(10),dp(10));settings.setBackground(shape(Color.argb(145,255,255,255),50));
        header.addView(settings,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER);brand.setOrientation(LinearLayout.HORIZONTAL);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_focus_logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brand.addView(logo,new LinearLayout.LayoutParams(dp(31),dp(31)));
        TextView brandText=label("Focus",24,"#4746CB");brandText.setTypeface(jakartaSemi);brand.addView(brandText);
        header.addView(brand,new LinearLayout.LayoutParams(0,dp(44),1));
        View rightSpace=new View(this);header.addView(rightSpace,new LinearLayout.LayoutParams(dp(42),dp(42)));page.addView(header);
        settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));

        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(14),dp(12),dp(14),dp(10));panel.setBackground(shape(Color.argb(46,255,255,255),20));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,0,1);pp.topMargin=dp(8);page.addView(panel,pp);
        LinearLayout phaseBadge=new LinearLayout(this);phaseBadge.setGravity(Gravity.CENTER);phaseBadge.setOrientation(LinearLayout.HORIZONTAL);
        phaseBadge.setPadding(dp(13),0,dp(13),0);phaseBadge.setBackground(shape(Color.argb(46,255,255,255),20));
        phaseText=label("جلسة مذاكرة",14,"#4746CB");phaseText.setTypeface(cairoMedium);
        ImageView phaseIcon=new ImageView(this);phaseIcon.setId(android.view.View.generateViewId());phaseIcon.setColorFilter(c("#4746CB"));
        phaseBadge.addView(phaseIcon,new LinearLayout.LayoutParams(dp(19),dp(19)));
        phaseBadge.addView(phaseText,new LinearLayout.LayoutParams(-2,dp(34)));
        panel.addView(phaseBadge,new LinearLayout.LayoutParams(-2,dp(34)));
        phaseText.setTag(phaseIcon);

        FrameLayout ringFrame=new FrameLayout(this);
        int ringSize=Math.min(dp(184),(int)(getResources().getDisplayMetrics().widthPixels*.52f));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ringSize,ringSize);rp.topMargin=dp(6);rp.bottomMargin=dp(5);panel.addView(ringFrame,rp);
        timerRing=new TimerRing(this);ringFrame.addView(timerRing,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout center=new LinearLayout(this);center.setOrientation(LinearLayout.VERTICAL);center.setGravity(Gravity.CENTER);
        timerText=label("25:00",40,"#4746CB");timerText.setTypeface(jakartaBold);center.addView(timerText);
        ringFrame.addView(center,new FrameLayout.LayoutParams(-1,-1));

        startButton=pill("ابدأ",true);startButton.setTypeface(cairoSemi);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(44));sp.topMargin=dp(1);panel.addView(startButton,sp);
        startButton.setOnClickListener(v->toggleTimer());
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setOrientation(LinearLayout.HORIZONTAL);controls.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] names={"+ وقت","تخطّي","إنهاء"};
        for(String n:names){
            Button x=pill(n,false);x.setTypeface(cairoMedium);LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(0,dp(40),1);xp.setMargins(dp(3),dp(5),dp(3),dp(2));controls.addView(x,xp);
            if(n.equals("تخطّي"))x.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_skip,0,0,0);
            if(n.equals("إنهاء"))x.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_finish,0,0,0);
            x.setOnClickListener(v->{if(n.equals("+ وقت"))addFiveMinutes();else if(n.equals("تخطّي"))nextPhase();else finishPeriod();});
        }
        panel.addView(controls,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(10),dp(3),dp(10),dp(3));bottom.setBackground(shape(Color.argb(38,255,255,255),20));
        LinearLayout.LayoutParams botp=new LinearLayout.LayoutParams(-1,dp(48));botp.topMargin=dp(5);page.addView(bottom,botp);
        TextView completed=label("جلسات المذاكرة المكتملة",12,"#4746CB");completed.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);completed.setTypeface(cairoMedium);bottom.addView(completed,new LinearLayout.LayoutParams(-1,dp(19)));
        progressDots=new LinearLayout(this);progressDots.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);progressDots.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);bottom.addView(progressDots,new LinearLayout.LayoutParams(-1,dp(21)));renderProgressDots();
        setContentView(root);sync();
    }
    private int duration(){return prefs.getInt(phase==0?"study_minutes":phase==1?"short_minutes":"long_minutes",phase==0?25:phase==1?5:15);}
    private long sessionDuration(){return prefs.getLong("session_duration_ms",duration()*60000L);}
    private void updatePhaseBadge(){
        phaseText.setText(phase==0?"جلسة مذاكرة":phase==1?"استراحة قصيرة":"استراحة طويلة");
        ImageView icon=(ImageView)phaseText.getTag();
        icon.setImageResource(phase==0?R.drawable.ic_book_open:phase==1?R.drawable.ic_coffee:R.drawable.ic_bed);
        icon.setColorFilter(c("#4746CB"));
    }
    private void toggleTimer(){
        if(running){
            long left=Math.max(0,prefs.getLong("end_at",System.currentTimeMillis())-System.currentTimeMillis());
            prefs.edit().putBoolean("running",false).putLong("remaining_ms",left).apply();stopService(new Intent(this,TimerService.class));
            running=false;if(ticker!=null)ticker.cancel();startButton.setText("استئناف");startButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0);updateRing(left);return;
        }
        long total=sessionDuration();long left=prefs.getLong("remaining_ms",total);
        if(left<=0||left>total){total=duration()*60000L;left=total;prefs.edit().putLong("session_duration_ms",total).apply();}
        long end=System.currentTimeMillis()+left;
        prefs.edit().putLong("end_at",end).putLong("remaining_ms",left).putBoolean("running",true).putInt("phase",phase).apply();
        running=true;Intent i=new Intent(this,TimerService.class);
        if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
        startButton.setText("إيقاف مؤقت");startButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);startTicker();
    }
    private void addFiveMinutes(){
        long now=System.currentTimeMillis(),left=running?Math.max(0,prefs.getLong("end_at",now)-now):prefs.getLong("remaining_ms",duration()*60000L);
        long total=sessionDuration()+300000L,end=now+left+300000L;
        prefs.edit().putLong("session_duration_ms",total).putLong("end_at",end).putLong("remaining_ms",left+300000L).apply();
        if(running){Intent i=new Intent(this,TimerService.class);i.setAction(TimerService.ACTION_REFRESH);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);startTicker();}
        else updateRing(left+300000L);
    }
    private void nextPhase(){
        stopCurrent();phase=(phase+1)%3;long d=durationFor(phase);
        prefs.edit().putInt("phase",phase).putLong("remaining_ms",d).putLong("session_duration_ms",d).apply();sync();
    }
    private long durationFor(int p){return prefs.getInt(p==0?"study_minutes":p==1?"short_minutes":"long_minutes",p==0?25:p==1?5:15)*60000L;}
    private void finishPeriod(){stopCurrent();phase=0;long d=durationFor(0);prefs.edit().putInt("phase",0).putLong("remaining_ms",d).putLong("session_duration_ms",d).apply();sync();}
    private void stopCurrent(){if(running){prefs.edit().putBoolean("running",false).apply();stopService(new Intent(this,TimerService.class));running=false;}if(ticker!=null)ticker.cancel();}
    private void sync(){
        if(phaseText==null)return;phase=prefs.getInt("phase",0);running=prefs.getBoolean("running",false);updatePhaseBadge();
        
        long d=sessionDuration(),left=running?Math.max(0,prefs.getLong("end_at",System.currentTimeMillis())-System.currentTimeMillis()):prefs.getLong("remaining_ms",d);
        if(!running&&left<=0){d=durationFor(phase);left=d;prefs.edit().putLong("session_duration_ms",d).putLong("remaining_ms",d).apply();}
        render(left);renderProgressDots();startButton.setText(running?"إيقاف مؤقت":"ابدأ");
        startButton.setCompoundDrawablesRelativeWithIntrinsicBounds(running?R.drawable.ic_pause:0,0,0,0);
        updateRing(left);if(running)startTicker();
    }
    private void render(long ms){long s=Math.max(0,(ms+999)/1000);timerText.setText(String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60));}
    private void updateRing(long left){if(timerRing!=null){float f=sessionDuration()<=0?1f:Math.max(0f,Math.min(1f,(float)left/sessionDuration()));timerRing.setFraction(f);}}
    private void startTicker(){
        if(ticker!=null)ticker.cancel();long left=Math.max(0,prefs.getLong("end_at",System.currentTimeMillis())-System.currentTimeMillis());
        ticker=new CountDownTimer(left,250){public void onTick(long m){render(m);updateRing(m);}public void onFinish(){sync();}}.start();
    }
    private void renderProgressDots(){
        if(progressDots==null)return;progressDots.removeAllViews();
        int total=Math.max(1,Math.min(20,prefs.getInt("long_after",4)));
        int done=prefs.getInt("completed_sessions",0)%total;
        for(int i=0;i<total;i++){View dot=new View(this);dot.setBackground(shape(i<done?Color.argb(204,229,150,209):Color.argb(102,229,150,209),20));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(7),dp(7));p.setMargins(dp(3),0,dp(3),0);progressDots.addView(dot,p);}
    }
    @Override protected void onResume(){super.onResume();if(prefs!=null&&phaseText!=null){loadWallpaper(wallpaper);sync();}}
    @Override protected void onDestroy(){if(ticker!=null)ticker.cancel();super.onDestroy();}
    private class TimerRing extends View{
        Paint p=new Paint(3);float fraction=1f;
        TimerRing(android.content.Context c){super(c);}
        void setFraction(float f){fraction=f;invalidate();}
        protected void onDraw(Canvas canvas){
            super.onDraw(canvas);float w=getWidth(),h=getHeight(),r=Math.min(w,h)*.43f;
            p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(Color.argb(25,226,133,159));canvas.drawCircle(w/2,h/2,r,p);
            p.setShader(new SweepGradient(w/2,h/2,new int[]{Color.parseColor("#4746CB"),Color.parseColor("#766BE0"),Color.parseColor("#E596D1"),Color.parseColor("#4746CB")},new float[]{0f,.45f,.88f,1f}));
            canvas.save();canvas.rotate(-90,w/2,h/2);canvas.drawArc(w/2-r,h/2-r,w/2+r,h/2+r,0,-360*fraction,false,p);canvas.restore();p.setShader(null);
        }
    }
}
