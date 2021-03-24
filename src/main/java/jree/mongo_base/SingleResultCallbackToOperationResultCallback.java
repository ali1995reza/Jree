package jree.mongo_base;

import com.mongodb.Function;
import com.mongodb.internal.async.SingleResultCallback;
import jree.abs.codes.FailReasonsCodes;
import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.util.Assertion;

public class SingleResultCallbackToOperationResultCallback<T, R> implements SingleResultCallback<T> {

    private final static Function DEFAULT_CONVERTER = new Function() {
        @Override
        public Object apply(Object o) {
            return o;
        }
    };

    private final static Function ALWAYS_TURE = new Function<Object, Boolean>() {
        @Override
        public Boolean apply(Object o) {
            return true;
        }
    };

    public static <T> SingleResultCallback<T> wrapCallBack(OperationResultListener<T> callback) {
        return new SingleResultCallbackToOperationResultCallback<>(callback, DEFAULT_CONVERTER);
    }

    public static <T, R> SingleResultCallback<T> wrapCallBack(OperationResultListener<R> callback, Function<T, R> converter) {
        return new SingleResultCallbackToOperationResultCallback<>(callback, converter);
    }

    public static <T> SingleResultCallback<T> alwaysTrueCallback(OperationResultListener<Boolean> callback){
        return new SingleResultCallbackToOperationResultCallback<>(callback, ALWAYS_TURE);
    }

    private final OperationResultListener<R> wrapped;
    private final Function<T, R> converter;

    public SingleResultCallbackToOperationResultCallback(OperationResultListener<R> wrapped, Function<T, R> converter) {
        this.wrapped = wrapped == null ? OperationResultListener.EMPTY_LISTENER : wrapped;
        Assertion.ifNull("converter is null", converter);
        this.converter = converter;
    }

    @Override
    public void onResult(T result, Throwable throwable) {
        if (throwable != null) {
            wrapped.onFailed(new FailReason(throwable, FailReasonsCodes.RUNTIME_EXCEPTION));
        } else {
            wrapped.onSuccess(converter.apply(result));
        }
    }

}
