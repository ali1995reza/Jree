package jree.abs.parts;

import jree.api.Session;

public interface SubscribeInterceptor<BODY, ID> {

    void onSubscribe(Session<BODY, ID> session, long conversation);

    void onUnsubscribe(Session<BODY, ID> session, long conversation);

}
