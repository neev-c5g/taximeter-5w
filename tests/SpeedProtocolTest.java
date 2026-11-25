package taximeter;
import java.io.*;
import java.util.Arrays;
public final class SpeedProtocolTest {
    static byte[] frame(int type, byte[] body) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(type);int n=body.length;
        out.write(n&255);out.write(n>>8&255);out.write(n>>16&255);out.write(n>>24&255);out.write(body);return out.toByteArray();
    }
    static byte[] join(byte[]... parts) throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] p:parts)out.write(p);return out.toByteArray();}
    static double read(byte[] b) throws IOException {return SpeedProtocol.read(new DataInputStream(new ByteArrayInputStream(b)));}
    static void check(boolean v) {if(!v)throw new AssertionError();}
    static void rejects(byte[] b) throws Exception {boolean failed=false;try {read(b);}catch(IOException expected){failed=true;}check(failed);}
    public static void main(String[] args) throws Exception {
        byte[] ok=frame('X',new byte[]{0,0,0,0});
        check(read(join(frame('O',"speed 42.5\n".getBytes("UTF-8")),ok))==42.5);
        check(Double.isNaN(read(join(frame('O',"speed -\n".getBytes("UTF-8")),ok))));
        check(Double.isNaN(read(join(frame('O',"speed 5\n".getBytes("UTF-8")),frame('X',new byte[]{1,0,0,0})))));
        rejects(join(frame('O',"speed 5".getBytes("UTF-8")),frame('O',"speed 6".getBytes("UTF-8")),ok));
        rejects(new byte[]{'O',1,4,0,0}); rejects(new byte[]{'O',4,0,0,0,'x'});
        check(Arrays.equals(SpeedProtocol.request(),new byte[]{'L','F','D','1',2,0,0,0,7,0,0,0,'v','e','h','i','c','l','e',5,0,0,0,'s','p','e','e','d'}));
        System.out.println("SpeedProtocolTest passed");
    }
}
