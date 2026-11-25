package hmgapi.vehicle;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import hmgapi.engine.CarMap;
import hmgapi.engine.SystemCheck;
import hmgapi.hook.Points;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Fixed-purpose, read-only taximeter vehicle bridge, started and stopped by HMG-API. */
public final class TaxiSpeedHub {
    private static final String SOCKET = "taximeter-speed-v1";
    private static final String PKG = "dev.ev6.taximeter.prototype";
    private static final byte[] REQUEST = request();
    private static byte[] request() {
        String[] a = {"vehicle", "speed"};
        int n = 8; for (String s : a) n += 4 + s.length();
        ByteBuffer b = ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN);
        b.put(new byte[]{'L','F','D','1'}).putInt(a.length);
        for (String s : a) { byte[] x = s.getBytes(java.nio.charset.Charset.forName("UTF-8")); b.putInt(x.length).put(x); }
        return b.array();
    }
    private static int appUid() throws IOException {
        BufferedReader r = new BufferedReader(new FileReader("/data/system/packages.list"));
        try { String line; while ((line = r.readLine()) != null) {
            String[] v = line.trim().split(" +");
            if (v.length > 1 && PKG.equals(v[0])) return Integer.parseInt(v[1]);
        }} finally { r.close(); }
        throw new IOException("taximeter uid missing");
    }
    private static void frame(OutputStream out, int type, byte[] body) throws IOException {
        out.write(type); int n=body.length;
        out.write(n & 255); out.write((n >>> 8) & 255); out.write((n >>> 16) & 255); out.write((n >>> 24) & 255);
        out.write(body); out.flush();
    }
    private static void serve(LocalSocket s) throws Exception {
        if (s.getPeerCredentials().getUid() != appUid()) return;
        s.setSoTimeout(650);
        byte[] raw = new byte[REQUEST.length]; new DataInputStream(s.getInputStream()).readFully(raw);
        if (!Arrays.equals(raw, REQUEST)) return;
        String answer = "speed " + Hub.value("speed") + "\n";
        OutputStream out = s.getOutputStream();
        frame(out, 'O', answer.getBytes("UTF-8"));
        frame(out, 'X', new byte[]{0,0,0,0});
    }
    private static void close(LocalSocket s) {
        try { s.shutdownOutput(); } catch (Exception ignored) {}
        try {
            java.io.FileDescriptor fd = s.getFileDescriptor();
            if (fd == null) throw new IOException("missing fd");
            Class.forName("libcore.io.IoUtils").getMethod("close", java.io.FileDescriptor.class).invoke(null, fd);
        } catch (Exception e) { try { s.close(); } catch (Exception ignored) {} }
    }
    public static void main(String[] args) throws Exception {
        Hub.facts = new SystemCheck.Facts("", null);
        Points.use(CarMap.load("", Hub.facts, null));
        if (!Points.ready()) throw new IOException("vehicle map unavailable");
        Hub.hcan = new dalvik.system.PathClassLoader(Hub.HCAN_JAR, Hub.class.getClassLoader());
        LocalServerSocket server = new LocalServerSocket(SOCKET);
        System.out.println("taximeter read-only bridge ready");
        while (true) { LocalSocket s = server.accept(); try { serve(s); }
            catch (Exception e) { System.out.println("vehicle read failed: " + e); } finally { close(s); } }
    }
}
