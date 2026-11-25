package taximeter;

public final class VehicleSpeedReaderTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        check(VehicleSpeedReader.parseKmh(" 42.5 ") == 42.5);
        check(VehicleSpeedReader.parseKmh("0") == 0);
        for (String value : new String[] {null, "", "-", "oops", "NaN", "Infinity", "-1", "301"})
            check(Double.isNaN(VehicleSpeedReader.parseKmh(value)));
        System.out.println("VehicleSpeedReaderTest passed");
    }
}
