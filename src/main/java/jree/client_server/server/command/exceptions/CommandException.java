package jree.client_server.server.command.exceptions;

public class CommandException extends Exception {

    public CommandException(Throwable e) {
        super(e);
    }

    public CommandException(String e) {
        super(e);
    }

}
