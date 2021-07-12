package jree.abs.parts;

import jree.api.OperationResultListener;
import jree.api.Session;

public interface SessionInterceptor<BODY, ID> {

    SessionInterceptor EMPTY = new SessionInterceptor() {
        @Override
        public void beforeSessionOpen(long clientId, long sessionId, OperationResultListener listener) {
            listener.onSuccess(null);
        }

        @Override
        public void onSessionOpen(Session session, OperationResultListener  listener) {
            listener.onSuccess(null);
        }

        @Override
        public void afterSessionOpened(Session session, OperationResultListener  listener) {
            listener.onSuccess(null);
        }

        @Override
        public void beforeSessionClose(Session session, OperationResultListener  listener) {
            listener.onSuccess(null);
        }

        @Override
        public void onSessionClose(Session session, OperationResultListener  listener) {
            listener.onSuccess(null);
        }

        @Override
        public void afterSessionClosed(Session session, OperationResultListener  listener) {
            listener.onSuccess(null);
        }
    };

    void beforeSessionOpen(long clientId, long sessionId, OperationResultListener<Void> listener);

    void onSessionOpen(Session<BODY, ID> session, OperationResultListener<Void> listener);

    void afterSessionOpened(Session<BODY, ID> session, OperationResultListener<Void> listener);

    void beforeSessionClose(Session<BODY, ID> session, OperationResultListener<Void> listener);

    void onSessionClose(Session<BODY, ID> session, OperationResultListener<Void> listener);

    void afterSessionClosed(Session<BODY, ID> session, OperationResultListener<Void> listener);

}
