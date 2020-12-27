package jree.api;

public interface PubSubSystem<T> {

    void setBodySerializer(BodySerializer<T> serializer);
    MessageManager<T> messageManager();
    SessionManager<T> sessionManager();

}
