package jree.api;


import java.util.List;
import java.util.function.Consumer;

public interface MessageManager<BODY, ID> {

    void createConversation(long id, OperationResultListener<Long> callback);
    long createConversation(long id);
    void createConversation(OperationResultListener<Long> callback);
    long createConversation();
    void readMessages(List<ReadMessageCriteria<ID>> criteria, Consumer<PubMessage<BODY, ID>> forEach);
    Iterable<PubMessage<BODY, ID>> readMessages(List<ReadMessageCriteria<ID>> criteria);
    Iterable<PubMessage<BODY, ID>> readMessages(ReadMessageCriteria<ID>... criteria);
}
