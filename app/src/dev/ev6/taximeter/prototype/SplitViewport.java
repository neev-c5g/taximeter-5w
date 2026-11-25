package dev.ev6.taximeter.prototype;

import android.app.Activity;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;

/** Keep app controls in the visible portion while the stock popup is open. */
final class SplitViewport {
    private final Activity activity;private final View child;private final ContentObserver observer;
    SplitViewport(Activity activity,View child){
        this.activity=activity;this.child=child;
        observer=new ContentObserver(new Handler(Looper.getMainLooper())){@Override public void onChange(boolean self){update();}};
        try{activity.getContentResolver().registerContentObserver(Settings.System.getUriFor("split_screen_status"),false,observer);}catch(RuntimeException ignored){}
        update();
    }
    private void update(){
        int state=-1;try{state=Settings.System.getInt(activity.getContentResolver(),"split_screen_status",-1);}catch(RuntimeException ignored){}
        int screen=activity.getResources().getDisplayMetrics().widthPixels;
        int width=state==0?FrameLayout.LayoutParams.MATCH_PARENT:Math.max(1,screen*2/3-8);
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)child.getLayoutParams();if(lp!=null&&lp.width!=width){lp.width=width;child.setLayoutParams(lp);}
    }
    void close(){try{activity.getContentResolver().unregisterContentObserver(observer);}catch(RuntimeException ignored){}}
}
