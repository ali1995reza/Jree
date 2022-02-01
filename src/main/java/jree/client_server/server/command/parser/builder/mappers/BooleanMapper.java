package jree.client_server.server.command.parser.builder.mappers;

import java.util.function.Function;

public class BooleanMapper implements Function<String, Boolean> {

    private final static BooleanMapper INSTANCE = new BooleanMapper();

    public static BooleanMapper getInstance() {
        return INSTANCE;
    }

    @Override
    public Boolean apply(String s) {
        if (s.equals("1") || s.equalsIgnoreCase("t") || s.equalsIgnoreCase("true")) {
            return true;
        } else if (s.equals("0") || s.equalsIgnoreCase("f") || s.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalStateException("can not parse ["+s+"] as boolean");
    }
}
