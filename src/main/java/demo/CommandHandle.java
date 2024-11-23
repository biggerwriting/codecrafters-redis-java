package demo;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/23
 */
public class CommandHandle {
    public static ConcurrentHashMap<String, ExpiryValue> setDict = new ConcurrentHashMap<>();

    public void handle(Socket clientSocket) throws IOException {
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
                if (null != bufferedReader.readLine()) {
                    long expiry = Long.MAX_VALUE;
                    if ("px".equalsIgnoreCase(bufferedReader.readLine())) {
                        bufferedReader.readLine();
                        expiry = Long.parseLong(bufferedReader.readLine());
                    }
                    setDict.put(key, new ExpiryValue(value, expiry));
                } else {

                    setDict.put(key, new ExpiryValue(value));
                }
                outputStream.write("+OK\r\n".getBytes());
            } else if ("get".equalsIgnoreCase(line)) {
                System.out.println("get command");
                bufferedReader.readLine();
                String key = bufferedReader.readLine();
                ExpiryValue expiryValue = setDict.get(key);

                if (null != expiryValue) {
                    String message = expiryValue.value;
                    if (expiryValue.expiry > System.currentTimeMillis()) {
                        outputStream.write(String.format("$%d\r\n%s\r\n", message.length(), message).getBytes());
                    } else {
                        setDict.remove(key);
                        outputStream.write("$-1\r\n".getBytes());
                    }
                } else {
                    outputStream.write("$-1\r\n".getBytes());
                }
            } else {
                //System.out.println("   *** unkonw command:"+line);
            }

        }
    }
}
