package dev.ev6.taximeter.prototype;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.graphics.Bitmap;
import android.view.View;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.RemoteViews;
import kr.blackr.split.sdk.WidgetProtocol;
import taximeter.HorseTempo;

/** Meter frames are published only while the selected custom panel subscribes. */
public final class SplitMeterProvider extends Service {
    private static volatile SplitMeterProvider active;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Messenger client;
    private String instance;
    private RemoteViews latest;
    private boolean moving;
    private float speedKmh;
    private int phase;
    private final Bitmap[] horseFrames = new Bitmap[8];
    private final Runnable animate = new Runnable() { public void run() {
        if (client == null || instance == null || latest == null) return;
        RemoteViews frame = latest.clone();
        frame.setViewVisibility(R.id.meter_horse, View.VISIBLE);
        int current=moving ? phase++%horseFrames.length : 0;
        if(horseFrames[current]==null) horseFrames[current]=HorsePainter.frame(current);
        frame.setImageViewBitmap(R.id.meter_horse,horseFrames[current]);
        Message message = Message.obtain(null, WidgetProtocol.FRAME);
        Bundle data = new Bundle();data.putInt(WidgetProtocol.KEY_VERSION, WidgetProtocol.VERSION);
        data.putString(WidgetProtocol.KEY_INSTANCE,instance);data.putParcelable(WidgetProtocol.KEY_REMOTE_VIEWS,frame);message.setData(data);
        try { client.send(message); if(moving) main.postDelayed(this,HorseTempo.frameDelayMs(speedKmh)); }
        catch(RemoteException e) { stopUpdates(); }
    }};
    private void stopUpdates() { main.removeCallbacks(animate);client=null;instance=null;latest=null;moving=false; }

    private final Messenger incoming = new Messenger(new Handler(Looper.getMainLooper()) {
        @Override public void handleMessage(Message message) {
            Bundle data = message.getData();
            String token = data == null ? null : data.getString(WidgetProtocol.KEY_INSTANCE);
            if (token == null || token.length() > 128) return;
            if (message.what == WidgetProtocol.SUBSCRIBE && message.replyTo != null
                    && data.getInt(WidgetProtocol.KEY_VERSION, -1) == WidgetProtocol.VERSION) {
                client = message.replyTo;
                instance = token;
                startService(new Intent(SplitMeterProvider.this, MeterService.class).setAction(MeterService.REFRESH));
            } else if (message.what == WidgetProtocol.UNSUBSCRIBE && token.equals(instance)) {
                stopUpdates();
            }
        }
    });
    @Override public void onCreate() { super.onCreate(); HorsePainter.init(getResources()); active = this; }
    @Override public IBinder onBind(Intent intent) { return incoming.getBinder(); }
    static void publish(final RemoteViews views, final boolean moving, final float speedKmh) {
        final SplitMeterProvider provider = active;
        if (provider == null) return;
        provider.main.post(new Runnable() { public void run() {
            if (active != provider || provider.client == null || provider.instance == null) return;
            provider.latest=views;provider.moving=moving;provider.speedKmh=speedKmh;provider.main.removeCallbacks(provider.animate);provider.main.post(provider.animate);
        }});
    }
    @Override public boolean onUnbind(Intent intent) { stopUpdates(); return false; }
    @Override public void onDestroy() {
        if (active == this) active = null;
        stopUpdates();main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
