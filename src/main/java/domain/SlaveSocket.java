package domain;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/30
 */
public class SlaveSocket {
    private Socket socket;
    private Integer id;
    OutputStream outputStream;
    DataInputStream inputStream;
    private Boolean readFlag = true;

    public SlaveSocket(Socket socket, Integer id) {
        this.socket = socket;
        this.id = id;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public DataInputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(DataInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getReadFlag() {
        return readFlag;
    }

    public void setReadFlag(Boolean readFlag) {
        this.readFlag = readFlag;
    }
}
