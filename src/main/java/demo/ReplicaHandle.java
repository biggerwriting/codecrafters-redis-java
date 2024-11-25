package demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import static demo.CommandHandle.*;
import static demo.CommandHandle.buildRespArray;
import static demo.Utils.readBuffLine;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/26
 */
public class ReplicaHandle extends Thread {
    @Override
    public void run() {
        try {
            handShake();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 建立副本
    public static void handShake() {
        String host = config.get(MASTER_HOST);
        int port = Integer.parseInt(config.get(MASTER_PORT));

        try (Socket socket = new Socket(host, port);
             InputStream inputStream = socket.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

             OutputStream outputStream = socket.getOutputStream();) {
            // PING
            outputStream.write("*1\r\n$4\r\nPING\r\n".getBytes());
            System.out.println("PING 得到服务端输出：【" + readBuffLine(inputStreamReader) + "】");
            // REPLCONF listening-port <PORT>
            String message = buildRespArray("REPLCONF", "listening-port", config.get(PORT));
            outputStream.write(message.getBytes());
            System.out.println("REPLCONF listening-port 得到服务端输出：【" + readBuffLine(inputStreamReader) + "】");
            //REPLCONF capa psync2
            message = buildRespArray("REPLCONF", "capa", "psync2");
            outputStream.write(message.getBytes());
            System.out.println("REPLCONF capa psync2 得到服务端输出：【" + readBuffLine(inputStreamReader) + "】");
            // PSYNC ? -1
            message = buildRespArray("PSYNC", "?", "-1");
            outputStream.write(message.getBytes());
            System.out.println("PSYNC ? -1 得到服务端输出：【" + readBuffLine(inputStreamReader) + "】");
            // todo 实际服务器还有话说, 如何知道他说完了呢
            while(!(message = readBuffLine(inputStreamReader)).isEmpty()){
                System.out.println("服务端还有话说：【" + message+ "】");
            }
            System.out.println("向服务器建立连接完毕");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
