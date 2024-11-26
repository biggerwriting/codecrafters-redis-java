package demo;

import domain.ServerInfo;

import java.io.*;
import java.net.Socket;

import static demo.CommandHandle.*;
import static demo.CommandHandle.buildRespArray;
import static demo.Utils.readBuffLine;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/26
 */
public class ReplicaHandle extends Thread {
   final  ServerInfo serverInfo;

    public ReplicaHandle(ServerInfo serverInfo) {
        this.serverInfo =serverInfo;
    }


    @Override
    public void run() {
        try {
            handShake();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 建立副本
    public void handShake() {

        try (Socket socket = new Socket(serverInfo.getMasterHost(), Integer.parseInt(serverInfo.getMasterPort()));

             DataInputStream inputStream = new DataInputStream(socket.getInputStream());

             OutputStream outputStream = socket.getOutputStream()) {
            // slave 三次握手和 master建立连接
            // PING
            outputStream.write("*1\r\n$4\r\nPING\r\n".getBytes());
            System.out.println("PING 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            // REPLCONF listening-port <PORT>
            String message = buildRespArray("REPLCONF", "listening-port", config.get(PORT));
            outputStream.write(message.getBytes());
            System.out.println("REPLCONF listening-port 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            //REPLCONF capa psync2
            message = buildRespArray("REPLCONF", "capa", "psync2");
            outputStream.write(message.getBytes());
            System.out.println("REPLCONF capa psync2 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            // PSYNC ? -1
            message = buildRespArray("PSYNC", "?", "-1");
            outputStream.write(message.getBytes());
            System.out.println("PSYNC ? -1 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            // todo 实际服务器还有话说, 如何知道他说完了呢
            while(!(message = readBuffLine(inputStream)).isEmpty()){
                System.out.println("服务端还有话说：【" + message+ "】");
            }
            System.out.println("向服务器建立连接完毕");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
