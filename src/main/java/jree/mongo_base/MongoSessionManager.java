package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import jree.api.*;

import java.util.List;

public class MongoSessionManager<T> implements SessionManager<T> {

    private final class RandomClientIdGenerator implements SingleResultCallback<UpdateResult>{

        private final OperationResultListener<Long> callback;
        private long id;

        private RandomClientIdGenerator(OperationResultListener<Long> callback , long id) {
            this.callback = callback;
            this.id = id;
        }

        @Override
        public void onResult(UpdateResult updateResult, Throwable throwable) {
            if(throwable!=null)
            {
                callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
            }else
            {
                if(updateResult.getMatchedCount()>0)
                {
                    id = StaticFunctions.newID();
                    detailsStore.addClient(
                            id ,
                            this
                    );
                }else {
                    callback.onSuccess(id);
                }
            }
        }
    }

    private final MongoMessageStore messageStore;
    private final MongoClientDetailsStore detailsStore;
    private final ClientsHolder clients;
    private final BodySerializer<T> serializer;

    public MongoSessionManager(MongoMessageStore messageStore, MongoClientDetailsStore detailsStore, ClientsHolder clients, BodySerializer<T> serializer) {
        this.messageStore = messageStore;
        this.detailsStore = detailsStore;
        this.clients = clients;
        this.serializer = serializer;
    }


    @Override
    public void createClient(long id, OperationResultListener<Boolean> callback) {
        detailsStore.addClient(id, new SingleResultCallback<UpdateResult>() {
            @Override
            public void onResult(UpdateResult updateResult, Throwable throwable) {
                if(throwable!=null)
                {
                    callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                }else
                {
                    if(updateResult.getMatchedCount()>0)
                    {
                        callback.onFailed(new FailReason(MongoFailReasonsCodes.CLIENT_ALREADY_EXISTS));
                    }else {
                        callback.onSuccess(true);
                    }
                }
            }
        });
    }

    @Override
    public boolean createClient(long id) {
        AsyncToSync<Boolean> asyncToSync = new AsyncToSync<>();
        createClient(id , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createClient(OperationResultListener<Long> callback) {
        long id = StaticFunctions.newID();
        detailsStore.addClient(id ,
                new RandomClientIdGenerator(callback, id));
    }

    @Override
    public long createClient() {
        AsyncToSync<Long> asyncToSync = new AsyncToSync<>();
        createClient(asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createClientIfNotExists(long id, OperationResultListener<Boolean> callback) {
        detailsStore.addClient(id, new SingleResultCallback<UpdateResult>() {
            @Override
            public void onResult(UpdateResult updateResult, Throwable throwable) {
                if(throwable!=null)
                {
                    callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                }else
                {
                    callback.onSuccess(true);
                }
            }
        });
    }

    @Override
    public boolean createClientIfNotExists(long id) {
        AsyncToSync<Boolean> asyncToSync = new AsyncToSync<>();
        createClientIfNotExists(id , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createSession(long clientId, OperationResultListener<Long> callback) {
        detailsStore.addSessionToClient(clientId, new SingleResultCallback<Long>() {
            @Override
            public void onResult(Long aLong, Throwable throwable) {
                if(throwable==null)
                {
                    callback.onSuccess(aLong);
                }else
                {
                    callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                }
            }
        });
    }

    @Override
    public long createSession(long clientId) {
        AsyncToSync<Long> asyncToSync = new AsyncToSync<>();
        createSession(clientId , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void connectToService(long clientId, long sessionId , SessionEventListener<T> ev , OperationResultListener<Session<T>> callback) {
        final ExceptionAdaptedEventListener<T>
                eventListener = new ExceptionAdaptedEventListener<>(ev);
        detailsStore.isSessionExists(
                clientId, sessionId,
                new SingleResultCallback<Boolean>() {
                    @Override
                    public void onResult(Boolean aBoolean, Throwable throwable) {
                        if(throwable==null)
                        {
                            if(aBoolean)
                            {
                                MongoSession session = new MongoSession(clientId ,
                                        sessionId , eventListener, messageStore, detailsStore, clients, serializer);

                                clients.addNewSession(session);
                                detailsStore.getConversationOffsets(
                                        session,
                                        new SingleResultCallback<List<ConversationOffset>>() {
                                            @Override
                                            public void onResult(List<ConversationOffset> conversationOffsets, Throwable throwable) {
                                                if(throwable!=null)
                                                {
                                                    clients.removeSession(session);
                                                    callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                                                    return;
                                                }

                                                callback.onSuccess(session);
                                                messageStore.readStoredMessage(conversationOffsets, serializer
                                                        , new Block<PubMessage>() {
                                                            @Override
                                                            public void apply(PubMessage message) {
                                                                session.beforeRelease(message);
                                                            }
                                                        }, new SingleResultCallback<Void>() {
                                                            @Override
                                                            public void onResult(Void aVoid, Throwable throwable) {

                                                                if(throwable!=null)
                                                                {
                                                                    clients.removeSession(session);
                                                                    session.eventListener().onClosedByException(
                                                                            throwable
                                                                    );
                                                                }else {
                                                                    session.release();
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                );
                                
                            }else
                            {
                                callback.onFailed(new FailReason(MongoFailReasonsCodes.SESSION_NOT_EXISTS));
                            }
                        }else
                        {
                            callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                        }
                    }
                }
        );
    }

    @Override
    public Session<T> connectToService(long clientId, long sessionId, SessionEventListener<T> eventListener) {
        AsyncToSync<Session<T>> asyncToSync = new AsyncToSync<>();
        connectToService(clientId, sessionId, eventListener , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void disconnectFromService(Session<T> session , OperationResultListener<Boolean> callback) {
        SessionsHolder holder = clients.getSessionsForClient(session.clientId());
        if(holder==null)
        {
            callback.onFailed(new FailReason(MongoFailReasonsCodes.CLIENT_NOT_ACTIVE));
        }
        if(!holder.removeSession((MongoSession) session))
        {
            callback.onFailed(new FailReason(MongoFailReasonsCodes.SESSION_NOT_ACTIVE));
        }

        callback.onSuccess(true);
    }

    @Override
    public boolean disconnectFromService(Session<T> session) {
        return false;
    }

    @Override
    public void checkPresence(List<Long> ids, OperationResultListener<List<Presence>> callback) {

    }

    @Override
    public void getSession(long clientId, long sessionId, OperationResultListener<Session<T>> callback) {
        SessionsHolder holder = clients.getSessionsForClient(clientId);
        if(holder==null)
            callback.onFailed(new FailReason(MongoFailReasonsCodes.SESSION_NOT_ACTIVE));

        MongoSession session =  holder.findSessionById(sessionId);

        if(session==null)
            callback.onFailed(new FailReason(MongoFailReasonsCodes.SESSION_NOT_ACTIVE));

        callback.onSuccess(session);
    }

    @Override
    public Session<T> getSession(long clientId, long sessionId) {

        SessionsHolder holder = clients.getSessionsForClient(clientId);
        if(holder==null)
            return null;

        return holder.findSessionById(sessionId);
    }
}
