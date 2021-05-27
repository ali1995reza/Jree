package jree.async;

import jree.api.OperationResultListener;

public class StepByStep<FT, F , NT , LT> {

    public static <F, T> StepByStep<F , F , T, T> start(Step<F, ? , T> firstStep) {
        return new StepByStep<>(firstStep);
    }

    private final ExecutableStep<FT> executableStep;
    private Step currentStep;

    private StepByStep(Step<FT, ? ,LT> firstStep) {
        this.executableStep = new ExecutableStep<>(firstStep);
        this.currentStep = firstStep;
    }

    public <NNT> StepByStep<FT, NT, NNT, LT> then(Step<NT, ? , NNT> then) {
        currentStep.setNext(then);
        this.currentStep = then;
        return (StepByStep<FT , NT, NNT, LT>) this;
    }

    public ExecutableStep<FT> finish(OperationResultListener<NT> lastCallback) {
        currentStep.setNext(lastCallback);
        this.currentStep = null;
        return executableStep;
    }

}
