package jree.mongo_base;

import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import jree.api.*;

import java.util.Collections;
import java.util.List;

public class MongoSessionManager<T> implements SessionManager<T, String> {

    private final MongoMessageStore messageStore;
    private final MongoClientDetailsStore detailsStore;
    private final ClientsHolder clients;
    private final ConversationSubscribersHolder<T> subscribers;
    private final BodySerializer<T> serializer;

    public MongoSessionManager(MongoMessageStore messageStore, MongoClientDetailsStore detailsStore, ClientsHolder clients, ConversationSubscribersHolder<T> subscribers, BodySerializer<T> serializer) {
        this.messageStore = messageStore;
        this.detailsStore = detailsStore;
        this.clients = clients;
        this.subscribers = subscribers;
        this.serializer = serializer;
    }

    @Override
    public void createClient(long id, OperationResultListener<Boolean> callback) {
        detailsStore.addClient(id, new SingleResultCallback<UpdateResult>() {
            @Override
            public void onResult(UpdateResult updateResult, Throwable throwable) {
                if (throwable != null) {
                    callback.onFailed(new FailReason(throwable, MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                } else {
                    if (updateResult.getMatchedCount() > 0) {
                        callback.onFailed(new FailReason(MongoFailReasonsCodes.CLIENT_ALREADY_EXISTS));
                    } else {
                        callback.onSuccess(true);
                    }
                }
            }
        });
    }

    @Override
    public boolean createClient(long id) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createClient(id, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createClient(OperationResultListener<Long> callback) {
        long id = StaticFunctions.newID();
        detailsStore.addClient(id,
                new RandomClientIdGenerator(callback, id));
    }

    @Override
    public long createClient() {
        AsyncToSync<Long> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createClient(asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createClientIfNotExists(long id, OperationResultListener<Boolean> callback) {
        detailsStore.addClient(id, new AttachableConditionSingleResultCallback<UpdateResult, OperationResultListener<Boolean>>()
                .ifSuccess(IndependentSuccessCaller.call(callback, true))
                .ifFail(FailCaller.RUNTIME_FAIL_CALLER));
    }

    @Override
    public boolean createClientIfNotExists(long id) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createClientIfNotExists(id, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createSession(long clientId, OperationResultListener<Long> callback) {
        detailsStore.addSessionToClient(clientId, new AttachableConditionSingleResultCallback<Long, OperationResultListener<Long>>()
                .ifSuccess(SuccessCaller.call(callback))
                .ifFail(FailCaller.RUNTIME_FAIL_CALLER));
    }

    @Override
    public long createSession(long clientId) {
        AsyncToSync<Long> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createSession(clientId, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void connectToService(long clientId, long sessionId, RelationController<T, String> controller, SessionEventListener<T, String> ev, OperationResultListener<Session<T, String>> callback) {
        final ExceptionAdaptedEventListener<T, String>
                eventListener = new ExceptionAdaptedEventListener<>(ev);
        detailsStore.isSessionExists(
                clientId, sessionId,
                new SingleResultCallback<Boolean>() {
                    @Override
                    public void onResult(Boolean aBoolean, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                        } else if (!aBoolean) {
                            callback.onFailed(new FailReason(MongoFailReasonsCodes.SESSION_NOT_EXISTS));
                        } else {
                            MongoSession<T> session = new MongoSession<>(
                                    clientId,
                                    sessionId,
                                    eventListener,
                                    messageStore,
                                    detailsStore,
                                    clients,
                                    subscribers,
                                    serializer,
                                    controller);
                            clients.addNewSession(session);

                            detailsStore.getSessionOffset(session, new SingleResultCallback<String>() {
                                @Override
                                public void onResult(String s, Throwable throwable) {
                                    messageStore.readStoredMessage(
                                            session, s, Collections.emptyList(),
                                            serializer,
                                            session::beforeRelease,
                                            new SingleResultCallback<Void>() {
                                                @Override
                                                public void onResult(Void aVoid, Throwable throwable) {
                                                    if (throwable != null) {
                                                        //so handle it
                                                        callback.onFailed(new FailReason(MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                                                    } else {
                                                        session.release();
                                                        callback.onSuccess(session);
                                                    }
                                                }
                                            }
                                    );
                                }
                            });
                        }
                    }
                }
        );
    }

    @Override
    public Session<T, String> connectToService(long clientId, long sessionId, RelationController<T, String> controller, SessionEventListener<T, String> eventListener) {
        AsyncToSync<Session<T, String>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        connectToService(clientId, sessionId, controller, eventListener, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public boolean disconnectFromService(Session<T, String> session) {
        return false;
    }

    @Override
    public void checkPresence(List<Long> ids, OperationResultListener<List<Presence>> callback) {

    }

    @Override
    public void getSession(long clientId, long sessionId, OperationResultListener<Session<T, String>> callback) {
        SessionsHolder holder = clients.getSessionsForClient(clientId);
        if (holder == null)
            callback.onFailed(new FailReason(MongoFailReasonsCodes.SESSION_NOT_ACTIVE));

        MongoSession session = holder.findSessionById(sessionId);

        if (session == null)
            callback.onFailed(new FailReason(MongoFailReasonsCodes.SESSION_NOT_ACTIVE));

        callback.onSuccess(session);
    }

    @Override
    public Session<T, String> getSession(long clientId, long sessionId) {

        SessionsHolder holder = clients.getSessionsForClient(clientId);
        if (holder == null)
            return null;

        return holder.findSessionById(sessionId);
    }

    private final class RandomClientIdGenerator implements SingleResultCallback<UpdateResult> {

        private final OperationResultListener<Long> callback;
        private long id;

        private RandomClientIdGenerator(OperationResultListener<Long> callback, long id) {
            this.callback = callback;
            this.id = id;
        }

        @Override
        public void onResult(UpdateResult updateResult, Throwable throwable) {
            if (throwable != null) {
                callback.onFailed(new FailReason(throwable, MongoFailReasonsCodes.RUNTIME_EXCEPTION));
            } else {
                if (updateResult.getMatchedCount() > 0) {
                    id = StaticFunctions.newID();
                    detailsStore.addClient(
                            id,
                            this
                    );
                } else {
                    callback.onSuccess(id);
                }
            }
        }
    }
}
