import demo.ExpiryValue;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/23
 */
public class CommandHandle extends Thread {
    public static Map<String, String> config = new HashMap<String, String>();
    public static ConcurrentHashMap<String, ExpiryValue> setDict = new ConcurrentHashMap<>();
    public static final String CONFIG_DBFILENAME="dbfilename";
    public static final String CONFIG_DIR="dir";

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
                        response = buildResponse(message);
                        break;
                    }
                    case "GET": {
                        ExpiryValue expiryValue = setDict.get(tokens.get(1));
                        if (expiryValue != null && expiryValue.expiry > System.currentTimeMillis()) {
                            String message = expiryValue.value;
                            response = buildResponse(message);
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
                    case "CONFIG":{
                        // the expected response to CONFIG GET dir is:
                        //*2\r\n$3\r\ndir\r\n$16\r\n/tmp/redis-files\r\n
                        if("get".equalsIgnoreCase(tokens.get(1))){
                            if(CONFIG_DIR.equalsIgnoreCase(tokens.get(2))){
                                String dir = config.get(CONFIG_DIR);
                                response ="*2\r\n"+ buildResponse(CONFIG_DIR)+buildResponse(dir);
                            }else if(CONFIG_DBFILENAME.equalsIgnoreCase(tokens.get(2))){
                                String dbfilename = config.get(CONFIG_DBFILENAME);
                                response ="*2\r\n"+ buildResponse(CONFIG_DBFILENAME)+buildResponse(dbfilename);
                            }
                        }else {

                            response = "$-1\r\n";
                        }
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

    private static String buildResponse(String message) {
        String response;
        response = String.format("$%d\r\n%s\r\n", message.length(), message);
        return response;
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
