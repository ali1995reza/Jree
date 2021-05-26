package jree.async;

import jree.api.OperationResultListener;

public class StepByStep<FT, F , NT , LT> {

    public static <F, T> StepByStep<F , F , T, T> start(Step<F, ? , T> firstStep) {
        return new StepByStep<>(firstStep);
    }

    private final Step<F, ? , NT> firstStep;
    private Step currentStep;

    private StepByStep(Step<F, ? ,NT> firstStep) {
        this.firstStep = firstStep;
        this.currentStep = firstStep;
    }

    public <NT, NNT> StepByStep<FT, NT, NNT, LT> then(Step<NT, ? , NNT> then) {
        currentStep.setNext(then);
        this.currentStep = then;
        return (StepByStep<FT , NT, NNT, LT>) this;
    }

    public Step<FT, ? , LT> finish(OperationResultListener<NT> lastCallback) {
        currentStep.setNext(lastCallback);
        this.currentStep = null;
        return (Step<FT, ?, LT>) firstStep;
    }

}
