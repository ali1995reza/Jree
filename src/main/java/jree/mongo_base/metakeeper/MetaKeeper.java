package jree.mongo_base.metakeeper;

import jree.api.OperationResultListener;

public interface MetaKeeper {

    void isSessionExists(long client , long session , OperationResultListener<Boolean> callback);
    void isClientExists(long client , OperationResultListener<Boolean> callback);
    void isConversationExists(long conversation , OperationResultListener<Boolean> callback);
}
