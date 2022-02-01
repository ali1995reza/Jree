package jree.client_server.server.cli.commands;

import jree.client_server.server.command.annoations.Command;
import jree.client_server.server.command.annoations.CommandOption;

@Command("connect")
public class ConnectCommand {

    @CommandOption(longName = "token", shortName = "t", description = "authentication token")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
