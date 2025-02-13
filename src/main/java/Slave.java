import domain.ServerInfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static demo.Utils.log;
import static demo.Utils.readBuffLine;
import static java.lang.System.exit;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/26
 */
public class Slave {
    private static ServerInfo serverInfo = ServerInfo.getInstance();

    public static void initiateSlaveConnection(ServerInfo serverInfo) {

        try (Socket socket = new Socket(serverInfo.getMasterHost(), Integer.parseInt(serverInfo.getMasterPort()));

             DataInputStream inputStream = new DataInputStream(socket.getInputStream());

             OutputStream outputStream = socket.getOutputStream()) {
            // slave 三次握手和 master建立连接
            // PING
            outputStream.write("*1\r\n$4\r\nPING\r\n".getBytes());
            log("三次握手 PING 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            // REPLCONF listening-port <PORT>
            String message = ProtocolParser.buildRespArray("REPLCONF", "listening-port", String.valueOf(serverInfo.getPort()));
            outputStream.write(message.getBytes());
            log("三次握手 REPLCONF listening-port 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            //REPLCONF capa psync2
            message = ProtocolParser.buildRespArray("REPLCONF", "capa", "psync2");
            outputStream.write(message.getBytes());
            log("三次握手 REPLCONF capa psync2 得到服务端输出：【" + readBuffLine(inputStream) + "】");
            // PSYNC ? -1
            message = ProtocolParser.buildRespArray("PSYNC", "?", "-1");
            outputStream.write(message.getBytes());
            log("三次握手 PSYNC ? -1 得到服务端输出：【" + ProtocolParser.parseInput(inputStream, null) + "】");
            ProtocolParser.readFile(inputStream);

            // 作为redis服务器，处理cli请求
            new Connection(serverInfo).start();

            // todo 实际服务器还有话说, 如何知道他说完了呢
            Object readMsg;
            while (null != (readMsg = ProtocolParser.parseInput(inputStream, serverInfo))) {
                if (readMsg instanceof String) {
                    log(serverInfo, "服务端还有话说 string【", (String) readMsg, "】");
                } else if (readMsg instanceof List) {
                    List<String> array = (List<String>) readMsg;
                    log(serverInfo, "服务端还有话说 array【", array.toString(), "】");

                    // 处理set命令
                    CommandHandle commandHandle = new CommandHandle(socket, serverInfo);
                    String response = commandHandle.processCommand(array);

                    //The master will then send REPLCONF GETACK * to your replica. It'll expect to receive REPLCONF ACK 0 as a reply.
                    if (response != null && response.contains("REPLCONF")) {
                        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                        log("返回给master【" ,response,"】");
                    }
                } else {
                    log(serverInfo, "服务端还有话说 不知说了啥【", readMsg.toString(), "】");
                }
            }
            log( "ERROR 向服务器建立连接完毕, 服务器异常断开连接");
//            exit(1);
        } catch (Exception e) {
            log( "从服务器异常 Exception: " + e.getMessage());
            e.printStackTrace();
            exit(2);
        }
    }
}
