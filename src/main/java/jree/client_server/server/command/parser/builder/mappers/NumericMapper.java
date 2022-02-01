package jree.client_server.server.command.parser.builder.mappers;

import jree.client_server.server.command.assertion.Assert;

import java.math.BigDecimal;
import java.util.function.Function;

public class NumericMapper implements Function<String, Object> {


    private final Class clazz;

    public NumericMapper(Class clazz) {
        this.clazz = clazz;
        Assert.isTrue(clazz == Integer.class || clazz == int.class ||
                        clazz == Long.class || clazz == long.class ||
                        clazz == Double.class || clazz == double.class ||
                        clazz == Float.class || clazz == float.class ||
                        clazz == Byte.class || clazz == byte.class ||
                        clazz == BigDecimal.class,
                () -> new IllegalStateException("class [" + clazz + "] not a number format"));
    }

    @Override
    public Object apply(String s) {
        if (clazz == Integer.class || clazz == int.class) {
            return Integer.parseInt(s);
        }

        if (clazz == Long.class || clazz == long.class) {
            return Long.parseLong(s);
        }

        if (clazz == Double.class || clazz == double.class) {
            return Double.parseDouble(s);
        }

        if (clazz == Float.class || clazz == float.class) {
            return Float.parseFloat(s);
        }

        if (clazz == Byte.class || clazz == byte.class) {
            return Byte.parseByte(s);
        }

        if (clazz == BigDecimal.class) {
            return new BigDecimal(s);
        }

        throw new IllegalStateException("can not parse [" + s + "] as numeric value");
    }
}
