package jree.abs.parts.interceptor;

import jree.api.PubMessage;
import jree.api.Recipient;
import jree.api.Signal;

public interface InterceptorContext<BODY, ID> {

    void notifyMessage(PubMessage<BODY, ID> pubMessage);

    void notifySignal(Signal<BODY> signal);

    void notifyUnsubscribe(Recipient subscriber, long conversationId);

    void notifySubscribe(Recipient recipient, long conversationId);

    void notifyRemoveSession(long clientId, long sessionId);

    void notifyRemoveClient(long clientId);

    //todo add more functions

}
