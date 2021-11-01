package jree.async;

import jree.api.FailReason;
import jree.api.OperationResultListener;
import jutils.assertion.Assertion;

public abstract class Step <PREVIOUS_PROVIDED_STEP_TYPE, CURRENT_STEP_TYPE, PROVIDE_TO_NEXT_STEP_TYPE> implements OperationResultListener<CURRENT_STEP_TYPE> {

    private OperationResultListener<PROVIDE_TO_NEXT_STEP_TYPE> next;

    public Step() {
    }

    final void setNext(OperationResultListener<PROVIDE_TO_NEXT_STEP_TYPE> next) {
        Assertion.ifNotNull("setting next step on a disposed step", this.next);
        Assertion.ifNull("next step is null" , next);
        this.next = next;
    }

    final void execute(PREVIOUS_PROVIDED_STEP_TYPE providedValue) {
        doExecute(providedValue, this);
    }

    final void execute() {
        doExecute(null, this);
    }

    protected abstract void doExecute(PREVIOUS_PROVIDED_STEP_TYPE providedValue, OperationResultListener<CURRENT_STEP_TYPE> target);

    protected abstract PROVIDE_TO_NEXT_STEP_TYPE finished(CURRENT_STEP_TYPE result);

    @Override
    public final void onSuccess(CURRENT_STEP_TYPE result) {
        PROVIDE_TO_NEXT_STEP_TYPE t = finished(result);
        if(next==null)
            return;

        if(next instanceof Step) {
            ((Step<PROVIDE_TO_NEXT_STEP_TYPE, ? , ?>) next).execute(t);
        } else {
            next.onSuccess(t);
        }
    }

    @Override
    public final void onFailed(FailReason reason) {
        next.onFailed(reason);
    }

}
