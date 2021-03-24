package jree.abs.parts;

import com.mongodb.Block;
import com.mongodb.internal.async.SingleResultCallback;
import jree.abs.funcs.ForEach;
import jree.api.*;

import java.util.List;

public interface MessageStore<BODY, ID extends Comparable<ID>> {

    void readStoredMessageByCriteria(List<ReadMessageCriteria<String>> criteria, ForEach<PubMessage<BODY, ID>> forEach);

    void readStoredMessage(Session session , ID offset , List<Long> conversations , ForEach<PubMessage<BODY,ID>> forEach);

    void addConversation(final long conversation, OperationResultListener<Long> callback);

    void isConversationExists(final long conversation , OperationResultListener<Boolean> callback);

    void storeMessage(PubMessage<BODY, ID> message, OperationResultListener<PubMessage<BODY, ID>> result);

    void storeAsDisposableMessage(PubMessage<BODY, ID> message, OperationResultListener<PubMessage<BODY, ID>> result);

    void updateMessage(Session editor, Recipient recipient, ID messageId, BODY message, OperationResultListener<PubMessage<BODY, ID>> callback);

    void removeMessage(Session session, Recipient recipient, ID messageId, OperationResultListener<PubMessage<BODY, ID>> result);

    void setTag(Session session, Recipient recipient, InsertTag tag, OperationResultListener<Tag> result);

    void close();
}
