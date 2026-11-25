package taximeter;
public final class HorseTempoTest {
    public static void main(String[] args){
        long low=HorseTempo.frameDelayMs(5),medium=HorseTempo.frameDelayMs(50),high=HorseTempo.frameDelayMs(120);
        if(!(low>medium&&medium>high&&high>=75))throw new AssertionError("speed should increase animation tempo");
        if(HorseTempo.frameDelayMs(300)!=HorseTempo.frameDelayMs(140)||HorseTempo.frameDelayMs(Double.NaN)!=HorseTempo.frameDelayMs(0))throw new AssertionError("bounded and invalid speed");
        System.out.println("HorseTempoTest passed");
    }
}
