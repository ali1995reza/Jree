package jree.client_server.server.command.exceptions;

public class EmptyCommandException extends CommandException {

    public EmptyCommandException(Throwable e) {
        super(e);
    }

    public EmptyCommandException(String e) {
        super(e);
    }

    public EmptyCommandException(){
        super("empty command");
    }
}
