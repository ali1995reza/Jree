package jree.abs.parts.interceptor;

import jree.api.*;

public interface MessageInterceptor<BODY, ID> {

    MessageInterceptor EMPTY = new MessageInterceptor() {};

    default void beforePublishMessage(BODY body, Publisher publisher, Recipient recipient, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void onMessagePublish(PubMessage<BODY, ID> message, OperationResultListener<Void> listener){
        listener.onSuccess(null);
    }

    default void beforeSendSignal(BODY signal, Publisher publisher, Recipient recipient, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void onSignalSend(Signal<BODY> signal, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }
}
