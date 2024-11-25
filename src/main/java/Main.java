import demo.CommandHandle;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static demo.CommandHandle.*;

public class Main {

    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");
        // settings
        settings(args);
        // load database
        if (config.get(CommandHandle.CONFIG_DIR) != null) {
            try {
                CommandHandle.initSetDict();
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }
        }

        //  Uncomment this block to pass the first stage
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = getPort();
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

    private static void settings(String[] args) {
        for (int i = 0; i < args.length; i++) {
            //./your_program.sh  --dir /tmp/redis-files --dbfilename dump.rdb
            String param = args[i];
            if ("--dir".equalsIgnoreCase(param)) {
                config.put(CommandHandle.CONFIG_DIR, args[++i]);
            } else if ("--dbfilename".equalsIgnoreCase(param)) {
                config.put(CommandHandle.CONFIG_DBFILENAME, args[++i]);
            } else if ("--port".equalsIgnoreCase(param)) {
                config.put("port", args[++i]);
            } else if ("--replicaof".equalsIgnoreCase(param)) {// --replicaof "localhost 6379"
                config.put("role", "slave");
                String[] master = args[++i].split(" ");// <MASTER_HOST> <MASTER_PORT>
                config.put(CommandHandle.MASTER_HOST, master[0]);
                config.put(CommandHandle.MASTER_PORT, master[1]);
                handShake();
            }
        }
        // master_replid and master_repl_offset
        config.put(MASTER_REPLID, "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb");
        config.put(MASTER_REPL_OFFSET, "0");
    }

    private static void handShake() {
        String host = config.get(MASTER_HOST);
        int port = Integer.parseInt(config.get(MASTER_PORT));

        try (Socket socket = new Socket(host, port); InputStream inputStream = socket.getInputStream(); InputStreamReader inputStreamReader = new InputStreamReader(inputStream); BufferedReader bufferedReader = new BufferedReader(inputStreamReader); OutputStream outputStream = socket.getOutputStream();) {
            outputStream.write("*1\r\n$4\r\nPING\r\n".getBytes());
            System.out.println("得到服务端输出：【" + readBuffLine(inputStreamReader) + "】");

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String readBuffLine(InputStreamReader inputStreamReader) throws IOException {
        char[] charArray = new char[1024];
        int readLength = inputStreamReader.read(charArray);
        String line = new String(charArray, 0, readLength);
        return line;
    }

    private static int getPort() {
        try {
            if (config.get("port") != null) {
                return Integer.parseInt(config.get("port"));
            }
        } catch (Exception e) {
            return 6379;
        }
        return 6379;
    }

}
