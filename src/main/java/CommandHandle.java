import demo.ExpiryValue;
import domain.ServerInfo;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static demo.Utils.log;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/23
 */
public class CommandHandle extends Thread {
    public static ConcurrentHashMap<String, ExpiryValue> setDict = new ConcurrentHashMap<>();
    public static final String CONFIG_DBFILENAME = "dbfilename";
    public static final String CONFIG_DIR = "dir";
    public static final String MASTER_HOST = "MASTER_HOST";
    public static final String PORT = "port";
    public static final String MASTER_PORT = "MASTER_PORT";
    // master_replid and master_repl_offset
    public static final String MASTER_REPLID = "master_replid";
    public static final String MASTER_REPL_OFFSET = "master_repl_offset";


    private final AtomicInteger acknowledgedReplicaCount = new AtomicInteger();
    private final AtomicInteger sendReplicaCount = new AtomicInteger();
    private final Socket socket;
    private final ServerInfo serverInfo;
    private  Integer socketId;

    public CommandHandle(Socket socket, ServerInfo serverInfo, Integer socketId) {
        this.socket = socket;
        this.serverInfo = serverInfo;
        this.socketId = socketId;
    }
    public CommandHandle(Socket socket, ServerInfo serverInfo) {
        this.socket = socket;
        this.serverInfo = serverInfo;
    }

    @Override
    public void run() {
        try {
            handle(socket);
        } catch (IOException e) {
            System.out.println("End CommandHandle with exp: " + e.getMessage());
            ;
        }
    }

    void handle(Socket socket) throws IOException {
        try (OutputStream outputStream = socket.getOutputStream();
             DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        ) {
            log("【socketId=" + socketId+"】",socketId+ "- read begin " + System.currentTimeMillis());
            Object readMsg;
            String response = null;
            log("[parseInput] call - handle");
            while (null != (readMsg = ProtocolParser.parseInput(inputStream, serverInfo))) {
                log("【socketId=" + socketId+"】","得到客户端请求【", readMsg.toString(), "】");
                if (readMsg instanceof String) {
                    response = processCommand(Collections.singletonList((String) readMsg));
                } else if (readMsg instanceof List) {
                    List<String> array = (List<String>) readMsg;
                    response = processCommand(array);
                } else {
                    log("【socketId=" + socketId+"】", " 不知说了啥【", readMsg.toString(), "】");
                }
                log("【socketId=" + socketId+"】", " 返回响应【", response, "】");

                if (response != null) {
                    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                    // 建立从连接
                    if (response.startsWith("+FULLRESYNC")) {
                        log("【socketId=" + socketId+"】","建立了主从连接，向从服务器发送空的RDB文件");
                        serverInfo.getReplicas().add(socket);

                        sendEmpteyRDBFile(outputStream);
                    }
                }
                log("[parseInput] call - handle");
            }
            log("【socketId=" + socketId+"】","read end " + System.currentTimeMillis());
        } catch (IOException e) {
            log("【socketId=" + socketId+"】","IOException: " + e.getMessage());
            e.printStackTrace();
        }

    }


