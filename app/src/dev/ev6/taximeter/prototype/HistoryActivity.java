package dev.ev6.taximeter.prototype;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local, read-only trip history; pages are loaded off the UI thread. */
public final class HistoryActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();private final Handler ui=new Handler(Looper.getMainLooper());
    private SplitViewport viewport;
    private LinearLayout list;private TextView status;private Button more;private long beforeId;private boolean loading,dead;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(36,18,36,18);page.setBackgroundColor(0xff16212a);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);TextView title=text("운행 기록",30,0xfff3f7f8);header.addView(title,new LinearLayout.LayoutParams(0,70,1));
        Button refresh=new Button(this);refresh.setText("새로고침");header.addView(refresh,new LinearLayout.LayoutParams(170,68));refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){reload();}});
        Button back=new Button(this);back.setText("돌아가기");header.addView(back,new LinearLayout.LayoutParams(170,68));back.setOnClickListener(new View.OnClickListener(){public void onClick(View v){finish();}});page.addView(header);
        status=text("기록을 불러오는 중",16,0xffaebfc5);page.addView(status,new LinearLayout.LayoutParams(-1,36));
        ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        more=new Button(this);more.setText("이전 기록 더 보기");more.setVisibility(View.GONE);more.setOnClickListener(new View.OnClickListener(){public void onClick(View v){load(false);}});page.addView(more,new LinearLayout.LayoutParams(-1,60));
        android.widget.FrameLayout outer=new android.widget.FrameLayout(this);outer.setBackgroundColor(0xff16212a);outer.addView(page,new android.widget.FrameLayout.LayoutParams(-1,-1));setContentView(outer);viewport=new SplitViewport(this,page);
    }
    @Override protected void onStart(){super.onStart();reload();}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private void reload(){if(loading)return;beforeId=0;list.removeAllViews();load(true);}
    private void load(final boolean initial){
        if(loading)return;loading=true;more.setEnabled(false);final long cursor=beforeId;status.setText("기록을 불러오는 중");
        io.execute(new Runnable(){public void run(){List<TripHistory.Trip> result=null;String error=null;TripHistory db=new TripHistory(HistoryActivity.this);
            try{result=db.page(cursor);}catch(RuntimeException e){error="기록을 불러오지 못했습니다";}finally{db.close();}
            final List<TripHistory.Trip> rows=result;final String failure=error;ui.post(new Runnable(){public void run(){
                if(dead)return;loading=false;more.setEnabled(true);
                if(failure!=null){status.setText(failure);return;}
                if(rows.isEmpty()&&initial){status.setText("저장된 운행 기록이 없습니다");more.setVisibility(View.GONE);return;}
                for(final TripHistory.Trip trip:rows){
                    LinearLayout card=new LinearLayout(HistoryActivity.this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(18,10,18,10);card.setBackgroundColor(0xff25343c);
                    TextView heading=text(date(trip.startedAt)+"  ·  "+trip.region,20,0xffeff7f7);card.addView(heading);
                    TextView details=text(String.format(Locale.KOREA,"%,d원    %.2fkm    %s    %s",trip.fare,trip.distance/1000d,time(trip.elapsed),state(trip)),17,0xffbbd0d4);card.addView(details);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=8;list.addView(card,lp);card.setClickable(true);card.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showDetail(trip);}});
                    beforeId=trip.id;
                }
                status.setText("운행을 선택하면 상세 내역을 볼 수 있습니다");more.setVisibility(rows.size()==TripHistory.PAGE_SIZE?View.VISIBLE:View.GONE);
            }});
        }});
    }
    private void showDetail(TripHistory.Trip t){
        String message="지역  "+t.region+"\n시작  "+date(t.startedAt)+"\n종료  "+date(t.endedAt)+"\n상태  "+state(t)
            +String.format(Locale.KOREA,"\n\n요금  %,d원\n기본  %,d원  ·  추가  %,d원\n거리  %.2fkm\n운행  %s  ·  저속  %s",t.fare,t.base,Math.max(0,t.fare-t.base),t.distance/1000d,time(t.elapsed),time(t.slow))
            +"\n시계외 선택  "+(t.outsideUsed?"사용":"없음")+"  ·  심야 시간  "+(t.nightUsed?"포함":"없음")+"\n\n차속 기반 추정 기록입니다.";
        new AlertDialog.Builder(this).setTitle("운행 상세").setMessage(message).setPositiveButton("닫기",null).show();
    }
    private static String date(long ms){return ms<=0?"시간 정보 없음":new SimpleDateFormat("yyyy.MM.dd HH:mm",Locale.KOREA).format(new Date(ms));}
    private static String time(long ms){long sec=Math.max(0,ms)/1000;return String.format(Locale.KOREA,"%02d:%02d:%02d",sec/3600,(sec/60)%60,sec%60);}
    private static String state(TripHistory.Trip t){return t.interrupted?"운행 중단":t.complete?"운행 종료":"신호 누락";}
    @Override protected void onDestroy(){if(viewport!=null)viewport.close();dead=true;ui.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}
