package dev.ev6.taximeter.prototype;

import taximeter.TaxiMeter;
import android.content.SharedPreferences;

/** Daytime standard-taxi estimates. The displayed source data is not a certified meter. */
final class FareProfiles {
    final String id, title, source;
    final int baseWon, baseMetres, incrementMetres, incrementSeconds;
    final double slowBelowKmh;
    FareProfiles(String id,String title,int baseWon,int baseMetres,int incrementMetres,int incrementSeconds,double threshold,String source){
        this.id=id;this.title=title;this.baseWon=baseWon;this.baseMetres=baseMetres;this.incrementMetres=incrementMetres;this.incrementSeconds=incrementSeconds;this.slowBelowKmh=threshold;this.source=source;
    }
    int nightPercent(int hour){
        if(hour<0||hour>23)throw new IllegalArgumentException("hour");
        if("seoul".equals(id)||"incheon".equals(id))return hour>=23||hour<2?40:(hour>=22||hour<4?20:0);
        if("busan".equals(id)||"daejeon".equals(id)||"daegu".equals(id))return hour<2?30:(hour==23||hour<4?20:0);
        if("ulsan".equals(id))return hour>=22||hour<4?20:0;
        if("gunsan".equals(id))return hour<4?20:0;
        return 0;
    }
    boolean supportsOutside(){return !"custom".equals(id);}
    int surchargePercent(int hour,boolean outside){
        int night=nightPercent(hour);if(!outside||!supportsOutside())return night;
        if("seoul".equals(id))return night+20;
        if("incheon".equals(id))return night+30;
        if("busan".equals(id))return night==0?30:(night==30?50:40);
        if("daegu".equals(id))return night==0?35:40;
        if("daejeon".equals(id))return night==0?30:50;
        if("ulsan".equals(id))return night==0?30:50;
        if("gunsan".equals(id))return night+20;
        return night;
    }
    TaxiMeter.Tariff tariff(){return new TaxiMeter.Tariff(baseWon,baseMetres,100,incrementMetres,slowBelowKmh,incrementSeconds*1000L,4000);}
    static final FareProfiles[] ALL={
        new FareProfiles("seoul","서울 중형",4800,1600,131,30,15.72,"https://news.seoul.go.kr/traffic/archives/1659"),
        new FareProfiles("busan","부산 중형",4800,2000,132,33,15.0,"https://www.busan.go.kr/depart/ahtaxi03"),
        new FareProfiles("incheon","인천 중형",4800,1600,135,33,15.0,"https://www.incheon.go.kr/traffic/TR010201"),
        new FareProfiles("daegu","대구 중형",4500,1700,125,31,15.0,"https://www.daegu.go.kr/tra/index.do?menu_id=00000647"),
        new FareProfiles("daejeon","대전 중형",4800,1650,125,29,15.0001,"https://www.daejeon.go.kr/drh/board/boardNormalView.do?boardId=normal_0189&ntatcSeq=1525249286"),
        new FareProfiles("ulsan","울산 중형",4500,2000,125,30,15.0,"https://ulsan.go.kr/u/traffic/contents.ulsan?mId=001003006001000000"),
        new FareProfiles("gunsan","전북",4300,2000,134,32,15.0001,"https://arts.gunsan.go.kr/main/m140/view/1309726")
    };
    static FareProfiles resolved(String id,SharedPreferences preferences,boolean trip){
        if(!"custom".equals(id))return byId(id);
        String key=trip?"trip_custom_":"custom_";
        int base=preferences.getInt(key+"base",4800),distance=preferences.getInt(key+"base_m",1600),step=preferences.getInt(key+"step_m",131),seconds=preferences.getInt(key+"seconds",30),slow=preferences.getInt(key+"slow_tenth",150);
        if(base<0||base>100000||distance<1||distance>100000||step<1||step>10000||seconds<1||seconds>3600||slow<1||slow>3000)return byId("seoul");
        String name=preferences.getString(key+"name","직접 설정");
        return new FareProfiles("custom",name,base,distance,step,seconds,slow/10.0,"직접 입력");
    }
    static FareProfiles byId(String id){for(FareProfiles p:ALL)if(p.id.equals(id))return p;return ALL[0];}
    static int indexOf(String id){for(int i=0;i<ALL.length;i++)if(ALL[i].id.equals(id))return i;return 0;}
}
