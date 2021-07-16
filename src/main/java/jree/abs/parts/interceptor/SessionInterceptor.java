package jree.abs.parts.interceptor;

import jree.api.OperationResultListener;
import jree.api.Session;

public interface SessionInterceptor<BODY, ID> {

    SessionInterceptor EMPTY = new SessionInterceptor() {};

    default void beforeOpenSession(long clientId, long sessionId, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void onSessionOpen(Session<BODY, ID> session, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void beforeCloseSession(Session<BODY, ID> session, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void onSessionClose(Session<BODY, ID> session, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void beforeRemoveSession(long clientId, long sessionId, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void onSessionRemove(long clientId, long sessionId, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void beforeRemoveClient(long clientId, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void onClientRemove(long clientId, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

}
