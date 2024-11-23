package demo;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/23
 */
public class CommandHandle extends Thread {
    public static ConcurrentHashMap<String, ExpiryValue> setDict = new ConcurrentHashMap<>();

    private final Socket socket;

    public CommandHandle(Socket socket) {
        this.socket = socket;
    }


    void handle(Socket socket) throws IOException {
        try (OutputStream outputStream = socket.getOutputStream();
             InputStream inputStream = socket.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        ) {
            char[] charArray = new char[1024];
            System.out.println("read begin " + System.currentTimeMillis());
            int readLength;
            while ((readLength = inputStreamReader.read(charArray)) != -1) {
                String line = new String(charArray, 0, readLength);
                System.out.println("得到客户端请求：【" + line + "】");
                String response = null;
                List<String> tokens = parse(line);
                System.out.println("命令参数：" + tokens);
                switch (tokens.get(0).toUpperCase()) {
                    case "PING": {
                        response = "+PONG\r\n";
                        break;
                    }
                    case "ECHO": {
                        String message = tokens.size() > 1 ? tokens.get(1) : "";
                        response = String.format("$%d\r\n%s\r\n", message.length(), message);
                        break;
                    }
                    case "GET": {
                        ExpiryValue expiryValue = setDict.get(tokens.get(1));
                        if (expiryValue != null && expiryValue.expiry > System.currentTimeMillis()) {
                            String message = expiryValue.value;
                            response = String.format("$%d\r\n%s\r\n", message.length(), message);
                        } else {
                            setDict.remove(tokens.get(1));
                            response = "$-1\r\n";
                        }
                        break;
                    }
                    case "SET": {
                        long expiry = Long.MAX_VALUE;
                        if (tokens.size() > 3 && "px".equalsIgnoreCase(tokens.get(3))) {
                            expiry = System.currentTimeMillis() + Long.parseLong(tokens.get(4));
                        }
                        setDict.put(tokens.get(1), new ExpiryValue(tokens.get(2), expiry));
                        response = "+OK\r\n";
                        break;
                    }
                    default: {
                        response = "$-1\r\n";
                        break;
                    }
                }
                System.out.println("response = " + response);
                outputStream.write(response.getBytes());
            }
            System.out.println("read end " + System.currentTimeMillis());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }

    }

    List<String> parse(String param) {
        List<String> args = new ArrayList<>();
        if (param.startsWith("*")) {
            String[] array = param.split("\r\n");
            System.out.println(Arrays.asList(array));
            int size = Integer.valueOf(array[0].substring(1));
            System.out.println(size);
            for (int i = 2; i < array.length; i += 2) {
                args.add(array[i]);
            }
        } else {
            args.add(param);
        }
        return args;
    }

    @Override
    public void run() {
        try {
            handle(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
