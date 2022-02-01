package jree.client_server.server.cli.commands;

import jree.client_server.server.command.annoations.Command;
import jree.client_server.server.command.annoations.CommandOption;

@Command("send")
public class SendMessageCommand {

    @CommandOption(longName = "message", shortName = "m", description = "message body")
    private String message;
    @CommandOption(longName = "recipient", shortName = "r", description = "recipient , conversation co_[conversation-id], cl_[client-id], usr_[username]")
    private String recipient;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}