    public String processCommand(List<String> tokens) throws IOException {

        String response = null;
        log("【socketId=" + socketId+"】","命令参数：" + tokens);
        // String command = tokens.get(0).toUpperCase();

        switch (tokens.get(0).toUpperCase()) {
            case "PING": {
                response = "+PONG\r\n";
                break;
            }
            case "ECHO": {
                String message = tokens.size() > 1 ? tokens.get(1) : "";
                response = ProtocolParser.buildSimpleString(message);
                break;
            }
            case "GET": {
                response = getCommand(tokens);
                break;
            }
            case "SET": {
                response = setCommand(tokens);
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
                String message = tokens.size() > 1 ? tokens.get(1) : "";
                // receiving the REPLCONF GETACK * command and responding with REPLCONF ACK 0
                if ("GETACK".equalsIgnoreCase(message)) {
                    response = ProtocolParser.buildRespArray("REPLCONF", "ACK", String.valueOf(serverInfo.getSlaveOffset() - 37));
                }

                // [REPLCONF, ACK, 31] -- 收到从服务器的ack 响应，但是为什么是在这？
                if (tokens.size() == 3 && "ACK".equalsIgnoreCase(tokens.get(1))) {
                    acknowledgedReplicaCount.incrementAndGet();
                }

                break;
            }
            case "PSYNC": {
                // 建立从服务连接
                response = "+FULLRESYNC " + serverInfo.getMasterReplid() + " 0\r\n";
                break;
            }
            case "WAIT": {
                response = waitCommand(tokens);
                break;
            }
            default: {
                response = "$-1\r\n";
                break;
            }
        }

        log("【socketId=" + socketId+"】", "response = ", response);
        return response;
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
            response = ProtocolParser.buildResponse(message);
        }
        return response;
    }

    private static String keyCommand(List<String> tokens, String response) {
        ArrayList<String> keys = new ArrayList<>(setDict.keySet());
        if ("*".equals(tokens.get(1))) {
            response = "*" + keys.size() + "\r\n" + keys.stream().map(ProtocolParser::buildResponse).collect(Collectors.joining());
        }
        return response;
    }

    private static String configCommand(List<String> tokens, ServerInfo serverInfo) {
        String response = null;
        if ("get".equalsIgnoreCase(tokens.get(1))) {
            if (CONFIG_DIR.equalsIgnoreCase(tokens.get(2))) {
                String dir = serverInfo.getDir();
                response = ProtocolParser.buildRespArray(CONFIG_DIR, dir);
            } else if (CONFIG_DBFILENAME.equalsIgnoreCase(tokens.get(2))) {
                String dbfilename = serverInfo.getDbfilename();
                response = ProtocolParser.buildRespArray(CONFIG_DBFILENAME, dbfilename);
            }
        } else {
            response = "$-1\r\n";
        }
        return response;
    }

    private String setCommand(List<String> tokens) {
        String response;
        long expiry = Long.MAX_VALUE;
        if (tokens.size() > 3 && "px".equalsIgnoreCase(tokens.get(3))) {
            expiry = System.currentTimeMillis() + Long.parseLong(tokens.get(4));
        }
        setDict.put(tokens.get(1), new ExpiryValue(tokens.get(2), expiry));

        if (serverInfo.getRole().equalsIgnoreCase("master")
                && !serverInfo.getReplicas().isEmpty()) {
            log("【socketId=" + socketId+"】","Sending data to replicas -> " + tokens);
            Set<Socket> replicas = serverInfo.getReplicas();
            replicas.forEach(socket -> {
                try {
                    OutputStream replicaOutputStream = socket.getOutputStream();
                    replicaOutputStream.write(ProtocolParser.buildArray(tokens).getBytes(StandardCharsets.UTF_8));
                    log("【socketId=" + socketId+"】","data sent to replicas");
                } catch (SocketException e) {
                    log("【socketId=" + socketId+"】","Error sending data to replica: " + e.getMessage());
                    replicas.remove(socket);
                    log("【socketId=" + socketId+"】","目前的slave服务数量：" + replicas.size());
                } catch (Exception e) {
                    log("【socketId=" + socketId+"】","Error sending data to replica: " + e.getMessage());
                    e.printStackTrace();
                    replicas.remove(socket);
                    log("【socketId=" + socketId+"】","目前的slave服务数量：" + replicas.size());
                }
            });
        }
        log("【socketId=" + socketId+"】","setDict finished, 目前的 dict 数量 size = " + setDict.size());
        response = "+OK\r\n";
        return response;
    }

    private String waitCommand(List<String> tokens) {
        if (tokens.size() > 2 && serverInfo.getRole().equalsIgnoreCase("master")) {
            long timeOutMillis = Long.parseLong(tokens.get(2));
            log("【socketId=" + socketId+"】","waitCommand timeOutMillis = " + timeOutMillis);
        }
        if (tokens.size() > 2 && serverInfo.getRole().equalsIgnoreCase("master")
                && !serverInfo.getReplicas().isEmpty()) {
            long timeOutMillis = Long.parseLong(tokens.get(2));
            log("【socketId=" + socketId+"】","waitCommand [have slaves] timeOutMillis = " + timeOutMillis);

            Set<Socket> replicas = serverInfo.getReplicas();
            // Map each replica to a CompletableFuture representing async task
            Stream<CompletableFuture<Void>> futures = replicas.stream()
                    .map(replica -> CompletableFuture.runAsync(() -> getAcknowledgement(replica), Connection.executor));
            try {
                if (timeOutMillis > 0) {
                    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(timeOutMillis, TimeUnit.MILLISECONDS);

                    log("【socketId=" + socketId+"】","waitCommand [have slaves] wait response");
                }
            } catch (Exception e) {
                log("【socketId=" + socketId+"】","waitCommand 等待slave wait 响应异常:" + e.getMessage());
                e.printStackTrace();
            }
        }
        // Get count of acknowledged replicas and reset counter
        int ackCount = acknowledgedReplicaCount.intValue();
        acknowledgedReplicaCount.set(0);
        log("【socketId=" + socketId+"】","waitCommand [set counter] " + ackCount);
        return String.format(":%d\r\n", ackCount);
    }

    private void getAcknowledgement(Socket socket) {
        try {
            synchronized (socket){

                int sendCount = sendReplicaCount.incrementAndGet();
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                OutputStream outputStream = socket.getOutputStream();
                String ackCommand = ProtocolParser.buildRespArray("REPLCONF", "GETACK", "*");
                outputStream.write(ackCommand.getBytes());
                log("【socketId=" + socketId+"】【sendCount=",sendCount + "】 Ack command sent:【", ackCommand, "】");

                log("[parseInput] call - getAcknowledgement");
                String ackResponse = ProtocolParser.parseInput(inputStream, null).toString();
                log("【socketId=" + socketId+"】【sendCount=",sendCount + "】 Ack esponse received:【", ackResponse, "】");
                acknowledgedReplicaCount.incrementAndGet();
            }


        } catch (IOException e) {
            System.out.printf("Acknowledgement failed: %s\n", e.getMessage());
        }
    }


    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {
        long timeOutMillis = 500L;
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
            try {
                int num = 1000;
                System.out.println(Thread.currentThread().getName() + " sleep start " + num);
                Thread.sleep(num);
                System.out.println(Thread.currentThread().getName() + " sleep end   " + num);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });//                .get(timeOutMillis, TimeUnit.MILLISECONDS)

        CompletableFuture<Void> voidCompletableFuture1 = CompletableFuture.runAsync(() -> {
            try {
                int num = 200;
                System.out.println(Thread.currentThread().getName() + " sleep start " + num);
                Thread.sleep(num);
                System.out.println(Thread.currentThread().getName() + " sleep end   " + num);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        //                .get(timeOutMillis, TimeUnit.MILLISECONDS)
//        try {
//
        CompletableFuture.allOf(voidCompletableFuture, voidCompletableFuture1).get(timeOutMillis, TimeUnit.MILLISECONDS);
//        } catch (TimeoutException e) {
//            System.out.println("time out:" + e.getMessage());
//        }
        Stream<CompletableFuture<Void>> completableFutureStream = list.stream().map(index -> CompletableFuture.runAsync(() -> {
            try {
                int num = index * 200;
                System.out.println(Thread.currentThread().getName() + " B sleep start " + num);
                Thread.sleep(num);
                System.out.println(Thread.currentThread().getName() + " B sleep end   " + num);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

//        CompletableFuture[] array = completableFutureStream.toArray(CompletableFuture[]::new);
        try {

            CompletableFuture.allOf(completableFutureStream.toArray(CompletableFuture[]::new)).get(timeOutMillis, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            System.out.println("2 time out:" + e.getMessage());
        }
        //completeOnTimeout(null,timeOutMillis, TimeUnit.MILLISECONDS)).collect(Collectors.toList());


        System.out.println("main end");
        Thread.sleep(4000);
    }


    private static String getCommand(List<String> tokens) {
        String response;
        ExpiryValue expiryValue = setDict.get(tokens.get(1));
        if (expiryValue != null && expiryValue.expiry > System.currentTimeMillis()) {
            String message = expiryValue.value;
            response = ProtocolParser.buildSimpleString(message);
        } else {
            setDict.remove(tokens.get(1));
            response = "$-1\r\n";
        }
        return response;
    }


    List<String> parse(String param) {
        List<String> args = new ArrayList<>();
        if (param.startsWith("*")) {
            String[] array = param.split("\r\n");
            int size = Integer.valueOf(array[0].substring(1));
            System.out.println("parse " + size + " " + Arrays.asList(array));
            for (int i = 2; i < array.length; i += 2) {
                args.add(array[i]);
            }
        } else {
            args.add(param);
        }
        return args;
    }

    public static void initSetDict(String filename) throws IOException {

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
