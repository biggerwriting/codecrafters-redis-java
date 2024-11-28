import domain.ServerInfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static demo.Utils.log;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/27
 */
public class ProtocolParser {
    private ProtocolParser() {
    }


    public static Object parseInput(DataInputStream inputStream, ServerInfo serverInfo) {
        try {
            char b = (char) inputStream.readByte();
            if (serverInfo != null) {
                serverInfo.addOffset(1);
            }
            switch (b) {
                case '+': {
                    return parseSimpleString(inputStream, serverInfo);
                }
                case '$': {
                    return parseBulkString(inputStream, serverInfo);
                }
                case '*': {
                    return parseArray(inputStream, serverInfo);
                }
                default: {
                    System.out.println("整不会了" + Integer.toHexString(b));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // *<number-of-elements>\r\n<element-1>...<element-n>
    private static List<String> parseArray(DataInputStream inputStream, ServerInfo serverInfo) throws IOException {
        String lengthStr = inputStream.readLine();
        int size = Integer.parseInt(lengthStr);
        serverInfo.addOffset(lengthStr.length() + 2);

        System.out.println("array size: " + size);
        List<String> array = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Object obj = parseInput(inputStream, serverInfo);
            if (obj instanceof String) {
                array.add((String) obj);
            } else {
                throw new RuntimeException("unhandled type " + obj);
            }
        }
        log("array【", array.toString(), "】");
        return array;
    }

    // +OK\r\n
    private static String parseSimpleString(DataInputStream inputStream, ServerInfo serverInfo) throws IOException {
        String line = inputStream.readLine();
        if (serverInfo != null) {
            serverInfo.addOffset(line.length() + 2);
        }
        log("simple string【", line, "】");
        return line;
    }

    // $<length>\r\n<data>\r\n
    private static String parseBulkString(DataInputStream inputStream, ServerInfo serverInfo) throws IOException {
        String lengthStr = inputStream.readLine();
        int length = Integer.parseInt(lengthStr);
        System.out.println("bulk string length: " + length);
        byte[] array = new byte[length];
        inputStream.read(array);
        String s = new String(array, 0, length);
        log("bulk string【", s, "】");
        inputStream.readLine();
        if (serverInfo != null) {
            serverInfo.addOffset(lengthStr.length() + 2 + length + 2);
        }
        return s;
    }

    private static int parseDigits(DataInputStream inputStream) throws IOException {
        StringBuilder digits = new StringBuilder();
        char c;
        while ((c = (char) inputStream.readByte()) != '\r') {
            digits.append(c);
        }
        inputStream.readByte(); // skip '\n' after '\r'
        return Integer.parseInt(digits.toString());
    }


    public static String buildArray(List<String> array) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(array.size()).append("\r\n");
        for (int i = 0; i < array.size(); i++) {
            String token = array.get(i);
            sb.append(buildSimpleString(token));
        }
        return sb.toString();
    }

    public static String buildSimpleString(String token) {
        return String.format("$%d\r\n%s\r\n", token.length(), token);
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

    public static void readFile(DataInputStream inputStream) throws IOException {
        // $8\r\nREDIS001
        char b = (char) inputStream.readByte();
        if ('$' == b) {
            int length = Integer.parseInt(inputStream.readLine());
            System.out.println("read file, size: " + length);
            byte[] array = new byte[length];
            inputStream.read(array);
            // todo 这个array 没人用
        } else {
            throw new RuntimeException("未识别的文件字符：" + Integer.toHexString(b));
        }
    }
}
