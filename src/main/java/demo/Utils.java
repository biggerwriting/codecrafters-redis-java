package demo;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/26
 */
public class Utils {

    public static String readBuffLine(DataInputStream inputStream) throws IOException {
        byte[] array = new byte[1024];
        int readLength = inputStream.read(array);
        if (readLength != -1) {
            return new String(array, 0, readLength);
        } else {
            return "";
        }
    }
}
