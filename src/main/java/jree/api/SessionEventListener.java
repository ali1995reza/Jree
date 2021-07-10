package jree.api;

public interface SessionEventListener<BODY, ID> {

    void onMessagePublished(SessionContext context, PubMessage<BODY, ID> message);

    void onSignalReceived(SessionContext context , Signal<BODY> signal);

    void onInitialized(SessionContext context);

    void onClosing(SessionContext context);

    void onClosedByException(SessionContext context, Throwable exception);

    void onCloseByCommand(SessionContext context);

}
