package jree.api;

public interface SessionEventListener<T ,ID> {

    void onMessagePublished(SessionContext context,
                            PubMessage<T , ID> message);
    void preInitialize(SessionContext context);
    void onInitialized(SessionContext context);
    void onClosedByException(SessionContext context , Throwable exception);
    void onCloseByCommand(SessionContext context);
}
