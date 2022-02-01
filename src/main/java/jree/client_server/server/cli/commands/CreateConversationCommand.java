package jree.client_server.server.cli.commands;


import jree.client_server.server.command.annoations.Command;
import jree.client_server.server.command.annoations.CommandOption;

@Command("create_conversation")
public class CreateConversationCommand {

    @CommandOption(longName = "id", description = "conversation id")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
