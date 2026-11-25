package dev.ev6.taximeter.prototype;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.util.Locale;
import taximeter.TaxiMeter;

public final class MeterWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        c.startService(new Intent(c, MeterService.class).setAction(MeterService.REFRESH));
    }
    static void render(Context c, TaxiMeter.Snapshot s, boolean signal, boolean interrupted, boolean moving, float speedKmh) {
        RemoteViews v = makeViews(c, s, signal, interrupted);
        try { AppWidgetManager.getInstance(c).updateAppWidget(new ComponentName(c, MeterWidget.class), v); } catch (RuntimeException ignored) { }
        SplitMeterProvider.publish(v, moving, speedKmh);
    }
    static RemoteViews makeViews(Context c, TaxiMeter.Snapshot s, boolean signal, boolean interrupted) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.taximeter_widget);
        boolean running = s != null && s.running;
        android.content.SharedPreferences p=c.getSharedPreferences("meter", Context.MODE_PRIVATE);
        FareProfiles profile=FareProfiles.resolved(p.getString(s==null?"fare_profile":"fare_trip_profile",p.getString("fare_profile","seoul")),p,s!=null);
        FareProfiles controlProfile=FareProfiles.resolved(p.getString(running?"fare_trip_profile":"fare_profile","seoul"),p,running);
        boolean outside=controlProfile.supportsOutside() && p.getBoolean("outside",false);
        String status=running?profile.title+" · 운행 중":controlProfile.title+" · 다음 운행";
        if(interrupted)status+="  (이전 운행 중단)";
        v.setTextViewText(R.id.meter_status,status);
        v.setTextViewText(R.id.meter_fare, s == null ? "— 원" : String.format(Locale.KOREA, "%,d 원", s.fareWon));
        v.setTextViewText(R.id.meter_distance, s == null ? "거리\n— km" : String.format(Locale.KOREA, "거리\n%.2f km", s.distanceMetres / 1000));
        v.setTextViewText(R.id.meter_elapsed, s == null ? "운행\n—:—" : "운행\n" + time(s.elapsedMs));
        v.setTextViewText(R.id.meter_slow, s == null ? "저속\n—:—" : "저속\n" + time(s.slowMs));
        long base=s==null?profile.baseWon:p.getLong("fare_trip_base",profile.baseWon);
        String breakdown=s==null?"기본 · 추가 —":String.format(Locale.KOREA,"기본 %,d + 추가 %,d원",base,Math.max(0,s.fareWon-base));
        if(!running && s!=null && !profile.id.equals(controlProfile.id))breakdown="최근 운행: "+profile.title+"  ·  "+breakdown;
        v.setTextViewText(R.id.meter_breakdown,breakdown);
        v.setTextViewText(R.id.meter_warning, p.getBoolean("history_error",false) ? "운행 기록 저장 실패" : interrupted ? "중단 전까지의 기록 · 자동 재개 안 됨" : (s == null ? "추정 요금 · 차량 신호 확인 필요" : (running && !signal ? "차량 신호 없음" : (!s.complete ? "누락 구간 있음 · 실제 요금 아님" : ("custom".equals(profile.id)?"추정 요금 · 할증 미반영":("gunsan".equals(profile.id)?"추정 요금 · 복합할증 미반영":"추정 요금 · 시계외 수동"))))));
        v.setTextViewText(R.id.meter_toggle, running ? "운행 종료" : "운행 시작");
        Intent action = new Intent(c, MeterService.class).setAction(running ? MeterService.STOP : MeterService.START);
        v.setOnClickPendingIntent(R.id.meter_toggle, PendingIntent.getService(c, 0, action, PendingIntent.FLAG_UPDATE_CURRENT));
        Intent open=new Intent(c,MeterActivity.class).putExtra("from_widget",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.meter_open,PendingIntent.getActivity(c,2,open,PendingIntent.FLAG_UPDATE_CURRENT));
        v.setViewVisibility(R.id.meter_outside,controlProfile.supportsOutside()?android.view.View.VISIBLE:android.view.View.GONE);
        v.setTextViewText(R.id.meter_outside,outside?"시계외 켬":"시계외 끔");
        Intent outsideAction=new Intent(c,MeterService.class).setAction(outside?MeterService.OUTSIDE_OFF:MeterService.OUTSIDE_ON);
        v.setOnClickPendingIntent(R.id.meter_outside,PendingIntent.getService(c,1,outsideAction,PendingIntent.FLAG_UPDATE_CURRENT));
        return v;
    }
    private static String time(long ms) { long s = ms / 1000; return String.format(Locale.KOREA, "%02d:%02d", s / 60, s % 60); }
}
