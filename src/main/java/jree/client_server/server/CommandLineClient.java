package jree.client_server.server;

import jree.client_server.client.io.JreeClient;

import java.net.URI;
import java.util.Scanner;

public class CommandLineClient {



    public static void main(String[] args) {
        int port = Input.getInt("Server Port");
        JreeClient client = new JreeClient();
        client.setMessageListener(System.out::println);
        client.connect(URI.create("ws://localhost:"+port+"/chat"));
        String token = Input.getString("Token");
        client.auth(token);
        while (true) {
            try {
                String command = Input.getString("Command");
                String[] commandParts = command.split("\\s+");
                if (commandParts[0].equalsIgnoreCase("message")) {
                    client.sendMessage(commandParts[1], commandParts[2]);
                } else {
                    System.err.println("Command not found !");
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

}
