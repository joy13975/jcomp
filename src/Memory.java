import java.util.ArrayList;
import java.io.*;

public class Memory {

    static ArrayList<Byte> memory = new ArrayList<Byte>();

    static public int allocString(String str) {
        int addr = memory.size();
        int size = str.length();
        for (int i = 0; i < size; i++) {
            memory.add(new Byte("", str.charAt(i)));
        }
        //memory.add(new Byte("", 0));
        return addr;
    }

    public static int allocMem(int data) {
        int addr = memory.size();
        for (int i = 0; i < 4; i++) {
            memory.add(new Byte("", (data & (0x000000ff << 8*i)) >> 8*i));
        }
        //memory.add(new Byte("", 0));
        return addr;
    }

    public static int calloc(int size) {
        return allocMem(0);
    }

    static public void dumpData(PrintStream o) {
        Byte b;
        String s;
        int c;
        int size = memory.size();
        for (int i = 0; i < size; i++) {
            b = memory.get(i);
            c = b.getContents();
            if (c >= 32) {
                s = String.valueOf((char)c);
            } else {
                s = ""; // "\\"+String.valueOf(c);
            }
            o.println("DATA " + c + " ; " + s + " " + b.getName());
        }
    }
}

class Byte {
    String varname;
    int contents;

    Byte(String n, int c) {
        varname = n;
        contents = c;
    }

    String getName() {
        return varname;
    }

    int getContents() {
        return contents;
    }
}
