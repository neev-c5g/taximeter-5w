package dev.ev6.taximeter.prototype;
import java.util.HashSet;
public final class FareProfilesTest {
    public static void main(String[] args) {
        HashSet<String> ids=new HashSet<String>();
        for(FareProfiles p:FareProfiles.ALL){
            if(!ids.add(p.id)||p.baseWon<=0||p.baseMetres<=0||p.incrementMetres<=0||p.incrementSeconds<=0||!p.source.startsWith("https://"))throw new AssertionError(p.id);
            p.tariff();
        }
        FareProfiles seoul=FareProfiles.byId("seoul"), gunsan=FareProfiles.byId("gunsan"), daejeon=FareProfiles.byId("daejeon");
        if(seoul.baseWon!=4800||seoul.baseMetres!=1600||gunsan.baseWon!=4300||gunsan.incrementMetres!=134||daejeon.baseMetres!=1650||daejeon.incrementSeconds!=29)throw new AssertionError();
        if(seoul.nightPercent(21)!=0||seoul.nightPercent(22)!=20||seoul.nightPercent(23)!=40||seoul.nightPercent(2)!=20||seoul.nightPercent(4)!=0)throw new AssertionError("Seoul night schedule");
        FareProfiles busan=FareProfiles.byId("busan");
        if(busan.nightPercent(22)!=0||busan.nightPercent(23)!=20||busan.nightPercent(0)!=30||busan.nightPercent(2)!=20||busan.nightPercent(4)!=0)throw new AssertionError("Busan night schedule");
        if(gunsan.nightPercent(23)!=0||gunsan.nightPercent(0)!=20||gunsan.nightPercent(4)!=0)throw new AssertionError("Gunsan night schedule");
        if(seoul.surchargePercent(12,true)!=20||seoul.surchargePercent(23,true)!=60||seoul.surchargePercent(23,false)!=40)throw new AssertionError("Seoul outside");
        if(busan.surchargePercent(12,true)!=30||busan.surchargePercent(23,true)!=40||busan.surchargePercent(0,true)!=50)throw new AssertionError("Busan overlap");
        FareProfiles incheon=FareProfiles.byId("incheon"),daegu=FareProfiles.byId("daegu");
        if(incheon.surchargePercent(23,true)!=70||daegu.surchargePercent(23,true)!=40||daejeon.surchargePercent(0,true)!=50)throw new AssertionError("region overlap");
        System.out.println("FareProfilesTest passed");
    }
}
