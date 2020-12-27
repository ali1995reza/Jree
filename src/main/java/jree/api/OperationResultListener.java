package jree.api;

public interface OperationResultListener<T> {

    OperationResultListener EMPTY_LISTENER = new OperationResultListener() {
        @Override
        public void onSuccess(Object result) {

        }

        @Override
        public void onFailed(FailReason reason) {
        }
    };

    OperationResultListener LOGGER_LISTENER = new OperationResultListener() {
        @Override
        public void onSuccess(Object result) {
            System.out.println("RESULT : "+result);
        }

        @Override
        public void onFailed(FailReason reason) {
            reason.printStackTrace();
        }
    };

    void onSuccess(T result);
    void onFailed(FailReason reason);
}
