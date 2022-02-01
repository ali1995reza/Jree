package jree.client_server.server.command.parser.builder;

import org.apache.commons.cli.Option;

import java.lang.reflect.Field;
import java.util.function.Function;

public class OptionFieldPair {

    private final Field field;
    private final Option option;
    private final Function<String, ? extends Object> valueMapper;

    public OptionFieldPair(Field field, Option option, Function<String, ? extends Object> valueMapper) {
        this.field = field;
        this.option = option;
        this.valueMapper = valueMapper;
    }

    public Field getField() {
        return field;
    }

    public Option getOption() {
        return option;
    }

    public void set(Object object, String val) {
        try {
            field.set(object, valueMapper.apply(val));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}
