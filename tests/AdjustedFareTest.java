package taximeter;
public final class AdjustedFareTest {
    public static void main(String[] args) {
        AdjustedFare a=new AdjustedFare();
        if(a.begin(4800,20)!=5800||a.update(4900,20)!=5920||a.update(5000,40)!=6060||a.update(5100,0)!=6160)throw new AssertionError("incremental night changes");
        if(a.begin(4800,40)!=6700||a.update(4800,0)!=6700)throw new AssertionError("base not retroactive");
        boolean bad=false;try{a.update(4700,0);}catch(IllegalStateException e){bad=true;}if(!bad)throw new AssertionError("backwards fare");
        System.out.println("AdjustedFareTest passed");
    }
}
