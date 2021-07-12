package jree.abs.interceptorexecutor;

import jree.abs.parts.Interceptor;
import jree.abs.parts.MessageInterceptor;
import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.api.PubMessage;

final class MessageInterceptorExecutor<BODY, ID> implements MessageInterceptor<BODY, ID> {

    private final Interceptor<BODY, ID>[] interceptors;

    public MessageInterceptorExecutor(Interceptor<BODY, ID>[] interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public void beforePublishMessage(PubMessage<BODY, ID> message, OperationResultListener<Void> listener) {
        interceptors[0].messageInterceptor()
                .beforePublishMessage(message, new BeforePublishMessageExecutor(message, listener));
    }

    @Override
    public void onMessagePublish(PubMessage<BODY, ID> message, OperationResultListener<Void> listener) {
        interceptors[0].messageInterceptor()
                .onMessagePublish(message, new OnMessagePublishExecutor(message, listener));
    }

    @Override
    public void afterMessagePublished(PubMessage<BODY, ID> message, OperationResultListener<Void> listener) {
        interceptors[0].messageInterceptor()
                .afterMessagePublished(message, new AfterMessagePublishedExecutor(message, listener));
    }

    private final class BeforePublishMessageExecutor implements OperationResultListener<Void> {

        private final PubMessage<BODY, ID> message;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private BeforePublishMessageExecutor(PubMessage<BODY, ID> message, OperationResultListener<Void> listener) {
            this.message = message;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                interceptors[index].messageInterceptor()
                        .beforePublishMessage(message, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }
    }

    private final class OnMessagePublishExecutor implements OperationResultListener<Void> {

        private final PubMessage<BODY, ID> message;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private OnMessagePublishExecutor(PubMessage<BODY, ID> message, OperationResultListener<Void> listener) {
            this.message = message;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                interceptors[index].messageInterceptor()
                        .onMessagePublish(message, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }
    }

    private final class AfterMessagePublishedExecutor implements OperationResultListener<Void> {

        private final PubMessage<BODY, ID> message;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private AfterMessagePublishedExecutor(PubMessage<BODY, ID> message, OperationResultListener<Void> listener) {
            this.message = message;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                interceptors[index].messageInterceptor()
                        .afterMessagePublished(message, this);
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
