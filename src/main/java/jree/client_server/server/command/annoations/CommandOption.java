package jree.client_server.server.command.annoations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandOption {

    String shortName() default "";

    String longName() default "";

    boolean required() default true;

    boolean hasArgs() default true;

    String description() default "";

}
