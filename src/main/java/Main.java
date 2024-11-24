import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");
        for (int i = 0; i < args.length; i++) {
            //./your_program.sh  --dir /tmp/redis-files --dbfilename dump.rdb
            String param = args[i];
            if ("--dir".equalsIgnoreCase(param)) {
                CommandHandle.config.put(CommandHandle.CONFIG_DIR, args[++i]);
            }
            if ("--dbfilename".equalsIgnoreCase(param)) {
                CommandHandle.config.put(CommandHandle.CONFIG_DBFILENAME, args[++i]);
            }

        }

        //  Uncomment this block to pass the first stage
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        try {
            ExecutorService executor = Executors.newFixedThreadPool(5);
            serverSocket = new ServerSocket(port);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            while (true) {
                System.out.println("get a connection");
                // Wait for connection from client.
                clientSocket = serverSocket.accept();
                CommandHandle commandHandle = new CommandHandle(clientSocket);
                executor.execute(commandHandle);
                System.out.println("处理结束");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
