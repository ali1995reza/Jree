package jree.async;

public class ExecutableStep<ARG> {

    private final Step<ARG, ?, ?> firstStep;

    public ExecutableStep(Step<ARG, ?, ?> firstStep) {
        this.firstStep = firstStep;
    }

    public void execute(ARG arg) {
        firstStep.execute(arg);
    }

    public void execute() {
        firstStep.execute();
    }
}
