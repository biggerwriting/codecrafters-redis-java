package demo;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/26
 */
public class Utils {

    public static String readBuffLine(InputStreamReader inputStreamReader) throws IOException {
        char[] charArray = new char[1024];
        int readLength = inputStreamReader.read(charArray);
        if(readLength != -1){
            return new String(charArray, 0, readLength);
        }else {
            return "";
        }
    }
}
