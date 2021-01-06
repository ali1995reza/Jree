package jree.mongo_base;

import jree.api.OperationResultListener;

import java.util.function.BiConsumer;

public class IndependentSuccessCaller<T> implements BiConsumer {

    public final static <T> IndependentSuccessCaller<T> call(OperationResultListener<T> callback, T result)
    {
        return new IndependentSuccessCaller<>(callback, result);
    }

    private final OperationResultListener<T> callback;
    private final T result;

    public IndependentSuccessCaller(OperationResultListener<T> callback, T result) {
        this.callback = callback;
        this.result = result;
    }


    @Override
    public void accept(Object t, Object o) {
        callback.onSuccess(result);
    }
}
