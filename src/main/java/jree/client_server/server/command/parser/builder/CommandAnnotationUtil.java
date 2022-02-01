package jree.client_server.server.command.parser.builder;

import jree.client_server.server.command.assertion.Assert;
import jree.client_server.server.command.annoations.Command;
import jree.client_server.server.command.annoations.CommandOption;
import jree.client_server.server.command.parser.builder.mappers.BooleanMapper;
import jree.client_server.server.command.parser.builder.mappers.CharMapper;
import jree.client_server.server.command.parser.builder.mappers.NumericMapper;
import jree.client_server.server.command.parser.builder.mappers.StringMapper;
import org.apache.commons.cli.Option;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class CommandAnnotationUtil {

    private final static Pattern COMMAND_PATTERN = Pattern.compile("^([0-9a-zA-Z_\\-])+$");
    private final static Pattern OPTION_PATTERN = Pattern.compile("^([0-9a-zA-Z_])+$");


    public static String getCommandName(Class cls) {
        Command command = (Command) cls.getAnnotation(Command.class);
        Assert.isNotNull(command, () -> new IllegalStateException("class [" + cls + "] is not annotated with Command annotation"));
        Assert.isTrue(COMMAND_PATTERN.matcher(command.value()).matches(), () -> new IllegalStateException("class [" + cls + "] command name not valid"));
        return command.value();
    }

    public static OptionFieldPair getCommandOption(Field field) {
        CommandOption option = field.getAnnotation(CommandOption.class);
        if (option == null) {
            return null;
        }
        Assert.isFalse(Modifier.isFinal(field.getModifiers()), () -> new IllegalStateException("final fields can not be a command option [" + field + "]"));
        Assert.isFalse(Modifier.isStatic(field.getModifiers()), () -> new IllegalStateException("static fields can not be a command option [" + field + "]"));
        validateType(field.getType());
        Option.Builder builder = Option.builder();
        final String shortName = option.shortName();
        final String longName = option.longName();
        Assert.isFalse(isBlank(shortName) && isBlank(longName), () -> new IllegalStateException("short name and long name both is blank for [" + field + "]"));
        if (!isBlank(shortName)) {
            Assert.isTrue(OPTION_PATTERN.matcher(shortName).matches(), () -> new IllegalStateException("invalid option name [" + shortName + "]"));
            builder.option(shortName);
        }
        if (!isBlank(longName)) {
            Assert.isTrue(OPTION_PATTERN.matcher(longName).matches(), () -> new IllegalStateException("invalid option name [" + longName + "]"));
            builder.longOpt(longName);
        }
        builder.required(option.required());
        builder.hasArg(option.hasArgs());
        builder.desc(option.description());
        builder.type(field.getType());
        field.setAccessible(true);
        if (field.getType() == String.class) {
            return new OptionFieldPair(field, builder.build(), StringMapper.getInstance());
        } else if (field.getType() == Character.class || field.getType() == char.class) {
            return new OptionFieldPair(field, builder.build(), CharMapper.getInstance());
        } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
            return new OptionFieldPair(field, builder.build(), BooleanMapper.getInstance());
        }
        return new OptionFieldPair(field, builder.build(), new NumericMapper(field.getType()));
    }

    public static List<OptionFieldPair> getCommandOptions(Class clazz) {
        List<OptionFieldPair> options = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            OptionFieldPair option = getCommandOption(field);
            if (option != null) {
                options.add(option);
            }
        }
        return Collections.unmodifiableList(options);
    }

    private static boolean isBlank(String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        //todo fix
        return false;
        //return s.isBlank();
    }

    private static void validateType(Class clazz) {
        Assert.isTrue(clazz == Integer.class || clazz == int.class ||
                        clazz == Long.class || clazz == long.class ||
                        clazz == Double.class || clazz == double.class ||
                        clazz == Float.class || clazz == float.class ||
                        clazz == Byte.class || clazz == byte.class ||
                        clazz == Character.class || clazz == char.class ||
                        clazz == Boolean.class || clazz == boolean.class ||
                        clazz == String.class ||
                        clazz == BigDecimal.class,
                () -> new IllegalStateException("class [" + clazz + "] not a valid type for option"));
    }

}
