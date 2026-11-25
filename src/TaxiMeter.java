package taximeter;

/** Experimental meter. Times are monotonic milliseconds; speeds are km/h.
 * The caller owns persistence, sampling and presentation. This is not a certified fare meter. */
public final class TaxiMeter {
    public static final class Tariff {
        public final int baseWon, incrementWon;
        public final double baseMetres, incrementMetres, slowBelowKmh;
        public final long slowIncrementMs, maxSampleGapMs;
        public Tariff(int baseWon, double baseMetres, int incrementWon, double incrementMetres,
                      double slowBelowKmh, long slowIncrementMs, long maxSampleGapMs) {
            if (baseWon < 0 || incrementWon <= 0 || !positive(baseMetres)
                    || !positive(incrementMetres) || !positive(slowBelowKmh)
                    || slowIncrementMs <= 0 || maxSampleGapMs <= 0)
                throw new IllegalArgumentException("invalid tariff");
            this.baseWon = baseWon; this.baseMetres = baseMetres;
            this.incrementWon = incrementWon; this.incrementMetres = incrementMetres;
            this.slowBelowKmh = slowBelowKmh; this.slowIncrementMs = slowIncrementMs;
            this.maxSampleGapMs = maxSampleGapMs;
        }
        private static boolean positive(double n) { return n > 0 && !Double.isInfinite(n) && !Double.isNaN(n); }
    }
    public static final class Snapshot {
        public final boolean running, complete;
        public final long elapsedMs, slowMs, fareWon;
        public final double distanceMetres;
        Snapshot(boolean running, boolean complete, long elapsedMs, long slowMs, long fareWon, double distanceMetres) {
            this.running = running; this.complete = complete; this.elapsedMs = elapsedMs;
            this.slowMs = slowMs; this.fareWon = fareWon; this.distanceMetres = distanceMetres;
        }
        /** Presentation-layer adjustments retain measurement completeness and totals. */
        public Snapshot withFare(long amount) {
            if (amount < 0) throw new IllegalArgumentException("invalid fare");
            return new Snapshot(running,complete,elapsedMs,slowMs,amount,distanceMetres);
        }
        /** Restored totals cannot be resumed: the unobserved interval is unknown. */
        public static Snapshot restored(boolean complete, long elapsedMs, long slowMs, long fareWon, double distanceMetres) {
            if (elapsedMs < 0 || slowMs < 0 || slowMs > elapsedMs || fareWon < 0
                    || Double.isNaN(distanceMetres) || Double.isInfinite(distanceMetres) || distanceMetres < 0)
                throw new IllegalArgumentException("invalid saved meter");
            return new Snapshot(false, complete, elapsedMs, slowMs, fareWon, distanceMetres);
        }
    }
    private final Tariff tariff;
    private boolean started, running, complete, haveSample;
    private long startMs, endMs, previousMs, lastObservationMs;
    private double previousKmh, metres, slowMillis;
    public TaxiMeter(Tariff tariff) { if (tariff == null) throw new IllegalArgumentException("tariff"); this.tariff = tariff; }
    public void start(long nowMs) {
        if (nowMs < 0 || running) throw new IllegalStateException("invalid start");
        started = true; running = true; complete = true; haveSample = false;
        startMs = endMs = nowMs; previousMs = 0; lastObservationMs = -1; slowMillis = 0; metres = 0;
    }
    /** Call with NaN for an unavailable reading. A gap makes the total explicitly incomplete. */
    public void sample(long nowMs, double speedKmh) {
        if (!running || nowMs < startMs || nowMs <= lastObservationMs)
            throw new IllegalStateException("invalid sample time");
        lastObservationMs = nowMs;
        if (Double.isNaN(speedKmh) || Double.isInfinite(speedKmh) || speedKmh < 0 || speedKmh > 300) {
            complete = false; haveSample = false; return;
        }
        if (!haveSample) {
            if (nowMs != startMs) complete = false;
        } else {
            long dt = nowMs - previousMs;
            if (dt > tariff.maxSampleGapMs) complete = false;
            else {
                metres += (previousKmh + speedKmh) * 0.5 * dt / 3600.0;
                // Interpolate the fraction below the threshold, matching the distance model.
                boolean beforeSlow = previousKmh < tariff.slowBelowKmh;
                boolean afterSlow = speedKmh < tariff.slowBelowKmh;
                if (beforeSlow && afterSlow) slowMillis += dt;
                else if (beforeSlow != afterSlow) {
                    double fraction = beforeSlow
                        ? (tariff.slowBelowKmh - previousKmh) / (speedKmh - previousKmh)
                        : (tariff.slowBelowKmh - speedKmh) / (previousKmh - speedKmh);
                    slowMillis += dt * fraction;
                }
            }
        }
        previousMs = nowMs; previousKmh = speedKmh; haveSample = true;
    }
    public void stop(long nowMs) {
        if (!running || nowMs < startMs || nowMs < lastObservationMs) throw new IllegalStateException("invalid stop");
        if (!haveSample || nowMs != previousMs) complete = false;
        running = false; endMs = nowMs;
    }
    public Snapshot snapshot(long nowMs) {
        if (!started) return new Snapshot(false, false, 0, 0, 0, 0);
        if (nowMs < startMs || (running && nowMs < lastObservationMs)) throw new IllegalStateException("invalid time");
        long elapsed = (running ? nowMs : endMs) - startMs;
        long distanceSteps = (long)Math.floor(Math.max(0, metres - tariff.baseMetres) / tariff.incrementMetres);
        long slowMs = (long) slowMillis;
        long timeSteps = slowMs / tariff.slowIncrementMs;
        long steps = distanceSteps > Long.MAX_VALUE - timeSteps ? Long.MAX_VALUE : distanceSteps + timeSteps;
        long fare = steps > (Long.MAX_VALUE - tariff.baseWon) / tariff.incrementWon
                ? Long.MAX_VALUE : tariff.baseWon + steps * tariff.incrementWon;
        boolean current = complete && (!running || (haveSample && nowMs == previousMs));
        return new Snapshot(running, current, elapsed, slowMs, fare, metres);
    }
}
