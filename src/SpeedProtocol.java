package taximeter;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/** Bounded decoder for the dedicated speed service's two-frame reply. */
final class SpeedProtocol {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    static byte[] request() {
        byte[] a="vehicle".getBytes(UTF8), b="speed".getBytes(UTF8);
        return ByteBuffer.allocate(16+a.length+b.length).order(ByteOrder.LITTLE_ENDIAN)
            .put(new byte[]{'L','F','D','1'}).putInt(2).putInt(a.length).put(a).putInt(b.length).put(b).array();
    }
    static double read(DataInputStream in) throws IOException {
        String text = null;
        for (int i=0; i<4; i++) {
            int type=in.readUnsignedByte();
            int len=Integer.reverseBytes(in.readInt());
            if (len < 0 || len > 1024) throw new IOException("invalid frame");
            byte[] body=new byte[len]; in.readFully(body);
            if (type=='O') {
                if (text != null) throw new IOException("duplicate output");
                text=new String(body,UTF8).trim();
            } else if (type=='X') {
                if (len != 4 || ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN).getInt()!=0) return Double.NaN;
                return text != null && text.startsWith("speed ") ? parseKmh(text.substring(6)) : Double.NaN;
            } else throw new IOException("unknown frame");
        }
        throw new IOException("missing exit frame");
    }
    static double parseKmh(String text) {
        if (text == null || text.trim().isEmpty() || "-".equals(text.trim())) return Double.NaN;
        try {
            double value=Double.parseDouble(text.trim());
            return Double.isNaN(value)||Double.isInfinite(value)||value<0||value>300 ? Double.NaN : value;
        } catch (NumberFormatException e) { return Double.NaN; }
    }
}
