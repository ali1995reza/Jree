package jree.mongo_base;

import com.mongodb.internal.async.SingleResultCallback;
import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.mongo_base.metakeeper.AbstractMetaKeeper;

public class MongoRemoteMetaKeeper implements AbstractMetaKeeper.RemoteKeeper {

    private final MongoClientDetailsStore store;
    private final MongoMessageStore messageStore;

    public MongoRemoteMetaKeeper(MongoClientDetailsStore store, MongoMessageStore messageStore) {
        this.store = store;
        this.messageStore = messageStore;
    }


    @Override
    public void isSessionExists(long client, long session, OperationResultListener<Boolean> callback) {
        store.isSessionExists(client, session, new SingleResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean aBoolean, Throwable throwable) {
                if(throwable!=null)
                {
                    callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                }else {
                    callback.onSuccess(aBoolean);
                }
            }
        });
    }

    @Override
    public void isClientExists(long client, OperationResultListener<Boolean> callback) {
        store.isClientExists(client, new SingleResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean aBoolean, Throwable throwable) {
                if(throwable!=null)
                {
                    callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                }else {
                    callback.onSuccess(aBoolean);
                }
            }
        });
    }

    @Override
    public void isConversationExists(long conversation, OperationResultListener<Boolean> callback) {
        messageStore.isConversationExists(conversation, new SingleResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean aBoolean, Throwable throwable) {
                if(throwable!=null)
                {
                    callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                }else {
                    callback.onSuccess(aBoolean);
                }
            }
        });
    }
}
