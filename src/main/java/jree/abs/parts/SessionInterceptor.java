package jree.abs.parts;

import jree.api.Session;

public interface SessionInterceptor<BODY, ID> {

    void beforeOpen(Session<BODY, ID> session);

    void onOpen(Session<BODY, ID> session);

    void afterOpened(Session<BODY, ID> session);

    void beforeClose(Session<BODY, ID> session);

    void onClose(Session<BODY, ID> session);

    void afterClosed(Session<BODY, ID> session);

}
