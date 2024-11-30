package domain;

import java.net.Socket;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/30
 */
public class SlaveSocket {
    private Socket socket;
    private Integer id;

    private Boolean readFlag = true;

    public SlaveSocket(Socket socket, Integer id) {
        this.socket = socket;
        this.id = id;
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
