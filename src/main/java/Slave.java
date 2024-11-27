import demo.CommandHandle;
import domain.ServerInfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static demo.CommandHandle.buildRespArray;
import static demo.Utils.log;
import static demo.Utils.readBuffLine;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/26
 */
public class Slave {
    public static void initiateSlaveConnection(ServerInfo serverInfo) {

        try (Socket socket = new Socket(serverInfo.getMasterHost(), Integer.parseInt(serverInfo.getMasterPort()));

             DataInputStream inputStream = new DataInputStream(socket.getInputStream());

             OutputStream outputStream = socket.getOutputStream()) {
            // slave 三次握手和 master建立连接
            // PING
            outputStream.write("*1\r\n$4\r\nPING\r\n".getBytes());
            System.out.println("[" + serverInfo.getRole()+"]"+"PING 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            // REPLCONF listening-port <PORT>
            String message = buildRespArray("REPLCONF", "listening-port", String.valueOf(serverInfo.getPort()));
            outputStream.write(message.getBytes());
            System.out.println("[" + serverInfo.getRole()+"]"+"REPLCONF listening-port 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            //REPLCONF capa psync2
            message = buildRespArray("REPLCONF", "capa", "psync2");
            outputStream.write(message.getBytes());
            System.out.println("[" + serverInfo.getRole()+"]"+"REPLCONF capa psync2 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            // PSYNC ? -1
            message = buildRespArray("PSYNC", "?", "-1");
            outputStream.write(message.getBytes());
            System.out.println("[" + serverInfo.getRole()+"]"+"PSYNC ? -1 得到服务端输出：【" + readBuffLine(inputStream) + "】");

            // 作为redis服务器，处理cli请求
            new Connection(serverInfo).start();

//            while (replicaMaster(inputStream)){
//
//            }

            // todo 实际服务器还有话说, 如何知道他说完了呢
            while (!(message = readBuffLine(inputStream)).isEmpty()) {
                log(serverInfo, "服务端还有话说【",message,"】");
                String[] split = message.split("\\*");
                for (int i = 1; i < split.length; i++) {
                    String singleCommand = "*"+split[i];
                    // 处理set命令
                    CommandHandle commandHandle = new CommandHandle(socket, serverInfo);
                    String response = commandHandle.processCommand(singleCommand);

                    //The master will then send REPLCONF GETACK * to your replica. It'll expect to receive REPLCONF ACK 0 as a reply.
                    if("*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$1\r\n0\r\n".equalsIgnoreCase(response)){
                        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            System.out.println("[" + serverInfo.getRole()+"]"+"ERROR 向服务器建立连接完毕, 服务器异常断开连接");
        } catch (IOException e) {
            System.out.println("[" + serverInfo.getRole()+"]"+"IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }

//    private static boolean replicaMaster(DataInputStream inputStream) {
//        String message = readBuffLine(inputStream);
//        if ()
//    }
}
