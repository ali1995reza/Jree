package jree.api;


import java.util.List;

public interface Session<T> extends Attachable {

    long clientId();

    long id();

    void close();

    void publishMessage(Recipient recipient, T message, OperationResultListener<PubMessage<T>> result);

    PubMessage<T> publishMessage(Recipient recipient, T message);

    void publishDisposableMessage(Recipient recipient , T message ,OperationResultListener<PubMessage<T>> result);

    PubMessage<T> publishDisposableMessage(Recipient recipient , T message);


    void addTag(Recipient recipient, InsertTag tag, OperationResultListener<InsertTagResult> result);

    InsertTagResult addTag(Recipient recipient, InsertTag tag);

    void setMessageOffset(Recipient recipient, long offset, OperationResultListener<Boolean> callback);

    boolean setMessageOffset(Recipient recipient, long offset);

    void subscribe(List<Subscribe> subscribes, OperationResultListener<Boolean> result);

    boolean subscribe(List<Subscribe> subscribes, SubscribeOption subscribeOption);

    void unsubscribe(List<Long> conversations, OperationResultListener<Boolean> result);

    boolean unsubscribe(List<Long> conversations);

}
