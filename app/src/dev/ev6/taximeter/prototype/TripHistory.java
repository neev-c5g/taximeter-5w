package dev.ev6.taximeter.prototype;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Private on-device history. A stable trip key makes recovery inserts idempotent. */
final class TripHistory extends SQLiteOpenHelper {
    private static final String DATABASE="trips.db";
    static final int PAGE_SIZE=50;
    static final class Trip {
        long id,startedAt,endedAt,fare,base,elapsed,slow;double distance;
        String key,region;boolean complete,interrupted,outsideUsed,nightUsed;
    }
    TripHistory(Context context){super(context,DATABASE,null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE trips (_id INTEGER PRIMARY KEY AUTOINCREMENT, trip_key TEXT NOT NULL UNIQUE, started_at INTEGER NOT NULL, ended_at INTEGER NOT NULL, region TEXT NOT NULL, fare INTEGER NOT NULL, base INTEGER NOT NULL, distance REAL NOT NULL, elapsed INTEGER NOT NULL, slow INTEGER NOT NULL, complete INTEGER NOT NULL, interrupted INTEGER NOT NULL, outside_used INTEGER NOT NULL, night_used INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("unsupported history version");}
    boolean add(Trip t){
        if(t==null||t.key==null||t.key.isEmpty()||t.region==null||t.region.isEmpty()||t.startedAt<0||t.endedAt<0||t.fare<0||t.base<0||t.elapsed<0||t.slow<0||t.slow>t.elapsed||Double.isNaN(t.distance)||Double.isInfinite(t.distance)||t.distance<0)throw new IllegalArgumentException("invalid trip");
        ContentValues v=new ContentValues();v.put("trip_key",t.key);v.put("started_at",t.startedAt);v.put("ended_at",t.endedAt);v.put("region",t.region);v.put("fare",t.fare);v.put("base",t.base);v.put("distance",t.distance);v.put("elapsed",t.elapsed);v.put("slow",t.slow);v.put("complete",t.complete?1:0);v.put("interrupted",t.interrupted?1:0);v.put("outside_used",t.outsideUsed?1:0);v.put("night_used",t.nightUsed?1:0);
        return getWritableDatabase().insertWithOnConflict("trips",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
    }
    List<Trip> page(long beforeId){
        ArrayList<Trip> rows=new ArrayList<Trip>();String selection=beforeId>0?"_id < ?":null;String[] args=beforeId>0?new String[]{String.valueOf(beforeId)}:null;
        Cursor c=getReadableDatabase().query("trips",null,selection,args,null,null,"_id DESC",String.valueOf(PAGE_SIZE));
        try{while(c.moveToNext())rows.add(read(c));}finally{c.close();}return rows;
    }
    private static Trip read(Cursor c){
        Trip t=new Trip();t.id=c.getLong(c.getColumnIndexOrThrow("_id"));t.key=c.getString(c.getColumnIndexOrThrow("trip_key"));t.startedAt=c.getLong(c.getColumnIndexOrThrow("started_at"));t.endedAt=c.getLong(c.getColumnIndexOrThrow("ended_at"));t.region=c.getString(c.getColumnIndexOrThrow("region"));t.fare=c.getLong(c.getColumnIndexOrThrow("fare"));t.base=c.getLong(c.getColumnIndexOrThrow("base"));t.distance=c.getDouble(c.getColumnIndexOrThrow("distance"));t.elapsed=c.getLong(c.getColumnIndexOrThrow("elapsed"));t.slow=c.getLong(c.getColumnIndexOrThrow("slow"));t.complete=c.getInt(c.getColumnIndexOrThrow("complete"))!=0;t.interrupted=c.getInt(c.getColumnIndexOrThrow("interrupted"))!=0;t.outsideUsed=c.getInt(c.getColumnIndexOrThrow("outside_used"))!=0;t.nightUsed=c.getInt(c.getColumnIndexOrThrow("night_used"))!=0;return t;
    }
}
