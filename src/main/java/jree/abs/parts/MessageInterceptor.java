package jree.abs.parts;

import jree.api.OperationResultListener;
import jree.api.PubMessage;

public interface MessageInterceptor<BODY, ID> {

    MessageInterceptor EMPTY = new MessageInterceptor() {
        @Override
        public void beforePublishMessage(PubMessage message, OperationResultListener listener) {
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

    void beforePublishMessage(PubMessage<BODY, ID> message, OperationResultListener<Void> listener);

    void onMessagePublish(PubMessage<BODY, ID> message, OperationResultListener<Void> listener);

    void afterMessagePublished(PubMessage<BODY, ID> message, OperationResultListener<Void> listener);

}
