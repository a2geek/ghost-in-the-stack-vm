package a2geek.ghost.command.util;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ByteFormatter {
    private ByteArrayInputStream data;
    public static ByteFormatter from(byte[] bytes) {
        ByteFormatter bf = new ByteFormatter();
        bf.data = new ByteArrayInputStream(bytes);
        return bf;
    }

    public boolean hasMore() {
        return data.available() > 0;
    }

    public String get(int nBytes) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (int i=0; i<nBytes; i++) {
            int b = data.read();
            if (b == -1) break;
            pw.printf("%02x ", b);
        }
        return sw.toString();
    }
}