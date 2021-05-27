package jree.abs.funcs;

import java.util.function.Consumer;

public interface ForEach<T> {

    static <T> ForEach<T> fromConsumer(Consumer<T> consumer){
        return consumer::accept;
    }

    void accept(T t);
    default void done(Throwable e){

    }
}
