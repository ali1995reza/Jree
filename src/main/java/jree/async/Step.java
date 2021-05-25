package jree.async;

import jree.api.FailReason;
import jree.api.OperationResultListener;

public abstract class Step <F,T> implements OperationResultListener<F> {

    private OperationResultListener<T> next;
    private Step firstStep;

    private Step(Step firstStep) {
        this.firstStep = firstStep==null?this:firstStep;
    }

    public Step() {
        this(null);
    }

    public <NT> Step<T,NT> then(Step<T,NT> next) {
        next.firstStep = firstStep;
        this.next = next;
        return next;
    }

    public <RF,RT> Step<RF,RT> finish(OperationResultListener<T> resultListener) {
        this.next = resultListener;
        return firstStep;
    }

    public <RF,RT> Step<RF,RT> finish() {
        return firstStep;
    }


    public abstract void execute(F from);

    public abstract T finished(F f);

    @Override
    public final void onSuccess(F result) {
        T t = finished(result);
        if(next==null)
            return;

        if(next instanceof Step) {
            ((Step<T, ?>) next).execute(t);
        } else {
            next.onSuccess(finished(result));
        }
    }

    @Override
    public final void onFailed(FailReason reason) {
        next.onFailed(reason);
    }

}
