package com.hubble.scan.utls;

public class ServersInfo {
    private static final int countServers = 15;
    private static final int generalPort = 25565;
    private static Server[] servers = new Server[countServers];
    static {
        servers[0] = new Server("###", "###", generalPort);
    }

    public static Server[] getServers() {
        return servers;
    }

    public static int getCountServers() {
        return countServers;
    }

    public static class Server {
        private String name;
        private String address;
        private int port;

        public Server(String name, String address, int port) {
            this.name = name;
            this.address = address;
            this.port = port;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }
    }
}
