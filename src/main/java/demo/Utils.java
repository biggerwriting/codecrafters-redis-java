package demo;

import domain.ServerInfo;

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
            System.out.println("! read error: " + e.getMessage());
            e.printStackTrace();
        }
        if (readLength > 0) {
            return new String(array, 0, readLength);
        } else {
            return "";
        }
    }

    //    public static void main(String[] args) {
//        log(new ServerInfo(), "hh","w\r\neee");
//    }
    public static void log(String... msg) {
        log(ServerInfo.getInstance(),  msg);
    }

    public static void log(ServerInfo serverInfo, String... msg) {
        StringBuffer sb = new StringBuffer();
        sb.append("[" + serverInfo.getRole() + "]");
        for (int i = 0; i < msg.length; i++) {
            // sb.append(lists[i].replaceAll("\n","\\\\n").replaceAll("\r","\\\\r"));
            sb.append(msg[i].replace("\n", "\\n").replace("\r", "\\r"));
        }
        System.out.println(sb);

    }

}
