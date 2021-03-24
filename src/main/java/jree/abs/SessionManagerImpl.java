package jree.abs;

import jree.abs.cache.RelationAndExistenceCache;
import jree.abs.funcs.AsyncToSync;
import jree.abs.funcs.ForEach;
import jree.abs.parts.DetailsStore;
import jree.abs.parts.IdBuilder;
import jree.abs.parts.MessageStore;
import jree.abs.utils.StaticFunctions;
import jree.api.*;
import jree.abs.codes.FailReasonsCodes;

import java.util.List;

final class SessionManagerImpl<BODY, ID extends Comparable<ID>> implements SessionManager<BODY, ID> {

    private final MessageStore<BODY, ID> messageStore;
    private final DetailsStore<ID> detailsStore;
    private final ClientsHolder clients;
    private final ConversationSubscribersHolder<BODY, ID> subscribers;
    private final RelationAndExistenceCache cache;
    private final IdBuilder<ID> idBuilder;
    private boolean close = false;
    private boolean canAcceptSessions = true;

    public SessionManagerImpl(MessageStore<BODY, ID> messageStore, DetailsStore<ID> detailsStore, ClientsHolder clients, ConversationSubscribersHolder<BODY, ID> subscribers, IdBuilder<ID> idBuilder) {
        this.messageStore = messageStore;
        this.detailsStore = detailsStore;
        this.clients = clients;
        this.subscribers = subscribers;
        this.cache = new RelationAndExistenceCache(detailsStore, messageStore);
        this.idBuilder = idBuilder;
    }

    @Override
    public void createClient(long id, OperationResultListener<Boolean> callback) {
        detailsStore.addClient(id, callback);
    }

    @Override
    public boolean createClient(long id) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createClient(id, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createClient(OperationResultListener<Long> callback) {
        long id = StaticFunctions.newLongId();
        detailsStore.addClient(id, new RandomClientIdGenerator(callback, id));
    }

    @Override
    public long createClient() {
        AsyncToSync<Long> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createClient(asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createClientIfNotExists(long id, OperationResultListener<Boolean> callback) {
        detailsStore.addClient(id, callback);
    }

    @Override
    public boolean createClientIfNotExists(long id) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createClientIfNotExists(id, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createSession(long clientId, OperationResultListener<Long> callback) {
        detailsStore.addSessionToClient(clientId, callback);
    }

    @Override
    public long createSession(long clientId) {
        AsyncToSync<Long> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createSession(clientId, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void openSession(long clientId, long sessionId, RelationController controller, SessionEventListener<BODY, ID> ev, OperationResultListener<Session<BODY, ID>> callback) {
        if(!canAcceptSessions)
            callback.onFailed(new FailReason(100000));

        if(clients.isSessionAlive(clientId , sessionId))
            callback.onFailed(new FailReason(32132132));

        final ExceptionAdaptedEventListener<BODY, ID> eventListener = new ExceptionAdaptedEventListener<>(ev);

        detailsStore.getSessionDetails(clientId, sessionId, new OperationResultListener<SessionDetails<ID>>() {
            @Override
            public void onSuccess(SessionDetails<ID> details) {
                if(!details.isSessionExists()){
                    callback.onFailed(new FailReason(FailReasonsCodes.SESSION_NOT_EXISTS));
                }else {
                    SessionImpl<BODY, ID> session = new SessionImpl<>(clientId, sessionId, eventListener, messageStore, detailsStore, clients, subscribers, controller, cache, idBuilder);
                    eventListener.preInitialize(session);
                    clients.addNewSession(session);
                    subscribers.addSubscriber(details.subscribeList() , session , EMPTY_LISTENER);
                    eventListener.onInitialized(session);
                    messageStore.readStoredMessage(session, details.offset(), details.subscribeList(), new ForEach<PubMessage<BODY, ID>>() {
                        @Override
                        public void accept(PubMessage<BODY, ID> message) {
                            session.beforeRelease(message);
                        }

                        @Override
                        public void done(Throwable e) {
                            if (e != null) {
                                //so handle it
                                callback.onFailed(new FailReason(e, FailReasonsCodes.RUNTIME_EXCEPTION));
                                clients.removeSession(session);
                                eventListener.onClosedByException(session , e);
                            } else {
                                session.release();
                                callback.onSuccess(session);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailed(FailReason reason) {
                callback.onFailed(reason);
            }
        });
    }

    @Override
    public Session<BODY, ID> openSession(long clientId, long sessionId, RelationController controller, SessionEventListener<BODY, ID> eventListener) {
        AsyncToSync<Session<BODY, ID>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        openSession(clientId, sessionId, controller, eventListener, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void checkPresence(List<Long> ids, OperationResultListener<List<Presence>> callback) {
    }

    @Override
    public void getSession(long clientId, long sessionId, OperationResultListener<Session<BODY, ID>> callback) {
        SessionsHolder holder = clients.getSessionsForClient(clientId);
        if (holder == null) {
            callback.onFailed(new FailReason(FailReasonsCodes.SESSION_NOT_ACTIVE));
        }
        SessionImpl session = holder.findSessionById(sessionId);
        if (session == null) {
            callback.onFailed(new FailReason(FailReasonsCodes.SESSION_NOT_ACTIVE));
        }
        callback.onSuccess(session);
    }

    @Override
    public Session<BODY, ID> getSession(long clientId, long sessionId) {
        SessionsHolder holder = clients.getSessionsForClient(clientId);
        if (holder == null) {
            return null;
        }
        return holder.findSessionById(sessionId);
    }

    private final class RandomClientIdGenerator implements OperationResultListener<Boolean> {

        private final OperationResultListener<Long> callback;
        private long id;

        private RandomClientIdGenerator(OperationResultListener<Long> callback, long id) {
            this.callback = callback;
            this.id = id;
        }

        @Override
        public void onSuccess(Boolean result) {
            if (!result) {
                id = StaticFunctions.newLongId();
                detailsStore.addClient(id, this);
            } else {
                callback.onSuccess(id);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            callback.onFailed(reason);
        }

    }

}
