import domain.ServerInfo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static demo.Utils.log;
import static demo.Utils.readBuffLine;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/27
 */
public class ProtocolParser {
    private ProtocolParser() {
    }


    /**
     * @param inputStream
     * @param serverInfo  记录从master 传过来多少命令
     * @return
     */
    public static Object parseInput(DataInputStream inputStream, ServerInfo serverInfo) {
        log("[parseInput] is called");
        try {
            char b = (char) inputStream.readByte();
            log("parseInput first byte:" + b);
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
                    log("parseInput first byte:" + Integer.toHexString(b) + "--unexcepted ");
                    log("遇到了异常的输入，把剩下的都打印出来：【", readBuffLine(inputStream), "】");
                }
            }
        } catch (IOException e) {
            System.out.println("读取输入error: " + e.getMessage());
            e.printStackTrace();
        }
        log("ERROR: parseInput 读取失败");
        return null;
    }

    // *<number-of-elements>\r\n<element-1>...<element-n>
    private static List<String> parseArray(DataInputStream inputStream, ServerInfo serverInfo) throws IOException {
        int size = parseDigits(inputStream);
        if (serverInfo != null) {
            serverInfo.addOffset(String.valueOf(size).length() + 2);
        }
        System.out.println("array size: " + size);
        List<String> array = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Object obj = parseInput(inputStream, serverInfo);
            if (obj instanceof String) {
                array.add((String) obj);
            } else {
                log("ERROR: unhandled type " + obj);
                throw new RuntimeException("unhandled type " + obj);
            }
        }
        //log("array【", array.toString(), "】");
        return array;
    }

    // +OK\r\n
    private static String parseSimpleString(DataInputStream inputStream, ServerInfo serverInfo) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for (char ch = (char) inputStream.readByte(); ch != '\r'; ch = (char) inputStream.readByte()) {
            stringBuilder.append(ch);
        }

        // Skip `\n`
        inputStream.readByte();

        String line = stringBuilder.toString();
        if (serverInfo != null) {
            serverInfo.addOffset(line.length() + 2);
        }
        return line;
    }

    // $<length>\r\n<data>\r\n
    private static String parseBulkString(DataInputStream inputStream, ServerInfo serverInfo) throws IOException {
        int length = parseDigits(inputStream);
        //System.out.println("bulk string length: " + length);
        byte[] array = new byte[length];
        inputStream.read(array);
        String s = new String(array, 0, length);
        // Skip `\r\n`
        inputStream.readByte();
        inputStream.readByte();
        if (serverInfo != null) {
            serverInfo.addOffset(String.valueOf(length).length() + 2 + length + 2);
        }
        return s;
    }

    private static int parseDigits(DataInputStream inputStream) throws IOException {
        StringBuilder digits = new StringBuilder();
        char c;
        while ((c = (char) inputStream.readByte()) != '\r') {
            digits.append(c);
        }

        byte b = inputStream.readByte();// skip '\n' after '\r'
        log("parseDigits [" + digits, "], end with ", Integer.toHexString(b));
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

    public static String buildInteger(int num) {

        return String.format(":%d\r\n", num);
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

    // "*3\r\n$8\r\nREPLCONF\r\n$6\r\nGETACK\r\n$1\r\n*\r\n"

    public static void main(String[] args) throws IOException {
        char plus = '+';
        // System.out.println(Integer.toHexString(plus));// 2b

        plus = '\r';
        // System.out.println(Integer.toHexString(plus));//d
        plus = '\n';
        // System.out.println(Integer.toHexString(plus));//a
        String ipStr = "+FULLRESYNC 71dc 0\r\n" +
                "$8\r\n" +
                "REDIS0011\r\n";
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(ipStr.getBytes()));
//        String s = (String) parseInput(dataInputStream);
//        System.out.println("1. parseIput: " + s);
//
//        s = (String) parseInput(dataInputStream);
//        System.out.println("2. parseIput: " + s);

        ipStr = "*3\r\n$8\r\nREPLCONF\r\n$6\r\nGETACK\r\n$1\r\n*\r\n";
        ipStr = "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$2\r\n99\r\n";
        dataInputStream = new DataInputStream(new ByteArrayInputStream(ipStr.getBytes()));
        System.out.println("找错误：" + parseInput(dataInputStream, null));

    }
}
