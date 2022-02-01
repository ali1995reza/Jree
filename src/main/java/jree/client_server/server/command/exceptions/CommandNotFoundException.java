package jree.client_server.server.command.exceptions;

public class CommandNotFoundException extends CommandException {

    public static CommandNotFoundException of(String command) {
        return new CommandNotFoundException("command ["+command+"] not found");
    }

    public CommandNotFoundException(Throwable e) {
        super(e);
    }

    public CommandNotFoundException(String e) {
        super(e);
    }
}
