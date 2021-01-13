package jree.api;


import java.util.List;
import java.util.function.Consumer;

public interface MessageManager<T , ID> {

    void createConversation(long id, OperationResultListener<Long> callback);
    long createConversation(long id);
    void createConversation(OperationResultListener<Long> callback);
    long createConversation();
    void readMessages(List<ReadMessageCriteria> criteria, Consumer<PubMessage<T , ID>> forEach);
    Iterable<PubMessage<T , ID>> readMessages(List<ReadMessageCriteria> criteria);
    Iterable<PubMessage<T , ID>> readMessages(ReadMessageCriteria... criteria);
}
