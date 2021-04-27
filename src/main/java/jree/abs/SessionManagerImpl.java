package jree.abs;

import jree.abs.cache.RelationAndExistenceCache;
import jree.abs.codes.FailReasonsCodes;
import jree.abs.funcs.AsyncToSync;
import jree.abs.funcs.ForEach;
import jree.abs.objects.RecipientImpl;
import jree.abs.parts.DetailsStore;
import jree.abs.parts.IdBuilder;
import jree.abs.parts.MessageStore;
import jree.abs.utils.StaticFunctions;
import jree.api.*;

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
    public void removeClient(long id, OperationResultListener<Boolean> callback) {
        //so lets first remove from store and then close active ones !
        cache.isExists(RecipientImpl.clientRecipient(id),
                new OperationResultListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        detailsStore.removeClient(id,
                                new OperationResultListener<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean result) {
                                        if (result) {
                                            cache.removeExistenceCache(RecipientImpl.clientRecipient(id));
                                            clients.removeClientAndCloseAllSessions(id);
                                            callback.onSuccess(true);
                                        } else {
                                            //its never call with mongo model !
                                        }
                                    }

                                    @Override
                                    public void onFailed(FailReason reason) {
                                        callback.onFailed(reason);
                                    }
                                });
                    }

                    @Override
                    public void onFailed(FailReason reason) {
                        callback.onFailed(reason);
                    }
                });
    }

    @Override
    public boolean removeClient(long id) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        removeClient(id, asyncToSync);
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
        if (!canAcceptSessions)
            callback.onFailed(new FailReason(100000));

        final ExceptionAdaptedEventListener<BODY, ID> eventListener = new ExceptionAdaptedEventListener<>(ev);

        detailsStore.getSessionDetails(clientId, sessionId,
                new OperationResultListener<SessionDetails<ID>>() {
                    @Override
                    public void onSuccess(SessionDetails<ID> details) {
                        if (!details.isSessionExists()) {
                            callback.onFailed(new FailReason(
                                    FailReasonsCodes.SESSION_NOT_EXISTS));
                        } else {
                            SessionImpl<BODY, ID> session = new SessionImpl<>(
                                    clientId, sessionId, eventListener,
                                    messageStore, detailsStore, clients,
                                    subscribers, controller, cache, idBuilder);
                            eventListener.preInitialize(session);
                            ClientsHolder.AddSessionResult result = clients.addNewSession(
                                    session);
                            if (!result.isAdded()) {
                                callback.onFailed(new FailReason(
                                        "can't add session right now",
                                        FailReasonsCodes.RUNTIME_EXCEPTION));
                                return;
                            }
                            if (result.isFirstSession()) {
                                subscribers.addSubscriber(
                                        details.subscribeList(),
                                        session, EMPTY_LISTENER);
                                //if just first session add it to subscriber list
                            }
                            eventListener.onInitialized(session);
                            messageStore.readStoredMessage(session,
                                    details.offset(), details.subscribeList(),
                                    new ForEach<PubMessage<BODY, ID>>() {
                                        @Override
                                        public void accept(PubMessage<BODY, ID> message) {
                                            session.beforeRelease(message);
                                        }

                                        @Override
                                        public void done(Throwable e) {
                                            if (e != null) {
                                                //so handle it
                                                callback.onFailed(
                                                        new FailReason(e,
                                                                FailReasonsCodes.RUNTIME_EXCEPTION));
                                                clients.removeSession(session);
                                                eventListener.onClosedByException(
                                                        session, e);
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
        openSession(clientId, sessionId, controller, eventListener,
                asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void removeSession(long clientId, long sessionId, OperationResultListener<Boolean> callback) {
        cache.isExists(RecipientImpl.sessionRecipient(clientId , sessionId),
                new OperationResultListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        detailsStore.removeSession(clientId,sessionId,
                                new OperationResultListener<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean result) {
                                        if (result) {
                                            cache.removeExistenceCache(RecipientImpl.sessionRecipient(clientId, sessionId));
                                            clients.removeSessionAndCloseIt(clientId, sessionId);
                                            callback.onSuccess(true);
                                        } else {
                                            //its never call with mongo model !
                                        }
                                    }

                                    @Override
                                    public void onFailed(FailReason reason) {
                                        callback.onFailed(reason);
                                    }
                                });
                    }

                    @Override
                    public void onFailed(FailReason reason) {
                        callback.onFailed(reason);
                    }
                });
    }

    @Override
    public boolean removeSession(long clientId, long sessionId) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        removeSession(clientId, sessionId, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void getPresence(List<Long> ids, OperationResultListener<List<Presence>> callback) {
    }

    @Override
    public List<Presence> getPresence(List<Long> ids) {
        AsyncToSync<List<Presence>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        getPresence(ids, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void getSession(long clientId, long sessionId, OperationResultListener<Session<BODY, ID>> callback) {
        SessionImpl session = clients.findSessionById(clientId, sessionId);

        if (session == null)
            callback.onFailed(
                    new FailReason(FailReasonsCodes.SESSION_NOT_ACTIVE));
        callback.onSuccess(session);
    }

    @Override
    public Session<BODY, ID> getSession(long clientId, long sessionId) {
        SessionImpl session = clients.findSessionById(clientId, sessionId);

        if (session == null)
            throw new FailReason(FailReasonsCodes.SESSION_NOT_ACTIVE);

        return session;
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
