package jree.mongo_base;

import jree.api.FailReason;
import jree.api.OperationResultListener;

import java.util.function.BiConsumer;

public class ConditionOperationResultListener<T , A> implements OperationResultListener<T> {

    private final static BiConsumer EMPTY_LISTENER = new BiConsumer() {
        @Override
        public void accept(Object o, Object o2) {

        }
    };

    private A attachment;
    private BiConsumer<T , A> onSuccess = EMPTY_LISTENER;
    private BiConsumer<FailReason , A> onFail = EMPTY_LISTENER;


    public ConditionOperationResultListener<T , A> ifSuccess(BiConsumer<T , A> onSuccess)
    {
        this.onSuccess = onSuccess==null?EMPTY_LISTENER:onSuccess;
        return this;
    }

    public ConditionOperationResultListener<T , A> ifFail(BiConsumer<FailReason , A> onFail)
    {
        this.onFail = onFail==null?EMPTY_LISTENER:onFail;
        return this;
    }

    public ConditionOperationResultListener<T , A> attach(A o)
    {
        attachment = o;
        return this;
    }

    public A attachment()
    {
        return attachment;
    }


    @Override
    public void onSuccess(T result) {
        onSuccess.accept(result , attachment);
    }

    @Override
    public void onFailed(FailReason reason) {
        onFail.accept(reason , attachment);
    }
}
