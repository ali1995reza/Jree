package jree.abs.parts;

import jree.abs.funcs.ForEach;
import jree.api.OperationResultListener;
import jree.api.Recipient;

import java.util.List;

public interface SubscribersHolder {

    void addSubscriber(long conversation, long clientId, OperationResultListener<Boolean> callback);

    void addSubscriber(List<Long> conversations, long clientId, OperationResultListener<Boolean> callback);

    void removeSubscriber(long conversation, long clientId, OperationResultListener<Boolean> callback);

    void removeSubscriberFromAllConversations(long clientId);

    void getSubscribers(long conversation, ForEach<List<Long>> forEach);

}
