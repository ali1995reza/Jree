package jree.async;

import jree.api.OperationResultListener;

public abstract class ExtraStep<F, T> extends Step<F, T, F> {

    private F result;

    @Override
    protected final void doExecute(F providedValue, OperationResultListener<T> target) {
        result = providedValue;
        executeExtraStep(providedValue, target);
    }

    @Override
    protected final F finished(T result) {
        return this.result;
    }

    protected abstract void executeExtraStep(F providedValue, OperationResultListener<T> target);

}
