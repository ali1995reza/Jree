package jree.client_server.server;

import java.util.Scanner;

public class RunJreeServer {

    public static void main(String[] args) {
        int port = Input.getInt("Enter Server Port");
        JreeServer server = new JreeServer(port);
        server.start();
    }
}
