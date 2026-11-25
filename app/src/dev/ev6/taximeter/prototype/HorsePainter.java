package dev.ev6.taximeter.prototype;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/** Eight generated neon-horse poses, cut from a uniformly divided transparent sheet. */
final class HorsePainter {
    static final int WIDTH=288, HEIGHT=216;
    private static volatile Bitmap sheet;
    private static final Paint FILTER=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private HorsePainter() {}
    static synchronized void init(Resources resources) {
        if(sheet!=null || resources==null)return;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
        try {sheet=BitmapFactory.decodeResource(resources,R.drawable.horse_gallop,options);}
        catch(RuntimeException ignored) { sheet=null; }
    }
    static Bitmap frame(int phase) {
        Bitmap bitmap=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888);
        draw(new Canvas(bitmap),phase);return bitmap;
    }
    static void draw(Canvas canvas,int phase) {
        Bitmap src=sheet;if(src==null)return;
        int index=((phase%8)+8)%8;int col=index%4,row=index/4;
        int left=col*src.getWidth()/4,right=(col+1)*src.getWidth()/4;
        int top=row*src.getHeight()/2,bottom=(row+1)*src.getHeight()/2;
        int height=bottom-top;
        // Trim empty bands while keeping all four hooves and the mane in each cell.
        Rect cell=new Rect(left,top+height*8/100,right,top+height*88/100);
        canvas.drawBitmap(src,cell,new RectF(0,0,WIDTH,HEIGHT),FILTER);
    }
}
