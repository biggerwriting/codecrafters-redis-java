package demo;

import java.io.DataInputStream;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/26
 */
public class Utils {

    public static String readBuffLine(DataInputStream inputStream) {
        byte[] array = new byte[1024];
        int readLength = 0;
        try {
            readLength = inputStream.read(array);
        } catch (Exception e) {
            System.out.println("! read error: "+e.getMessage());
            e.printStackTrace();
        }
        if (readLength > 0) {
            return new String(array, 0, readLength);
        } else {
            return "";
        }
    }
}
