package domain;

import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/26
 */
public class ServerInfo {
    public static final ServerInfo singleton = new ServerInfo();

    private ServerInfo() {
    }

    public static ServerInfo getInstance() {
        return singleton;
    }

    private String role = "master";
    private int port = 6379;
    private String dir;
    private String dbfilename;
    private String masterHost;
    private String masterPort;
    private String masterReplid;
    private Long masterReplOffset = 0L;
    private Long slaveOffset = 0L;
    private volatile Set<Socket> replicas = new HashSet<>();
    private volatile Set<SlaveSocket> slaveSockets = new HashSet<>();

    public Set<SlaveSocket> getSlaveSockets() {
        return slaveSockets;
    }

    public void setSlaveSockets(Set<SlaveSocket> slaveSockets) {
        this.slaveSockets = slaveSockets;
    }

    public void addOffset(long offset) {
        this.slaveOffset += offset;
    }

    public Long getSlaveOffset() {
        return slaveOffset;
    }

    public void setSlaveOffset(Long slaveOffset) {
        this.slaveOffset = slaveOffset;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getDbfilename() {
        return dbfilename;
    }

    public void setDbfilename(String dbfilename) {
        this.dbfilename = dbfilename;
    }

    public String getMasterHost() {
        return masterHost;
    }

    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    public String getMasterPort() {
        return masterPort;
    }

    public void setMasterPort(String masterPort) {
        this.masterPort = masterPort;
    }

    public String getMasterReplid() {
        return masterReplid;
    }

    public void setMasterReplid(String masterReplid) {
        this.masterReplid = masterReplid;
    }

    public Long getMasterReplOffset() {
        return masterReplOffset;
    }

    public void setMasterReplOffset(Long masterReplOffset) {
        this.masterReplOffset = masterReplOffset;
    }

    public Set<Socket> getReplicas() {
        return replicas;
    }

    public void setReplicas(Set<Socket> replicas) {
        this.replicas = replicas;
    }
}
