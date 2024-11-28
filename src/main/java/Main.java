import domain.ServerInfo;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");
        ServerInfo serverInfo = initialize(args);

        // load database
        loadDatabase(serverInfo);

        if ("master".equals(serverInfo.getRole())) {
            System.out.println("master set up");
            Connection connection = new Connection(serverInfo);
            connection.initiateConnections();
        } else {
            System.out.println("slave set up");
            Slave.initiateSlaveConnection(serverInfo);
        }
    }

    private static void loadDatabase(ServerInfo serverInfo) {
        if (serverInfo.getDir() != null && serverInfo.getDbfilename() != null) {
            try {
                String fileName = serverInfo.getDir() + File.separator + serverInfo.getDbfilename();
                CommandHandle.initSetDict(fileName);
            } catch (Exception e) {
                System.out.println("load database fail. Exception: " + e.getMessage());
            }
        }
    }

    private static ServerInfo initialize(String[] args) {
        ServerInfo serverInfo = ServerInfo.getInstance();

        for (int i = 0; i < args.length; i++) {
            //./your_program.sh  --dir /tmp/redis-files --dbfilename dump.rdb
            String param = args[i];
            if ("--dir".equalsIgnoreCase(param)) {
                serverInfo.setDir(args[++i]);
            } else if ("--dbfilename".equalsIgnoreCase(param)) {
                serverInfo.setDbfilename(args[++i]);
            } else if ("--port".equalsIgnoreCase(param)) {
                serverInfo.setPort(Integer.parseInt(args[++i]));
            } else if ("--replicaof".equalsIgnoreCase(param)) {// --replicaof "localhost 6379"
                serverInfo.setRole("slave");
                String[] master = args[++i].split(" ");// <MASTER_HOST> <MASTER_PORT>
                serverInfo.setMasterHost(master[0]);
                serverInfo.setMasterPort(master[1]);
            }
        }
        // master_replid and master_repl_offset
        serverInfo.setMasterReplid("8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb");
        serverInfo.setMasterReplOffset(0L);
        return serverInfo;
    }
}
