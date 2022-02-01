package jree.client_server.server.command.parser.builder.mappers;

import java.util.function.Function;

public class StringMapper implements Function<String, String> {
    private final static StringMapper INSTANCE = new StringMapper();

    public static StringMapper getInstance() {
        return INSTANCE;
    }

    @Override
    public String apply(String s) {
        return s;
    }
}
