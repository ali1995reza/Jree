package jree.client_server.server.command.exceptions;

public class CommandParseException extends CommandException {

    public CommandParseException(Throwable e) {
        super(e);
    }

    public CommandParseException(String e) {
        super(e);
    }
}
