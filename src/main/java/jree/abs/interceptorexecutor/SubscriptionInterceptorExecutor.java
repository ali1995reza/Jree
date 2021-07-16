package jree.abs.interceptorexecutor;

import jree.abs.parts.interceptor.Interceptor;
import jree.abs.parts.interceptor.SubscriptionInterceptor;
import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.api.Recipient;

public class SubscriptionInterceptorExecutor<BODY, ID> implements SubscriptionInterceptor<BODY, ID> {

    private final Interceptor<BODY, ID>[] interceptors;

    public SubscriptionInterceptorExecutor(Interceptor<BODY, ID>[] interceptors) {
        this.interceptors = interceptors;
    }

    private SubscriptionInterceptor<BODY, ID> getInterceptor(int index) {
        SubscriptionInterceptor<BODY, ID> interceptor = interceptors[index].subscriptionInterceptor();
        return interceptor!=null?interceptor:SubscriptionInterceptor.EMPTY;
    }

    @Override
    public void beforeSubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        getInterceptor(0).beforeSubscribe(subscriber, conversation, new BeforeSubscribeExecutor(subscriber, conversation, listener));
    }

    @Override
    public void onSubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        getInterceptor(0).onSubscribe(subscriber, conversation, new OnSubscribeExecutor(subscriber, conversation, listener));
    }

    @Override
    public void beforeUnsubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        getInterceptor(0).beforeUnsubscribe(subscriber, conversation, new BeforeUnsubscribeExecutor(subscriber, conversation, listener));
    }

    @Override
    public void onUnsubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        getInterceptor(0).onUnsubscribe(subscriber, conversation, new OnUnsubscribeExecutor(subscriber, conversation, listener));
    }

    private final class BeforeSubscribeExecutor implements OperationResultListener<Void> {

        private final Recipient subscriber;
        private final long conversationId;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private BeforeSubscribeExecutor(Recipient subscriber, long conversationId, OperationResultListener<Void> listener) {
            this.subscriber = subscriber;
            this.conversationId = conversationId;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).beforeSubscribe(subscriber, conversationId, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class OnSubscribeExecutor implements OperationResultListener<Void> {

        private final Recipient subscriber;
        private final long conversationId;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private OnSubscribeExecutor(Recipient subscriber, long conversationId, OperationResultListener<Void> listener) {
            this.subscriber = subscriber;
            this.conversationId = conversationId;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).onSubscribe(subscriber, conversationId, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class BeforeUnsubscribeExecutor implements OperationResultListener<Void> {

        private final Recipient subscriber;
        private final long conversationId;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private BeforeUnsubscribeExecutor(Recipient subscriber, long conversationId, OperationResultListener<Void> listener) {
            this.subscriber = subscriber;
            this.conversationId = conversationId;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).beforeUnsubscribe(subscriber, conversationId, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class OnUnsubscribeExecutor implements OperationResultListener<Void> {

        private final Recipient subscriber;
        private final long conversationId;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private OnUnsubscribeExecutor(Recipient subscriber, long conversationId, OperationResultListener<Void> listener) {
            this.subscriber = subscriber;
            this.conversationId = conversationId;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).onUnsubscribe(subscriber, conversationId, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

}
