package jree.client_server.server.command.standard;

import jree.client_server.server.command.annoations.Command;

@Command("help")
public class HelpCommand {

    private String help;

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }
}
