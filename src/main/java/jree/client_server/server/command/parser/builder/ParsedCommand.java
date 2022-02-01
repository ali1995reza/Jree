package jree.client_server.server.command.parser.builder;

import org.apache.commons.cli.Options;

import java.util.List;

public class ParsedCommand {

    private final String commandName;
    private final Class type;
    private final List<OptionFieldPair> fields;
    private final Options options;

    public ParsedCommand(String commandName, Class type, List<OptionFieldPair> fields) {
        this.commandName = commandName;
        this.type = type;
        this.fields = fields;
        options = new Options();
        for(OptionFieldPair f: fields) {
            options.addOption(f.getOption());
        }
    }

    public String getCommandName() {
        return commandName;
    }

    public Class getType() {
        return type;
    }

    public List<OptionFieldPair> getFields() {
        return fields;
    }

    public Options getOptions() {
        return options;
    }
}
