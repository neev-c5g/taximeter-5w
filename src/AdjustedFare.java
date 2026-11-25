package taximeter;

/** Applies each increment at its current night rate without changing previous charges. */
public final class AdjustedFare {
    private long plain, adjusted;
    private boolean started;
    public long begin(long raw, int percent) {
        valid(raw, percent);plain=raw;started=true;
        adjusted=Math.round((raw*(100.0+percent)/100.0)/100.0)*100;
        return adjusted;
    }
    public long update(long raw,int percent) {
        valid(raw,percent);if(!started||raw<plain)throw new IllegalStateException("fare moved backwards");
        long extra=raw-plain,factor=100L+percent;
        long added=extra>Long.MAX_VALUE/factor?Long.MAX_VALUE:extra*factor/100L;
        adjusted=adjusted>Long.MAX_VALUE-added?Long.MAX_VALUE:adjusted+added;plain=raw;return adjusted;
    }
    private static void valid(long raw,int percent){if(raw<0||percent<0||percent>100)throw new IllegalArgumentException();}
}
