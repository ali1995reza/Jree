package jree.mongo_base;

import jree.api.FailReason;
import jree.api.OperationResultListener;

import java.util.function.BiConsumer;

public class FailCaller<T> implements BiConsumer<Throwable , OperationResultListener<T>> {

    public final static FailCaller RUNTIME_FAIL_CALLER = new FailCaller(MongoFailReasonsCodes.RUNTIME_EXCEPTION);


    private final int failCode;

    public FailCaller(int failCode) {
        this.failCode = failCode;
    }


    @Override
    public void accept(Throwable throwable, OperationResultListener<T> listener) {
        listener.onFailed(new FailReason(throwable , failCode));
    }
}
