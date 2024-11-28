import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/27
 */
public class ProtocolParser {
    private ProtocolParser() {
    }


    public static Object parseInput(DataInputStream inputStream) {
        try {
            char b = (char) inputStream.readByte();
            switch (b) {
                case '+': {
                    return parseSimpleString(inputStream);
                }
                case '$': {
                    return parseBulkString(inputStream);
                }
                case '*':{
                    return parseArray(inputStream);
                }
                default:{

                    System.out.println("整不会了"+b);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // *<number-of-elements>\r\n<element-1>...<element-n>
    private static List<String> parseArray(DataInputStream inputStream) throws IOException {
        int size = Integer.parseInt(inputStream.readLine());
        System.out.println("array size: "+size);
        List<String> array = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Object obj = parseInput(inputStream);
            if(obj instanceof String){
                array.add((String) obj);
            }else {
                throw new RuntimeException("unhandled type "+ obj.getClass());
            }
        }
        return array;
    }

    // +OK\r\n
    private static String parseSimpleString(DataInputStream inputStream) throws IOException {
        String line = inputStream.readLine();
        return line;
    }
    // $<length>\r\n<data>\r\n
    private static String parseBulkString(DataInputStream inputStream) throws IOException {

        int length = Integer.parseInt(inputStream.readLine());
        System.out.println("bulk string length: "+length);
//        return "";

        byte[] array = new byte[length];
        int read = inputStream.read(array);
        System.out.println("read array returns:" +read);
        return new String(array,0,length);
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
    public static void main(String[] args) throws IOException {
        char plus = '+';
        System.out.println(Integer.toHexString(plus));

        plus = '\r';
        System.out.println(Integer.toHexString(plus));
        plus = '\n';
        System.out.println(Integer.toHexString(plus));
        String ipStr = "+FULLRESYNC 71dc 0\r\n" +
                "$8\r\n" +
                "REDIS0011\r\n";
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(ipStr.getBytes()));
        String s = (String) parseInput(dataInputStream);
        System.out.println("1. parseIput: " + s);

        s = (String) parseInput(dataInputStream);
        System.out.println("2. parseIput: " + s);
        int b;
//        while ((b = dataInputStream.read()) != -1) {
//            System.out.println("read byte: " + (char) b);
//        }
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
        return  String.format("$%d\r\n%s\r\n", token.length(), token);
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

}
