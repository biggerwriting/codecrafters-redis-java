package domain;

import java.io.OutputStream;
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

    public void addOffset(long offset) {
        this.masterReplOffset += offset;
    }

    private volatile Set<OutputStream> replicas = new HashSet<>();

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

    public Set<OutputStream> getReplicas() {
        return replicas;
    }

    public void setReplicas(Set<OutputStream> replicas) {
        this.replicas = replicas;
    }
}
