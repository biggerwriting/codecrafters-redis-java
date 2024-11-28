import domain.ServerInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/26
 */
public class Connection extends Thread {

    // 处理redis-cli请求的线程池
    static ExecutorService executor = Executors.newCachedThreadPool();
    ServerInfo serverInfo;

    public Connection(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Override
    public void run() {
        initiateConnections();
    }

    public void initiateConnections() {
        //  Uncomment this block to pass the first stage
        try (ServerSocket serverSocket = new ServerSocket(serverInfo.getPort())) {
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            while (true) {
                // Wait for connection from client.
                Socket clientSocket = serverSocket.accept();
                CommandHandle commandHandle = new CommandHandle(clientSocket, serverInfo);
                executor.execute(commandHandle);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
