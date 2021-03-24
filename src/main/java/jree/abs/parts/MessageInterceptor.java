package jree.abs.parts;

import jree.api.PubMessage;

public interface MessageInterceptor<BODY, ID> {

    void beforePublishMessage(PubMessage<BODY, ID> message);

    void onMessagePublish(PubMessage<BODY, ID> message);

    void onMessagePublished(PubMessage<BODY, ID> message);

}
