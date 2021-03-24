package jree.abs.funcs;

import java.util.function.Consumer;

public interface ForEach<T> {

    static <T> ForEach<T> fromConsumer(Consumer<T> consumer){
        return new ForEach<T>() {
            @Override
            public void accept(T t) {
                consumer.accept(t);
            }
        };
    }

    void accept(T t);
    default void done(Throwable e){

    }
}
