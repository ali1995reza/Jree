package jree.client_server.server.command.assertion;

import java.util.List;
import java.util.function.Supplier;

public class Assert {

    public static <T extends Throwable> void isFalse(boolean condition, Supplier<T> ex) throws T {
        if (condition) {
            throw ex.get();
        }
    }

    public static <T extends Throwable> void isTrue(boolean condition, Supplier<T> ex) throws T {
        if (!condition) {
            throw ex.get();
        }
    }

    public static <T extends Throwable> void isNotNull(Object o, Supplier<T> ex) throws T {
        isTrue(o != null, ex);
    }

    public static <T extends Throwable> void isNull(Object o, Supplier<T> ex) throws T {
        isTrue(o == null, ex);
    }

    public static <T extends Throwable> void isPositive(Integer number, Supplier<T> ex) throws T {
        isTrue(number > 0, ex);
    }


    public static <T extends Throwable> void isNotNegative(Integer number, Supplier<T> ex) throws T {
        isTrue(number >= 0, ex);
    }

    public static <T extends Throwable> void isPositive(Long number, Supplier<T> ex) throws T {
        isTrue(number > 0, ex);
    }

    public static <T extends Throwable> void isNotNegative(Long number, Supplier<T> ex) throws T {
        isTrue(number >= 0, ex);
    }

    public static <T extends Throwable> void isNotBlank(String str, Supplier<T> ex) throws T {
        //isFalse(str == null || str.isBlank(), ex);
        isFalse(str == null || str.isEmpty(), ex);
    }

    public static <T extends Throwable> void doseNotContains(String str, List<CharSequence> sequences, Supplier<T> ex) throws T {
        for (CharSequence sequence : sequences) {
            isFalse(str.contains(sequence), ex);
        }
    }
}
