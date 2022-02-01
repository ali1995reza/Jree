package jree.client_server.server.cli;

import jree.client_server.server.cli.commands.*;
import jree.client_server.server.command.exceptions.CommandException;
import jree.client_server.server.command.parser.CommandParser;
import jree.client_server.server.command.parser.builder.CommandParserBuilder;
import jree.client_server.server.command.standard.HelpCommand;

public class ServerCommandLine {

    private final CommandParser commandParser;

    public ServerCommandLine() {
        this.commandParser = CommandParserBuilder
                .builder()
                .add(HelpCommand.class)
                .add(ConnectCommand.class)
                .add(CreateConversationCommand.class)
                .add(JoinConversationCommand.class)
                .add(LeaveConversationCommand.class)
                .add(SendMessageCommand.class)
                .build();
    }

    public Object parse(String str) {
        try {
            return commandParser.parse(str);
        } catch (CommandException e) {
            throw new IllegalStateException(e);
        }
    }
}
