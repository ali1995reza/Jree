package jree.client_server.server.command.parser;

import jree.client_server.server.command.exceptions.CommandException;

public interface CommandParser {

    Object parse(String command) throws CommandException;

    <T> CommandParser setCommandHandler(Class<T> type, CommandHandler<T> handler);

}
