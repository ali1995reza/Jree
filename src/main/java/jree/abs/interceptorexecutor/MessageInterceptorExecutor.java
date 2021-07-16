package jree.abs.interceptorexecutor;

import jree.abs.parts.interceptor.Interceptor;
import jree.abs.parts.interceptor.MessageInterceptor;
import jree.api.*;

final class MessageInterceptorExecutor<BODY, ID> implements MessageInterceptor<BODY, ID> {

    private final Interceptor<BODY, ID>[] interceptors;

    public MessageInterceptorExecutor(Interceptor<BODY, ID>[] interceptors) {
        this.interceptors = interceptors;
    }

    private MessageInterceptor<BODY, ID> getInterceptor(int index) {
        MessageInterceptor<BODY, ID> interceptor = interceptors[index].messageInterceptor();
        return interceptor!=null?interceptor:MessageInterceptor.EMPTY;
    }

    @Override
    public void beforePublishMessage(BODY body, Publisher publisher, Recipient recipient, OperationResultListener<Void> listener) {
        getInterceptor(0).beforePublishMessage(body, publisher, recipient, new BeforePublishMessageExecutor(body, publisher, recipient, listener));
    }

    @Override
    public void onMessagePublish(PubMessage<BODY, ID> message, OperationResultListener<Void> listener) {
        getInterceptor(0).onMessagePublish(message, new OnMessagePublishExecutor(message, listener));
    }

    @Override
    public void beforeSendSignal(BODY signal, Publisher publisher, Recipient recipient, OperationResultListener<Void> listener) {
        getInterceptor(0).beforeSendSignal(signal, publisher, recipient, new BeforeSendSignalExecutor(signal, publisher, recipient, listener));
    }

    @Override
    public void onSignalSend(Signal<BODY> signal, OperationResultListener<Void> listener) {
        getInterceptor(0).onSignalSend(signal, new OnSignalSendExecutor(signal, listener));
    }

    private final class BeforePublishMessageExecutor implements OperationResultListener<Void> {

        private final BODY message;
        private final Publisher publisher;
        private final Recipient recipient;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private BeforePublishMessageExecutor(BODY message, Publisher publisher, Recipient recipient, OperationResultListener<Void> listener) {
            this.message = message;
            this.publisher = publisher;
            this.recipient = recipient;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).beforePublishMessage(message, publisher, recipient, this);
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
                getInterceptor(index).onMessagePublish(message, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }
    }

    private final class BeforeSendSignalExecutor implements OperationResultListener<Void> {

        private final BODY signal;
        private final Publisher publisher;
        private final Recipient recipient;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private BeforeSendSignalExecutor(BODY signal, Publisher publisher, Recipient recipient, OperationResultListener<Void> listener) {
            this.signal = signal;
            this.publisher = publisher;
            this.recipient = recipient;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).beforeSendSignal(signal, publisher, recipient, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }
    }

    private final class OnSignalSendExecutor implements OperationResultListener<Void> {

        private final Signal<BODY> signal;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private OnSignalSendExecutor(Signal<BODY> signal, OperationResultListener<Void> listener) {
            this.signal = signal;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).onSignalSend(signal, this);
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
