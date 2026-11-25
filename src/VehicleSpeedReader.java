package taximeter;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Blocking connection to the meter's restricted local speed-only bridge. */
public final class VehicleSpeedReader {
    public double readKmh() {
        LocalSocket socket = new LocalSocket();
        try {
            socket.connect(new LocalSocketAddress("taximeter-speed-v1", LocalSocketAddress.Namespace.ABSTRACT));
            if (socket.getPeerCredentials().getUid() != 0) return Double.NaN;
            socket.setSoTimeout(1500);
            OutputStream out=socket.getOutputStream(); out.write(SpeedProtocol.request()); out.flush();
            return SpeedProtocol.read(new DataInputStream(socket.getInputStream()));
        } catch (IOException e) { return Double.NaN; }
        catch (RuntimeException e) { return Double.NaN; }
        finally { try { socket.close(); } catch (IOException ignored) {} }
    }
    static double parseKmh(String text) { return SpeedProtocol.parseKmh(text); }
}
