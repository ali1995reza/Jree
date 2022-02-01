package jree.client_server.server.command.standard;

import jree.client_server.server.command.annoations.Command;
import jree.client_server.server.command.annoations.CommandOption;

@Command("exit")
public class ExitCommand {

    @CommandOption(shortName = "c", longName = "code", required = false, description = "exit code")
    private int code = 0; //default code is 0

    public int getCode() {
        return code;
    }
}
