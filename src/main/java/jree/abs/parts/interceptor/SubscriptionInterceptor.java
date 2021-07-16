package jree.abs.parts.interceptor;

import jree.api.OperationResultListener;
import jree.api.Recipient;

public interface SubscriptionInterceptor<BODY, ID> {

    SubscriptionInterceptor EMPTY = new SubscriptionInterceptor() {};

    default void beforeSubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void onSubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void beforeUnsubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    default void onUnsubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

}
