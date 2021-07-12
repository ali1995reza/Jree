package jree.abs.parts;

import jree.api.OperationResultListener;
import jree.api.Session;

public interface SubscribeInterceptor<BODY, ID> {

    SubscribeInterceptor EMPTY = new SubscribeInterceptor() {
        @Override
        public void onSubscribe(Session session, long conversation, OperationResultListener listener) {
            listener.onSuccess(null);
        }

        @Override
        public void onUnsubscribe(Session session, long conversation, OperationResultListener listener) {
            listener.onSuccess(null);
        }
    };


    void onSubscribe(Session<BODY, ID> session, long conversation, OperationResultListener<Void> listener);

    void onUnsubscribe(Session<BODY, ID> session, long conversation, OperationResultListener<Void> listener);

}
