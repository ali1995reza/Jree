package jree.async;

import jree.api.FailReason;
import jree.api.OperationResultListener;

public class StepByStepCheck {

    public static void main(String[] args) {
        StepByStep.start(new RawTypeProviderStep<Integer, String>() {
            @Override
            public void doExecute(Integer providedValue, OperationResultListener<String> target) {
                target.onSuccess(String.valueOf(providedValue)+"WTF");
            }
        }).then(RawTypeProviderStep.execute((String s,OperationResultListener<String> t)->{

            t.onSuccess("step 2 provided !");

        })).finish(new OperationResultListener<String>() {
            @Override
            public void onSuccess(String result) {
                System.out.println(result);
            }

            @Override
            public void onFailed(FailReason reason) {

            }
        }).execute(23222);
    }

}
