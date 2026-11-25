package dev.ev6.taximeter.prototype;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;
import taximeter.HorseTempo;

final class HorseView extends View {
    private float speedKmh;private int phase;private boolean running;
    HorseView(Context c) { super(c); HorsePainter.init(c.getResources()); }
    void setSpeed(float value){speedKmh=value;invalidate();}
    void setRunning(boolean value){if(running!=value){running=value;invalidate();}}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale=Math.min(getWidth()/(float)HorsePainter.WIDTH,getHeight()/(float)HorsePainter.HEIGHT);
        canvas.save();canvas.translate((getWidth()-HorsePainter.WIDTH*scale)/2f,(getHeight()-HorsePainter.HEIGHT*scale)/2f);canvas.scale(scale,scale);
        HorsePainter.draw(canvas,running?phase++%8:0);canvas.restore();
        if (running && getVisibility()==VISIBLE) postInvalidateDelayed(HorseTempo.frameDelayMs(speedKmh));
    }
}
