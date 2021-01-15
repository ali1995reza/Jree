package jree.mongo_base.metakeeper;

import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.mongo_base.MongoFailReasonsCodes;

public abstract class AbstractMetaKeeper implements MetaKeeper {



    public interface LocalKeeper{
        Boolean isSessionExists(long client, long session);
        Boolean isClientExists(long client);
        Boolean isConversationExists(long conversation);
        void keepSession(long client , long session , boolean exists);
        void keepClient(long client , boolean exists);
        void keepConversation(long conversation , boolean exists);
    }

    public interface RemoteKeeper{
        void isSessionExists(long client , long session , OperationResultListener<Boolean> callback);
        void isClientExists(long client , OperationResultListener<Boolean> callback);
        void isConversationExists(long conversation , OperationResultListener<Boolean> callback);
    }


    private final class RemoteToLocal implements OperationResultListener<Boolean> {

        private final OperationResultListener<Boolean> wrapped;
        private final long client , session , conversation;

        private RemoteToLocal(long client, long session, long conversation ,  OperationResultListener<Boolean> wrapped) {
            this.wrapped = wrapped;
            this.client = client;
            this.session = session;
            this.conversation = conversation;
        }


        @Override
        public void onSuccess(Boolean exists) {
            try {
                if(conversation>0) {
                    localKeeper.keepConversation(conversation, exists);
                }else if(session>0)
                {
                    localKeeper.keepSession(client , session , exists);
                }else {
                    localKeeper.keepClient(client , exists);
                }
                wrapped.onSuccess(exists);
            }catch (Throwable e)
            {
                wrapped.onFailed(new FailReason(e , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            wrapped.onFailed(reason);
        }
    }


    private final LocalKeeper localKeeper;
    private final RemoteKeeper remoteKeeper;


    protected AbstractMetaKeeper(LocalKeeper localKeeper, RemoteKeeper remoteKeeper) {
        this.localKeeper = localKeeper;
        this.remoteKeeper = remoteKeeper;
    }

    @Override
    public void isSessionExists(long client, long session, OperationResultListener<Boolean> callback) {
        Boolean isExists = localKeeper.isSessionExists(client , session);
        if(isExists==null)
        {
            remoteKeeper.isSessionExists(client, session,
                    new RemoteToLocal(client , session , -1 , callback));
        }else {
            callback.onSuccess(isExists);
        }
    }

    @Override
    public void isClientExists(long client, OperationResultListener<Boolean> callback) {
        Boolean isExists = localKeeper.isClientExists(client);
        if(isExists==null)
        {
            remoteKeeper.isClientExists(client ,
                    new RemoteToLocal(client , -1 , -1 , callback));
        }else {
            callback.onSuccess(isExists);
        }
    }

    @Override
    public void isConversationExists(long conversation, OperationResultListener<Boolean> callback) {
        Boolean isExists = localKeeper.isConversationExists(conversation);
        if(isExists==null)
        {
            remoteKeeper.isConversationExists(conversation,
                    new RemoteToLocal(-1 , -1 , conversation , callback));
        }else {
            callback.onSuccess(isExists);
        }
    }
}
