package jree.api;

public interface PubSubSystem<BODY, ID> {

    MessageManager<BODY, ID> messageManager();

    SessionManager<BODY, ID> sessionManager();

}
