package jree.api;


import java.util.List;
import java.util.function.Consumer;

public interface MessageManager<T> {

    void createConversation(long id, OperationResultListener<Long> callback);
    long createConversation(long id);
    void createConversation(OperationResultListener<Long> callback);
    long createConversation();
    void readMessages(List<ReadMessageCriteria> criteria, Consumer<PubMessage<T>> forEach);
    Iterable<PubMessage<T>> readMessages(List<ReadMessageCriteria> criteria);
    Iterable<PubMessage<T>> readMessages(ReadMessageCriteria... criteria);
}
