package com.focus.app;

import androidx.appcompat.app.AppCompatDelegate;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
    static { AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); }
    private SharedPreferences prefs;
    private TextView phaseText,timerText,sessionsText;
    private Button startButton;
    private CountDownTimer ticker;
    private int phase=0;
    private boolean running=false;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int c(String s){return Color.parseColor(s);}
    private GradientDrawable shape(String color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(c(color)); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView label(String s,int size,String color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(c(color));t.setGravity(Gravity.CENTER);return t;
    }
    private Button pill(String s,boolean primary){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(17);
        b.setTextColor(c(primary?"#FFFFFF":"#35368C"));
        b.setBackground(shape(primary?"#C34FC0":"#F9F2FC",40));
        return b;
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("focus_prefs",MODE_PRIVATE);
        getWindow().setStatusBarColor(c("#F8D9E0"));getWindow().setNavigationBarColor(c("#8CA8B7"));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{c("#F8D9E0"),c("#E7D6E8"),c("#91AEBE")});page.setBackground(bg);
        page.setPadding(dp(24),dp(12),dp(24),dp(18));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setOrientation(LinearLayout.HORIZONTAL);
        Button gear=new Button(this);gear.setText("⚙");gear.setTextSize(25);gear.setTextColor(c("#984BC0"));gear.setBackground(shape("#FFF7FB",60));
        header.addView(gear,new LinearLayout.LayoutParams(dp(58),dp(58)));
        TextView brand=label("✿ Focus",29,"#30358F");brand.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(58),1);header.addView(brand,bp);
        View spacer=new View(this);header.addView(spacer,new LinearLayout.LayoutParams(dp(58),dp(58)));
        page.addView(header);
        gear.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));

        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(20),dp(20),dp(20),dp(20));panel.setBackground(shape("#F7F1F7",34));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,0,1);pp.topMargin=dp(26);page.addView(panel,pp);
        phaseText=label("جلسة مذاكرة",19,"#34378F");phaseText.setTypeface(null,Typeface.BOLD);
        phaseText.setBackground(shape("#F0E2F5",30));panel.addView(phaseText,new LinearLayout.LayoutParams(-1,dp(52)));

        FrameLayout ringFrame=new FrameLayout(this);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(270),dp(270));rp.topMargin=dp(24);rp.bottomMargin=dp(20);panel.addView(ringFrame,rp);
        TimerRing ring=new TimerRing(this);ringFrame.addView(ring,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout center=new LinearLayout(this);center.setOrientation(LinearLayout.VERTICAL);center.setGravity(Gravity.CENTER);
        timerText=label("25:00",52,"#34378F");timerText.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        center.addView(timerText);
        TextView total=label("من 25:00",15,"#7779B4");center.addView(total);
        ringFrame.addView(center,new FrameLayout.LayoutParams(-1,-1));

        startButton=pill("ابدأ",true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(268),dp(62));sp.topMargin=dp(4);panel.addView(startButton,sp);
        startButton.setOnClickListener(v->toggleTimer());
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setOrientation(LinearLayout.HORIZONTAL);
        String[] names={"إنهاء","تخطّي","+ وقت"};for(String n:names){Button x=pill(n,false);LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(0,dp(62),1);xp.setMargins(dp(3),dp(18),dp(3),0);controls.addView(x,xp);x.setOnClickListener(v->{if(n.equals("+ وقت"))addFiveMinutes();else if(n.equals("تخطّي"))nextPhase();else finishPeriod();});}
        panel.addView(controls);

        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setGravity(Gravity.CENTER);bottom.setBackground(shape("#F7F1F7",28));
        LinearLayout.LayoutParams botp=new LinearLayout.LayoutParams(-1,dp(118));botp.topMargin=dp(22);page.addView(bottom,botp);
        sessionsText=label("0 / 4 جلسات مكتملة في الدورة",21,"#34378F");sessionsText.setTypeface(null,Typeface.BOLD);bottom.addView(sessionsText);
        TextView dots=label("●   ●   ●   ●",22,"#D9BDE6");bottom.addView(dots);
        setContentView(page);sync();
    }
    private int duration(){return prefs.getInt(phase==0?"study_minutes":phase==1?"short_minutes":"long_minutes",phase==0?25:phase==1?5:15);}
    private void toggleTimer(){
        if(running){long left=Math.max(0,prefs.getLong("end_at",System.currentTimeMillis())-System.currentTimeMillis());prefs.edit().putBoolean("running",false).putLong("remaining_ms",left).apply();stopService(new Intent(this,TimerService.class));running=false;if(ticker!=null)ticker.cancel();startButton.setText("استئناف");return;}
        long left=prefs.getLong("remaining_ms",duration()*60000L);if(left<=0)left=duration()*60000L;
        long end=System.currentTimeMillis()+left;prefs.edit().putLong("end_at",end).putLong("remaining_ms",left).putBoolean("running",true).putInt("phase",phase).apply();running=true;
        Intent i=new Intent(this,TimerService.class);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);startButton.setText("إيقاف مؤقت");startTicker();
    }
    private void addFiveMinutes(){long now=System.currentTimeMillis();long end=(running?prefs.getLong("end_at",now):now+prefs.getLong("remaining_ms",duration()*60000L))+300000L;prefs.edit().putLong("end_at",end).putLong("remaining_ms",end-now).apply();if(running){Intent i=new Intent(this,TimerService.class);i.setAction(TimerService.ACTION_REFRESH);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);startTicker();}else render(end-now);}
    private void nextPhase(){if(running){prefs.edit().putBoolean("running",false).apply();stopService(new Intent(this,TimerService.class));running=false;if(ticker!=null)ticker.cancel();}phase=(phase+1)%3;prefs.edit().putInt("phase",phase).putLong("remaining_ms",duration()*60000L).apply();sync();}
    private void finishPeriod(){if(running){prefs.edit().putBoolean("running",false).putLong("remaining_ms",0).apply();stopService(new Intent(this,TimerService.class));running=false;if(ticker!=null)ticker.cancel();}phase=0;prefs.edit().putInt("phase",0).putLong("remaining_ms",duration()*60000L).apply();sync();}
    private void sync(){phase=prefs.getInt("phase",0);running=prefs.getBoolean("running",false);phaseText.setText(phase==0?"جلسة مذاكرة":phase==1?"استراحة قصيرة":"استراحة طويلة");int done=prefs.getInt("completed_sessions",0);sessionsText.setText((done%4)+" / "+prefs.getInt("long_after",4)+" جلسات مكتملة في الدورة");long left=running?Math.max(0,prefs.getLong("end_at",System.currentTimeMillis())-System.currentTimeMillis()):prefs.getLong("remaining_ms",duration()*60000L);if(!running&&left<=0)left=duration()*60000L;render(left);startButton.setText(running?"إيقاف مؤقت":"ابدأ");if(running)startTicker();}
    private void render(long ms){long s=Math.max(0,(ms+999)/1000);timerText.setText(String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60));}
    private void startTicker(){if(ticker!=null)ticker.cancel();long left=Math.max(0,prefs.getLong("end_at",System.currentTimeMillis())-System.currentTimeMillis());ticker=new CountDownTimer(left,500){public void onTick(long m){render(m);}public void onFinish(){sync();}}.start();}
    @Override protected void onResume(){super.onResume();if(prefs!=null)sync();}
    @Override protected void onDestroy(){if(ticker!=null)ticker.cancel();super.onDestroy();}
    private class TimerRing extends View{
        Paint p=new Paint(3);public TimerRing(android.content.Context c){super(c);}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight(),r=Math.min(w,h)*.43f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(11));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.parseColor("#EAD9EF"));canvas.drawCircle(w/2,h/2,r,p);p.setShader(new LinearGradient(w*.2f,h*.2f,w*.8f,h*.8f,Color.parseColor("#D04FBA"),Color.parseColor("#6755C9"),Shader.TileMode.CLAMP));canvas.drawArc(w/2-r,h/2-r,w/2+r,h/2+r,-90,360,false,p);p.setShader(null);}
    }
}
