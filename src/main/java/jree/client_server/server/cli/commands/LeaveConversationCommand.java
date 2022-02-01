package jree.client_server.server.cli.commands;

import jree.client_server.server.command.annoations.Command;
import jree.client_server.server.command.annoations.CommandOption;

@Command("leave")
public class LeaveConversationCommand {

    @CommandOption(longName = "conversation", shortName = "c", description = "conversation to leave")
    private Long conversation;

    public Long getConversation() {
        return conversation;
    }

    public void setConversation(Long conversation) {
        this.conversation = conversation;
    }
}
