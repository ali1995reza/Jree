package jree.api;

public interface PubSubSystem<T , ID> {

    void setBodySerializer(BodySerializer<T> serializer);
    MessageManager<T , ID> messageManager();
    SessionManager<T , ID> sessionManager();

}
