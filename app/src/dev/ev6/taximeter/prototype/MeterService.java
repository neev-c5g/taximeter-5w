package dev.ev6.taximeter.prototype;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import java.util.Calendar;
import java.util.UUID;
import taximeter.TaxiMeter;
import taximeter.AdjustedFare;
import taximeter.VehicleSpeedReader;

/** Meter readings, persistence, and history writes are serialized on one worker. */
public final class MeterService extends Service {
    static final String START="taximeter.START", STOP="taximeter.STOP", REFRESH="taximeter.REFRESH", OUTSIDE_ON="taximeter.OUTSIDE_ON", OUTSIDE_OFF="taximeter.OUTSIDE_OFF";
    interface SpeedSource { double readKmh(); }
    private HandlerThread thread;private Handler worker;private TaxiMeter meter;private SpeedSource speedSource;
    private boolean running,signal,interrupted,moving,outsideUsed,nightUsed,recoveryBlocked;
    private TaxiMeter.Snapshot last;private long lastSampleAt,startedAt,endedAt,baseFare;private String tripKey;
    private final AdjustedFare pricing=new AdjustedFare();private FareProfiles activeProfile;private float speedKmh;
    private final Runnable tick=new Runnable(){public void run(){if(!running)return;readAndSample();worker.postDelayed(this,1000);}};
    @Override public void onCreate(){
        super.onCreate();thread=new HandlerThread("taximeter");thread.start();worker=new Handler(thread.getLooper());
        final VehicleSpeedReader reader=new VehicleSpeedReader();speedSource=new SpeedSource(){public double readKmh(){return reader.readKmh();}};
        SharedPreferences p=getSharedPreferences("meter",MODE_PRIVATE);activeProfile=FareProfiles.resolved(p.getString("fare_profile","seoul"),p,false);meter=new TaxiMeter(activeProfile.tariff());
        worker.post(new Runnable(){public void run(){restore();}});
    }
    @Override public int onStartCommand(final Intent intent,int flags,int id){
        final String action=intent==null?REFRESH:intent.getAction();
        worker.post(new Runnable(){public void run(){
            if(OUTSIDE_ON.equals(action)||OUTSIDE_OFF.equals(action)){
                boolean enabled=OUTSIDE_ON.equals(action);getSharedPreferences("meter",MODE_PRIVATE).edit().putBoolean("outside",enabled).apply();
                if(running&&enabled&&activeProfile.supportsOutside()){outsideUsed=true;save();}render();
            } else if(START.equals(action)&&!running){
                // A failed history write must not silently discard the previous completed trip.
                if(recoveryBlocked || (last!=null&&!archive())){render();return;}
                double speed=speedSource.readKmh();long now=SystemClock.elapsedRealtime();
                if(Double.isNaN(speed)){signal=false;getSharedPreferences("meter",MODE_PRIVATE).edit().putBoolean("signal_error",true).apply();render();return;}
                SharedPreferences p=getSharedPreferences("meter",MODE_PRIVATE);
                activeProfile=FareProfiles.resolved(p.getString("fare_profile","seoul"),p,false);meter=new TaxiMeter(activeProfile.tariff());
                tripKey=UUID.randomUUID().toString();startedAt=System.currentTimeMillis();endedAt=0;baseFare=0;outsideUsed=false;nightUsed=false;interrupted=false;
                meter.start(now);meter.sample(now,speed);lastSampleAt=now;running=true;signal=true;moving=speed>=1.0;speedKmh=(float)speed;
                last=price(meter.snapshot(now),true);baseFare=last.fareWon;save();render();worker.postDelayed(tick,1000);
            } else if(STOP.equals(action)&&running){
                worker.removeCallbacks(tick);readAndSample();meter.stop(lastSampleAt);running=false;moving=false;speedKmh=0f;endedAt=System.currentTimeMillis();
                last=price(meter.snapshot(lastSampleAt),false);save();archive();render();
            } else render();
        }});return START_NOT_STICKY;
    }
    void setSpeedSourceForTest(SpeedSource source){if(source==null)throw new IllegalArgumentException();speedSource=source;}
    private void readAndSample(){
        double speed=speedSource.readKmh();long now=SystemClock.elapsedRealtime();if(now<=lastSampleAt)return;
        meter.sample(now,speed);signal=!Double.isNaN(speed);moving=signal&&speed>=1.0;speedKmh=signal?(float)speed:0f;
        last=price(meter.snapshot(now),false);save();render();lastSampleAt=now;
    }
    private TaxiMeter.Snapshot price(TaxiMeter.Snapshot raw,boolean starting){
        int hour=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);boolean outside=getSharedPreferences("meter",MODE_PRIVATE).getBoolean("outside",false)&&activeProfile.supportsOutside();
        if(outside)outsideUsed=true;if(activeProfile.nightPercent(hour)>0)nightUsed=true;
        int percent=activeProfile.surchargePercent(hour,outside);
        return raw.withFare(starting?pricing.begin(raw.fareWon,percent):pricing.update(raw.fareWon,percent));
    }
    private void save(){
        if(last==null)return;
        getSharedPreferences("meter",MODE_PRIVATE).edit().putBoolean("present",true).putBoolean("running",running).putBoolean("complete",last.complete)
            .putBoolean("interrupted",interrupted).putBoolean("moving",moving).putBoolean("speed_signal",signal).putBoolean("signal_error",!signal&&running)
            .putFloat("speed_kmh",speedKmh).putLong("elapsed",last.elapsedMs).putLong("slow",last.slowMs).putLong("fare",last.fareWon)
            .putLong("distance",Double.doubleToRawLongBits(last.distanceMetres)).putString("trip_key",tripKey==null?"":tripKey)
            .putLong("trip_started_at",startedAt).putLong("trip_ended_at",endedAt).putLong("fare_trip_base",baseFare)
            .putBoolean("trip_outside_used",outsideUsed).putBoolean("trip_night_used",nightUsed).putString("fare_trip_profile",activeProfile.id)
            .putString("trip_custom_name",activeProfile.title).putInt("trip_custom_base",activeProfile.baseWon).putInt("trip_custom_base_m",activeProfile.baseMetres)
            .putInt("trip_custom_step_m",activeProfile.incrementMetres).putInt("trip_custom_seconds",activeProfile.incrementSeconds)
            .putInt("trip_custom_slow_tenth",(int)Math.round(activeProfile.slowBelowKmh*10)).commit();
    }
    private void restore(){
        SharedPreferences p=getSharedPreferences("meter",MODE_PRIVATE);if(!p.getBoolean("present",false))return;
        try{
            boolean wasRunning=p.getBoolean("running",false);interrupted=wasRunning||p.getBoolean("interrupted",false);
            activeProfile=FareProfiles.resolved(p.getString("fare_trip_profile",p.getString("fare_profile","seoul")),p,true);
            tripKey=p.getString("trip_key","");startedAt=p.getLong("trip_started_at",0);endedAt=wasRunning?0:p.getLong("trip_ended_at",0);
            baseFare=p.getLong("fare_trip_base",activeProfile.baseWon);outsideUsed=p.getBoolean("trip_outside_used",false);nightUsed=p.getBoolean("trip_night_used",false);
            last=TaxiMeter.Snapshot.restored(!interrupted&&p.getBoolean("complete",false),p.getLong("elapsed",-1),p.getLong("slow",-1),p.getLong("fare",-1),Double.longBitsToDouble(p.getLong("distance",0)));
            if(wasRunning){running=false;moving=false;signal=false;speedKmh=0f;save();}archive();
        }catch(RuntimeException e){last=null;interrupted=true;recoveryBlocked=true;p.edit().putBoolean("history_error",true).apply();}
    }
    private boolean archive(){
        if(last==null||running)return true;
        TripHistory.Trip t=new TripHistory.Trip();t.key=tripKey==null||tripKey.isEmpty()?"legacy-last":tripKey;t.startedAt=startedAt;t.endedAt=endedAt;
        t.region=activeProfile.title;t.fare=last.fareWon;t.base=baseFare;t.distance=last.distanceMetres;t.elapsed=last.elapsedMs;t.slow=last.slowMs;
        t.complete=last.complete;t.interrupted=interrupted;t.outsideUsed=outsideUsed;t.nightUsed=nightUsed;
        TripHistory history=new TripHistory(this);
        try{history.add(t);getSharedPreferences("meter",MODE_PRIVATE).edit().putBoolean("history_error",false).apply();return true;}
        catch(RuntimeException e){getSharedPreferences("meter",MODE_PRIVATE).edit().putBoolean("history_error",true).apply();return false;}
        finally{history.close();}
    }
    private void render(){MeterWidget.render(this,last,signal,interrupted,running&&signal,speedKmh);}
    @Override public void onDestroy(){worker.removeCallbacksAndMessages(null);thread.quit();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
