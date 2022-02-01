package jree.client_server.server.command.parser.builder.mappers;

import java.util.function.Function;

public class CharMapper implements Function<String, Character> {

    private final static CharMapper INSTANCE = new CharMapper();

    public static CharMapper getInstance() {
        return INSTANCE;
    }

    @Override
    public Character apply(String s) {
        if(s.length() != 1) {
            throw new IllegalStateException("just 1 len strings can map to characters");
        }
        return s.charAt(0);
    }
}
