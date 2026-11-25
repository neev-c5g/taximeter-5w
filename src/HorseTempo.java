package taximeter;

/** Current km/h controls frame interval; independent of any vehicle's top speed. */
public final class HorseTempo {
    private HorseTempo() {}
    public static long frameDelayMs(double speedKmh) {
        if(Double.isNaN(speedKmh)||Double.isInfinite(speedKmh)||speedKmh<0) speedKmh=0;
        return Math.max(75L,Math.round(220.0-Math.min(speedKmh,140.0)*1.0));
    }
}
