package jree.abs.parts;

import jree.api.OperationResultListener;
import jree.api.Session;

public interface SessionInterceptor<BODY, ID> {

    SessionInterceptor EMPTY = new SessionInterceptor() {
        @Override
        public void onSessionOpen(Session session, OperationResultListener listener) {
            listener.onSuccess(null);
        }

        @Override
        public void onSessionClose(Session session, OperationResultListener listener) {
            listener.onSuccess(null);
        }
    };

    void onSessionOpen(Session<BODY, ID> session, OperationResultListener<Void> listener);

    void onSessionClose(Session<BODY, ID> session, OperationResultListener<Void> listener);

}
