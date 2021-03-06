package jree.api;


import java.util.List;

public interface Session<BODY, ID> extends Attachable {

    long clientId();

    long id();

    void close();

    void publishMessage(Recipient recipient, BODY message, OperationResultListener<PubMessage<BODY,ID>> result);

    PubMessage<BODY, ID> publishMessage(Recipient recipient, BODY message);

    void editMessage(Recipient recipient , ID messageId , BODY newMessage , OperationResultListener<PubMessage<BODY, ID>> result);

    PubMessage<BODY, ID> editMessage(Recipient recipient , ID messageId , BODY newMessage);

    void removeMessage(Recipient recipient , ID messageId , OperationResultListener<PubMessage<BODY, ID>> result);

    PubMessage<BODY, ID> removeMessage(Recipient recipient , ID messageId);

    void publishDisposableMessage(Recipient recipient , BODY message , OperationResultListener<PubMessage<BODY, ID>> result);

    PubMessage<BODY, ID> publishDisposableMessage(Recipient recipient , BODY message);

    void addTag(Recipient recipient, InsertTag tag, OperationResultListener<Tag> result);

    Tag addTag(Recipient recipient, InsertTag<ID> tag);

    void setMessageOffset(ID offset, OperationResultListener<Boolean> callback);

    boolean setMessageOffset(ID offset);

    void subscribe(long conversation, OperationResultListener<Boolean> result);

    boolean subscribe(long conversation);

    void unsubscribe(long conversations, OperationResultListener<Boolean> result);

    boolean unsubscribe(long conversations);

    void setRelationAttribute(Recipient recipient , String key , String value , OperationResultListener<Boolean> result);

    boolean setRelationAttribute(Recipient recipient , String key , String value );

    void sendSignal(Recipient recipient ,  BODY signal , OperationResultListener<Signal<BODY>> callback);

    Signal<BODY> sendSignal(Recipient recipient ,  BODY signal);

    void subscribeList(OperationResultListener<List<Long>> callback);

    List<Long> subscribeList();

    List<Recipient> recipientsList();

    void recipientsList(OperationResultListener<List<Recipient>> callback);
}
