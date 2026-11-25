package taximeter;

public final class TaxiMeterTest {
    private static void check(boolean v, String m) { if (!v) throw new AssertionError(m); }
    private static TaxiMeter.Tariff tariff() { return new TaxiMeter.Tariff(4800, 1600, 100, 131, 15, 30000, 2000); }
    public static void main(String[] args) {
        TaxiMeter m = new TaxiMeter(tariff());
        m.start(0); m.sample(0, 36);
        for (int i = 1; i <= 180; i++) m.sample(i * 1000L, 36);
        TaxiMeter.Snapshot s = m.snapshot(180000);
        check(Math.abs(s.distanceMetres - 1800) < .001, "distance integral");
        check(s.fareWon == 4900 && s.slowMs == 0 && s.complete, "distance fare");
        m.stop(180000); check(!m.snapshot(200000).running && m.snapshot(200000).elapsedMs == 180000, "stopped result");
        m.start(200000); m.sample(200000, 0);
        for (int i = 1; i <= 30; i++) m.sample(200000 + i * 1000L, 0);
        s = m.snapshot(230000); check(s.fareWon == 4900 && s.slowMs == 30000 && s.complete, "low speed fare");
        m.sample(234000, 30); check(!m.snapshot(234000).complete && m.snapshot(234000).distanceMetres == 0, "gap not integrated");
        m.sample(235000, Double.NaN); m.sample(236000, 36);
        check(!m.snapshot(236000).complete && m.snapshot(236000).distanceMetres == 0, "missing reading not integrated");
        m.stop(237000); check(!m.snapshot(237000).complete, "unobserved end marked incomplete");
        TaxiMeter.Snapshot restored = TaxiMeter.Snapshot.restored(false, 30000, 1000, 4900, 120.5);
        check(!restored.running && !restored.complete && restored.distanceMetres == 120.5, "restore interrupted totals");
        boolean invalidRestore = false;
        try { TaxiMeter.Snapshot.restored(false, 1, 2, 4800, 0); } catch (IllegalArgumentException e) { invalidRestore = true; }
        check(invalidRestore, "invalid saved totals rejected");
        boolean rejected = false;
        try { new TaxiMeter.Tariff(1, Double.NaN, 1, 1, 1, 1, 1); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "invalid tariff rejected");
        TaxiMeter crossing = new TaxiMeter(tariff());
        crossing.start(0); crossing.sample(0, 0); crossing.sample(1000, 30); crossing.sample(2000, 0);
        check(crossing.snapshot(2000).slowMs == 1000, "interpolate threshold crossings");
        TaxiMeter boundary = new TaxiMeter(tariff());
        boundary.start(0); boundary.sample(0, 15); boundary.sample(1000, 15);
        check(boundary.snapshot(1000).slowMs == 0, "threshold itself is not low speed");
        System.out.println("TaxiMeterTest passed");
    }
}
