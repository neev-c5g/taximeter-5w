package dev.ev6.taximeter.prototype;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;
import java.util.Locale;

/** A full-screen fallback that uses exactly the same meter service and saved result. */
public final class MeterActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences prefs;
    private TextView title, fare, detail, warning;
    private Button toggle;
    private HorseView horse;
    private Spinner region;
    private Button editFare;
    private SplitViewport viewport;
    private android.widget.CheckBox outside;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("meter",MODE_PRIVATE);
        FrameLayout outer=new FrameLayout(this);outer.setBackgroundColor(0xff111c24);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(40,18,40,18);
        outer.addView(root,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=label(28);heading.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);heading.setText("택시미터기");
        header.addView(heading,new LinearLayout.LayoutParams(0,68,1));
        Button history=new Button(this);history.setText("운행 기록");header.addView(history,new LinearLayout.LayoutParams(220,68));
        history.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MeterActivity.this,HistoryActivity.class));}});root.addView(header);
        region=new Spinner(this);String[] names=new String[FareProfiles.ALL.length+1];for(int i=0;i<FareProfiles.ALL.length;i++)names[i]=FareProfiles.ALL[i].title;names[names.length-1]="직접 설정";
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,names);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);region.setAdapter(adapter);
        region.setSelection("custom".equals(prefs.getString("fare_profile","seoul"))?FareProfiles.ALL.length:FareProfiles.indexOf(prefs.getString("fare_profile","seoul")));
        region.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
            if(prefs.getBoolean("running",false))return;
            if(position==FareProfiles.ALL.length&&!prefs.getBoolean("custom_configured",false)){showFareEditor();return;}
            String selected=position==FareProfiles.ALL.length?"custom":FareProfiles.ALL[position].id;
            if(!selected.equals(prefs.getString("fare_profile","seoul"))){prefs.edit().putString("fare_profile",selected).apply();refreshWidget();}
        }});
        LinearLayout choices=new LinearLayout(this);choices.setGravity(Gravity.CENTER_VERTICAL);TextView regionLabel=label(16);regionLabel.setText("지역 요금");choices.addView(regionLabel,new LinearLayout.LayoutParams(130,64));
        choices.addView(region,new LinearLayout.LayoutParams(360,64));editFare=new Button(this);editFare.setText("요금 직접 설정");
        editFare.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showFareEditor();}});choices.addView(editFare,new LinearLayout.LayoutParams(260,64));root.addView(choices);
        LinearLayout body=new LinearLayout(this);body.setGravity(Gravity.CENTER_VERTICAL);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        FrameLayout art=new FrameLayout(this);horse=new HorseView(this);art.addView(horse,new FrameLayout.LayoutParams(-1,-1));body.addView(art,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout readout=new LinearLayout(this);readout.setOrientation(LinearLayout.VERTICAL);readout.setGravity(Gravity.CENTER);body.addView(readout,new LinearLayout.LayoutParams(0,-1,2));
        title=label(22);title.setTextColor(0xff85d7cb);fare=label(60);detail=label(21);readout.addView(title);readout.addView(fare);readout.addView(detail);
        warning=label(16);warning.setTextColor(0xffffca80);root.addView(warning);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);root.addView(actions,new LinearLayout.LayoutParams(-1,80));
        outside=new android.widget.CheckBox(this);outside.setText("시계외 할증 (직접 선택)");outside.setTextColor(Color.WHITE);
        outside.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startService(new Intent(MeterActivity.this,MeterService.class).setAction(outside.isChecked()?MeterService.OUTSIDE_ON:MeterService.OUTSIDE_OFF));}});
        actions.addView(outside,new LinearLayout.LayoutParams(0,80,1));toggle=new Button(this);toggle.setTextSize(22);actions.addView(toggle,new LinearLayout.LayoutParams(420,80));
        toggle.setOnClickListener(new View.OnClickListener(){public void onClick(View v){boolean running=prefs.getBoolean("running",false);startService(new Intent(MeterActivity.this,MeterService.class).setAction(running?MeterService.STOP:MeterService.START));}});
        setContentView(outer);viewport=new SplitViewport(this,root);requestCloseFromWidget(getIntent());
        prefs.registerOnSharedPreferenceChangeListener(this); refresh();
        startService(new Intent(this,MeterService.class).setAction(MeterService.REFRESH));
    }
    private TextView label(int size){TextView t=new TextView(this); t.setGravity(Gravity.CENTER);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(4,8,4,8);return t;}
    private static String time(long ms){long s=ms/1000;return String.format(Locale.KOREA,"%02d:%02d",s/60,s%60);}
    private void refresh(){
        boolean present=prefs.getBoolean("present",false),running=prefs.getBoolean("running",false),complete=prefs.getBoolean("complete",false);
        long amount=prefs.getLong("fare",0),elapsed=prefs.getLong("elapsed",0),slow=prefs.getLong("slow",0);
        double distance=Double.longBitsToDouble(prefs.getLong("distance",0));
        String profileId=prefs.getString(running?"fare_trip_profile":"fare_profile","seoul");
        FareProfiles current=FareProfiles.resolved(profileId,prefs,running);
        outside.setVisibility(current.supportsOutside()?View.VISIBLE:View.GONE);outside.setChecked(current.supportsOutside()&&prefs.getBoolean("outside",false));
        region.setEnabled(!running);editFare.setEnabled(!running);horse.setSpeed(prefs.getFloat("speed_kmh",0f));horse.setRunning(running && prefs.getBoolean("speed_signal",false));horse.setVisibility(View.VISIBLE);
        title.setText(running?"운행 중":present?"운행 종료":"운행 대기");
        fare.setText(present?String.format(Locale.KOREA,"%,d 원",amount):"— 원");
        detail.setText(present?String.format(Locale.KOREA,"거리 %.2f km     운행 %s     저속 %s",distance/1000,time(elapsed),time(slow)):"거리 — km     운행 —:—     저속 —:—");
        warning.setText(prefs.getBoolean("history_error",false)?"운행 기록을 저장하지 못했습니다. 저장 공간을 확인해 주세요":prefs.getBoolean("signal_error",false)?"차량 신호 없음 · 운행을 시작할 수 없습니다":present&&!complete?"신호 누락 또는 중단된 기록 · 실제 요금 아님":("custom".equals(prefs.getString(present?"fare_trip_profile":"fare_profile","seoul"))?"추정 요금 · 할증 미반영":("gunsan".equals(prefs.getString(present?"fare_trip_profile":"fare_profile","seoul"))?"추정 요금 · 복합할증 미반영":"추정 요금 · 시계외 수동 선택")));
        toggle.setText(running?"운행 종료":"운행 시작");
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);requestCloseFromWidget(intent);}
    private void requestCloseFromWidget(Intent intent){
        if(intent==null||!intent.getBooleanExtra("from_widget",false))return;intent.removeExtra("from_widget");
        final android.content.Context context=getApplicationContext();
        new Thread(new Runnable(){public void run(){try{context.getContentResolver().call(android.net.Uri.parse("content://kr.blackr.customsplit.config"),"request_close",null,null);}catch(RuntimeException ignored){}}},"close-widget-panel").start();
    }
    private void showFareEditor(){
        if(prefs.getBoolean("running",false))return;
        String selected=prefs.getString("fare_profile","seoul");FareProfiles current=FareProfiles.resolved(selected,prefs,false);
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(24,8,24,8);
        final EditText name=input(fields,"지역 이름",current.title,false),base=input(fields,"기본요금 (원)",String.valueOf(current.baseWon),true),
            baseM=input(fields,"기본거리 (m)",String.valueOf(current.baseMetres),true),step=input(fields,"100원당 거리 (m)",String.valueOf(current.incrementMetres),true),
            seconds=input(fields,"100원당 저속시간 (초)",String.valueOf(current.incrementSeconds),true),slow=input(fields,"저속 기준 (km/h, 소수점 가능)",String.valueOf(current.slowBelowKmh),true);
        ScrollView scroll=new ScrollView(this);scroll.addView(fields);
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("지역 요금 직접 설정").setView(scroll).setNegativeButton("취소",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){restoreRegionSelection();}}).setPositiveButton("저장",null).create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener(){public void onCancel(DialogInterface d){restoreRegionSelection();}});
        dialog.setOnShowListener(new DialogInterface.OnShowListener(){public void onShow(DialogInterface d){dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            try {
                String title=name.getText().toString().trim();int b=Integer.parseInt(base.getText().toString()),m=Integer.parseInt(baseM.getText().toString()),dist=Integer.parseInt(step.getText().toString()),sec=Integer.parseInt(seconds.getText().toString());
                double kmh=Double.parseDouble(slow.getText().toString());int tenth=(int)Math.round(kmh*10);
                if(title.isEmpty()||title.length()>30||b<0||b>100000||m<1||m>100000||dist<1||dist>10000||sec<1||sec>3600||Double.isNaN(kmh)||Double.isInfinite(kmh)||tenth<1||tenth>3000)throw new IllegalArgumentException();
                prefs.edit().putString("custom_name",title).putInt("custom_base",b).putInt("custom_base_m",m).putInt("custom_step_m",dist).putInt("custom_seconds",sec).putInt("custom_slow_tenth",tenth).putBoolean("custom_configured",true).putString("fare_profile","custom").apply();
                region.setSelection(FareProfiles.ALL.length);refreshWidget();dialog.dismiss();
            }catch(RuntimeException ex){Toast.makeText(MeterActivity.this,"입력한 요금과 단위를 확인해 주세요",Toast.LENGTH_SHORT).show();}
        }});}});dialog.show();
    }
    private EditText input(LinearLayout parent,String hint,String value,boolean number){
        EditText field=new EditText(this);field.setSingleLine(true);field.setHint(hint);field.setContentDescription(hint);
        if(number)field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextView label=label(15);label.setText(hint);parent.addView(label);field.setText(value);parent.addView(field);return field;
    }
    private void refreshWidget(){startService(new Intent(this,MeterService.class).setAction(MeterService.REFRESH));}
    private void restoreRegionSelection(){String id=prefs.getString("fare_profile","seoul");region.setSelection("custom".equals(id)?FareProfiles.ALL.length:FareProfiles.indexOf(id));}
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){runOnUiThread(new Runnable(){public void run(){refresh();}});}
    @Override protected void onDestroy(){if(viewport!=null)viewport.close();prefs.unregisterOnSharedPreferenceChangeListener(this);super.onDestroy();}
}
