package demo;

import domain.ServerInfo;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static demo.Utils.readBuffLine;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/23
 */
public class CommandHandle extends Thread {

    public static Map<String, String> config = new HashMap<String, String>();
    public static ConcurrentHashMap<String, ExpiryValue> setDict = new ConcurrentHashMap<>();
    public static final String CONFIG_DBFILENAME = "dbfilename";
    public static final String CONFIG_DIR = "dir";
    public static final String MASTER_HOST = "MASTER_HOST";
    public static final String PORT = "port";
    public static final String MASTER_PORT = "MASTER_PORT";
    // master_replid and master_repl_offset
    public static final String MASTER_REPLID = "master_replid";
    public static final String MASTER_REPL_OFFSET = "master_repl_offset";

    private final Socket socket;
    private final ServerInfo serverInfo;

    public CommandHandle(Socket socket, ServerInfo serverInfo) {
        this.socket = socket;
        this.serverInfo = serverInfo;
    }

    @Override
    public void run() {
        try {
            handle(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void handle(Socket socket) throws IOException {
        try (OutputStream outputStream = socket.getOutputStream();
             DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        ) {
            byte[] array = new byte[1024];
            System.out.println("read begin " + System.currentTimeMillis());
            String line;
            while (!(line = readBuffLine(inputStream)).isEmpty()) {
                System.out.println("得到客户端请求：【" + line + "】");
                String response = null;
                List<String> tokens = parse(line);
                System.out.println("命令参数：" + tokens);
                // String command = tokens.get(0).toUpperCase();

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
                        response = getCommand(tokens);
                        break;
                    }
                    case "SET": {
                        response = setCommand(tokens, line);
                        break;
                    }
                    case "CONFIG": {
                        // the expected response to CONFIG GET dir is:
                        //*2\r\n$3\r\ndir\r\n$16\r\n/tmp/redis-files\r\n
                        response = configCommand(tokens, serverInfo);
                        break;

                    }
                    case "KEYS": {
                        response = keyCommand(tokens, response);
                        break;
                    }
                    case "INFO": {
                        response = infoCommand(tokens, response);
                        break;
                    }
                    case "REPLCONF": {
                        // todo 这里传了端口号的，后续可能会用上？ REPLCONF listening-port <PORT>
                        response = "+OK\r\n";
                        break;
                    }
                    case "PSYNC": {
                        // 建立从服务连接
                        response = "+FULLRESYNC " + config.get(MASTER_REPLID) + " 0\r\n";
                        break;
                    }
                    default: {
                        response = "$-1\r\n";
                        break;
                    }
                }
                System.out.println("response = " + response);

                // 建立从连接
                if (response != null) {
                    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                    if (response.startsWith("+FULLRESYNC")) {
                        serverInfo.getReplicas().add(outputStream);
                        sendEmpteyRDBFile(outputStream);
                    }
                }


            }
            System.out.println("read end " + System.currentTimeMillis());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private static void sendEmpteyRDBFile(OutputStream outputStream) throws IOException {
        String response;
        String base64 = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog==";
        byte[] bytes = Base64.getDecoder().decode(base64);
        response = String.format("$%d\r\n", bytes.length);
        outputStream.write(response.getBytes());
        outputStream.write(bytes);
    }

    private String infoCommand(List<String> tokens, String response) {
        if ("replication".equalsIgnoreCase(tokens.get(1))) {
            String role = serverInfo.getRole();
            String message = "role:" + role + "\r\n" +
                    MASTER_REPLID + ":" + serverInfo.getMasterReplid() + "\r\n" +
                    MASTER_REPL_OFFSET + ":" + serverInfo.getMasterReplOffset();
            response = buildResponse(message);
        }
        return response;
    }

    private static String keyCommand(List<String> tokens, String response) {
        ArrayList<String> keys = new ArrayList<>(setDict.keySet());
        if ("*".equals(tokens.get(1))) {
            response = "*" + keys.size() + "\r\n" + keys.stream().map(CommandHandle::buildResponse).collect(Collectors.joining());
        }
        return response;
    }

    private static String configCommand(List<String> tokens, ServerInfo serverInfo) {
        String response = null;
        if ("get".equalsIgnoreCase(tokens.get(1))) {
            if (CONFIG_DIR.equalsIgnoreCase(tokens.get(2))) {
                String dir = serverInfo.getDir();
                response = "*2\r\n" + buildResponse(CONFIG_DIR) + buildResponse(dir);
            } else if (CONFIG_DBFILENAME.equalsIgnoreCase(tokens.get(2))) {
                String dbfilename = serverInfo.getDbfilename();
                response = "*2\r\n" + buildResponse(CONFIG_DBFILENAME) + buildResponse(dbfilename);
            }
        } else {
            response = "$-1\r\n";
        }
        return response;
    }

    private String setCommand(List<String> tokens, String line) {
        String response;
        long expiry = Long.MAX_VALUE;
        if (tokens.size() > 3 && "px".equalsIgnoreCase(tokens.get(3))) {
            expiry = System.currentTimeMillis() + Long.parseLong(tokens.get(4));
        }
        setDict.put(tokens.get(1), new ExpiryValue(tokens.get(2), expiry));

        if (serverInfo.getRole().equalsIgnoreCase("master")
                && !serverInfo.getReplicas().isEmpty()) {
            System.out.println("Sending data to replicas");
            Set<OutputStream> replicas = serverInfo.getReplicas();
            replicas.forEach(replicaOutputStream -> {
                try {
                    replicaOutputStream.write(line.getBytes(StandardCharsets.UTF_8));
                    System.out.println("data sent to replicas");
                } catch (SocketException e) {
                    System.out.println("Error sending data to replica: " + e.getMessage());
                    replicas.remove(replicaOutputStream);
                    System.out.println("目前的slave服务数量：" + replicas.size());
                } catch (Exception e) {
                    System.out.println("Error sending data to replica: " + e.getMessage());
                    e.printStackTrace();

                }
            });
        }
        response = "+OK\r\n";
        return response;
    }

    private static String getCommand(List<String> tokens) {
        String response;
        ExpiryValue expiryValue = setDict.get(tokens.get(1));
        if (expiryValue != null && expiryValue.expiry > System.currentTimeMillis()) {
            String message = expiryValue.value;
            response = buildResponse(message);
        } else {
            setDict.remove(tokens.get(1));
            response = "$-1\r\n";
        }
        return response;
    }


    public static String buildResponse(String message) {
        String response = null;
        if (null != message) {
            response = String.format("$%d\r\n%s\r\n", message.length(), message);
        }
        return response;
    }

    // # REPLCONF listening-port <PORT>
    //*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n6380\r\n
    public static String buildRespArray(String... messages) {
        String response = "*" + messages.length + "\r\n";
        for (String msg : messages) {
            response += buildResponse(msg);
        }
        // response = String.format("$%d\r\n%s\r\n", message.length(), message);
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

    public static void initSetDict() throws IOException {
        String filename = config.get(CONFIG_DIR) + File.separator + config.get(CONFIG_DBFILENAME);

        FileInputStream fileInputStream = new FileInputStream(filename);
        System.out.println(fileInputStream);

        InputStream in = fileInputStream;
        byte[] header = new byte[9];
        in.read(header);
        System.out.println("header = " + new String(header, StandardCharsets.UTF_8));
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));

        // 用来接收读取的字节数组
        StringBuilder sb = new StringBuilder();
        sb.append(header);

        // 读取到的字节数组长度，为-1时表示没有数据
        int length = 0;
        int b;// 读取的字节

        // 循环取数据
        while ((b = in.read()) != -1) {
            switch (b) {
                case 0xFF: {
                    System.out.println();
                    System.out.println("EOF");
                    break;
                }
                case 0xFE: {
                    System.out.println();
                    System.out.println("SELECTBD");
                    b = in.read();
                    System.out.println(b);
                    break;
                }
                case 0xFD: {
                    System.out.println();
                    break;
                }
                case 0xFC: {
                    System.out.println();
                    System.out.println("EXPIRETIMEMS");
                    break;
                }
                case 0xFB: {
                    System.out.println();
                    System.out.println("RESIZEDB");
                    int tableSize = in.read();
                    int expirySize = in.read();
//                    int noexpSize = tableSize - expirySize;
                    long expTime = Long.MAX_VALUE;
                    for (int i = 0; i < tableSize; i++) {

                        int type = in.read();

                        if (0xFC == type) {
                            System.out.println("EXPIRETIMEMS");
                            byte[] info = new byte[8];
                            in.read(info);
                            expTime = bytesToLong(info);
                            System.out.println("read 8-byte integer:" + expTime);
                            type = in.read();
                        } else if (0xFD == type) {
                            // todo
                            System.out.println("EXPIRETIME");
                        }
                        int keyLength = in.read();
                        byte[] info = new byte[keyLength];
                        in.read(info);
                        String key = new String(info, StandardCharsets.UTF_8);
                        System.out.println(i + "- key=[" + key + "]");
                        int valueLength = in.read();
                        if (0 == type) {
                            info = new byte[valueLength];
                            in.read(info);
                            String value = new String(info, StandardCharsets.UTF_8);
                            setDict.put(key, new ExpiryValue(value, expTime));
                        }
                    }
                    break;
                }
                case 0xFA: {
                    System.out.println();
                    System.out.println("AUX auxiliary fields. Arbitrary key-vales settings");
                    break;
                }
                case 0xC0: {
                    b = in.read();
                    System.out.println("读到8 bit integer:" + b);
                    break;
                }
                case 0xC1: {
                    byte[] info = new byte[2];
                    in.read(info);
                    System.out.println("读到16 bit integer:" + bytesToShort(info));
                    break;
                }
                case 0xC2: {
                    byte[] info = new byte[4];
                    in.read(info);
                    System.out.println("读到32 bit integer:" + bytesToInt(info));
                    break;
                }
                default: {
                    System.out.println("未匹配 b = " + Integer.toHexString(b));
                    byte[] info = new byte[b];
                    in.read(info);
                    System.out.println("读到：[" + new String(info, StandardCharsets.UTF_8) + "]");
                    sb.append(b);
                    break;
                }
            }
        }
        // 关闭流
        in.close();
        System.out.println("全部字符串【" + sb + "】");

    }

    // 小端字节顺序
    // 将byte数组转换为int，假设数组为小端字节顺序
    public static int bytesToInt(byte[] bytes) {
        int num = 0;
        for (int i = 3; i > -1; i--) {
            num <<= 8;
            num |= (bytes[i] & 0xff);
        }
        return num;
    }

    // 将byte数组转换为short，假设数组为大端字节顺序
    public static short bytesToShort(byte[] bytes) {
        short num = 0;
        num |= (bytes[1] & 0xff);
        num <<= 8;
        num |= (bytes[0] & 0xff);
        return num;
    }

    public static long bytesToLong(byte[] bytes) {
        long num = 0;
        for (int i = 7; i > -1; i--) {
            System.out.print(String.format("%02X", bytes[i]) + ",(" + i + ")");
            num <<= 8;
            num |= (bytes[i] & 0xff);
        }
        return num;
    }

    public static int littleBytesToInt(byte[] bytes) {
        int num = 0;
        for (int i = 3; i > -1; i--) {
            num <<= 8;
            System.out.print(String.format("%02X", bytes[i]) + ",");
            num |= (bytes[i] & 0xff);
        }
        return num;
    }

}
