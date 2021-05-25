package jree.async;

import jree.api.FailReason;
import jree.api.OperationResultListener;

public class OperationResultListenerWrapper<T> implements OperationResultListener<T> {

    @Override
    public void onSuccess(T result) {
    }

    @Override
    public void onFailed(FailReason reason) {
    }

}
