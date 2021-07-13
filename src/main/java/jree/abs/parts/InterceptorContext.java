package jree.abs.parts;

import jree.api.PubMessage;

public interface InterceptorContext<BODY, ID> {

    void notifyMessage(PubMessage<BODY, ID> pubMessage);

    //todo add more functions

}
