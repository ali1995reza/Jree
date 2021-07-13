package jree.abs.parts;

import jree.api.*;

public interface MessageInterceptor<BODY, ID> {

    MessageInterceptor EMPTY = new MessageInterceptor() {

        @Override
        public void beforePublishMessage(Object o, Publisher publisher, Recipient recipient, OperationResultListener listener) {
            listener.onSuccess(null);
        }

        @Override
        public void onMessagePublish(PubMessage message, OperationResultListener listener) {
            listener.onSuccess(null);
        }

        @Override
        public void afterMessagePublished(PubMessage message, OperationResultListener listener) {
            listener.onSuccess(null);
        }
    };

    void beforePublishMessage(BODY body, Publisher publisher, Recipient recipient, OperationResultListener<Void> listener);

    void onMessagePublish(PubMessage<BODY, ID> message, OperationResultListener<Void> listener);

    void afterMessagePublished(PubMessage<BODY, ID> message, OperationResultListener<Void> listener);

}
