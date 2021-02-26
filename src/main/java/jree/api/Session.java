package jree.api;


public interface Session<T , ID> extends Attachable {

    long clientId();

    long id();

    void close();

    void publishMessage(Recipient recipient, T message, OperationResultListener<PubMessage<T ,ID>> result);

    PubMessage<T , ID> publishMessage(Recipient recipient, T message);

    void editMessage(Recipient recipient , ID messageId , T newMessage , OperationResultListener<PubMessage<T , ID>> result);

    PubMessage<T , ID> editMessage(Recipient recipient , ID messageId , T newMessage);

    void removeMessage(Recipient recipient , ID messageId , OperationResultListener<PubMessage<T , ID>> result);

    PubMessage<T , ID> removeMessage(Recipient recipient , ID messageId);

    void publishDisposableMessage(Recipient recipient , T message ,OperationResultListener<PubMessage<T , ID>> result);

    PubMessage<T , ID> publishDisposableMessage(Recipient recipient , T message);

    void addTag(Recipient recipient, InsertTag tag, OperationResultListener<Tag> result);

    Tag addTag(Recipient recipient, InsertTag tag);

    void setMessageOffset(ID offset, OperationResultListener<Boolean> callback);

    boolean setMessageOffset(ID offset);

    void subscribe(Subscribe subscribes, OperationResultListener<Boolean> result);

    boolean subscribe(Subscribe subscribes);

    void unsubscribe(long conversations, OperationResultListener<Boolean> result);

    boolean unsubscribe(long conversations);

    void setRelationProperties(Recipient recipient , String key , String value , OperationResultListener<Boolean> result);

    boolean setRelationProperties(Recipient recipient , String key , String value );

}
