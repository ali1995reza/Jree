package jree.mongo_base;

import jree.api.OperationResultListener;

import java.util.function.BiConsumer;

public class SuccessCaller<T , A> implements BiConsumer<T , A> {


    public final static <T , A> SuccessCaller<T , A> call(OperationResultListener<T> callback)
    {
        return new SuccessCaller<>(callback);
    }

    private final OperationResultListener<T> callback;

    public SuccessCaller(OperationResultListener<T> callback) {
        this.callback = callback;
    }


    @Override
    public void accept(T t, A o) {
        callback.onSuccess(t);
    }
}
