package jree.async;

import jree.api.FailReason;
import jree.api.OperationResultListener;

public abstract class Step <PREVIOUS_PROVIDED_STEP_TYPE, CURRENT_STEP_TYPE, PROVIDE_TO_NEXT_STEP_TYPE> implements OperationResultListener<CURRENT_STEP_TYPE> {

    private OperationResultListener<PROVIDE_TO_NEXT_STEP_TYPE> next;

    public Step() {
    }

    public void setNext(OperationResultListener<PROVIDE_TO_NEXT_STEP_TYPE> next) {
        this.next = next;
    }

    public final void execute(PREVIOUS_PROVIDED_STEP_TYPE lastResult) {
        doExecute(lastResult, this);
    }

    public abstract void doExecute(PREVIOUS_PROVIDED_STEP_TYPE providedValue, OperationResultListener<CURRENT_STEP_TYPE> target);

    public abstract PROVIDE_TO_NEXT_STEP_TYPE finished(CURRENT_STEP_TYPE result);

    @Override
    public final void onSuccess(CURRENT_STEP_TYPE result) {
        PROVIDE_TO_NEXT_STEP_TYPE t = finished(result);
        if(next==null)
            return;

        if(next instanceof Step) {
            ((Step<PROVIDE_TO_NEXT_STEP_TYPE, ? , ?>) next).execute(t);
        } else {
            next.onSuccess(finished(result));
        }
    }

    @Override
    public final void onFailed(FailReason reason) {
        next.onFailed(reason);
    }

}
