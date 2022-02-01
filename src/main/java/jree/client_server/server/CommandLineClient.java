package jree.client_server.server;

import jree.client_server.client.io.JreeClient;

import java.net.URI;

public class CommandLineClient {


    public static void main(String[] args) {
        int port = Input.getInt("Server Port");
        JreeClient client = new JreeClient();
        client.setMessageListener(System.out::println);
        client.connect(URI.create("ws://localhost:" + port + "/chat"));
        String token = Input.getString("Token");
        client.executeCommand("connect -t \""+token+"\"");

        while (true) {
            client.executeCommand(Input.getString(" >> "));
        }
    }

}
