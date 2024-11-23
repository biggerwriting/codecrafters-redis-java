import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static ConcurrentHashMap<String, String> setDict = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        //  Uncomment this block to pass the first stage
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        try {
            serverSocket = new ServerSocket(port);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            while (true) {
                // Wait for connection from client.
                clientSocket = serverSocket.accept();
                Socket finalClientSocket = clientSocket;
                new Thread(() -> {
                    try {
                        handleCommend(finalClientSocket);
                    } catch (Exception e) {
                        System.out.println("Exception: " + e.getMessage());
                    }
                }).start();
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

    private static void handleCommend(Socket clientSocket) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        InputStream inputStream = clientSocket.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if ("ping".equalsIgnoreCase(line)) {
                System.out.println("server received a new [ping] line:" + line);
                //System.out.println("the whole command:"+new DataInputStream(inputStream).readUTF());
                outputStream.write("+PONG\r\n".getBytes());
            } else if ("echo".equalsIgnoreCase(line)) {
                System.out.println("server received a new [echo] line:" + line);
                String message = bufferedReader.readLine();// 这个数据是message的长度
                System.out.println("message:" + message);
                message = bufferedReader.readLine();
                System.out.println("message:" + message);
                outputStream.write(String.format("$%d\r\n%s\r\n", message.length(), message).getBytes());
            } else if ("set".equalsIgnoreCase(line)) {
                System.out.println("set command");
                bufferedReader.readLine();
                String key = bufferedReader.readLine();
                bufferedReader.readLine();
                String value = bufferedReader.readLine();
                setDict.put(key, value);
                outputStream.write("+OK\r\n".getBytes());
            } else if ("get".equalsIgnoreCase(line)) {
                System.out.println("get command");
                bufferedReader.readLine();
                String key = bufferedReader.readLine();
                String message = setDict.get(key);
                if (null != message) {
                    outputStream.write(String.format("$%d\r\n%s\r\n", message.length(), message).getBytes());
                } else {
                    outputStream.write("$-1\r\n".getBytes());
                }
            } else {
                //System.out.println("   *** unkonw command:"+line);
            }

        }
    }
}
