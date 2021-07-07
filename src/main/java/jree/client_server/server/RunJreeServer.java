package jree.client_server.server;

public class RunJreeServer {

    public static void main(String[] args) {
        JreeServer server = new JreeServer(5566);
        server.start();
    }
}
