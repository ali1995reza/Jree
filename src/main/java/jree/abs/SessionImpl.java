package jree.abs;

import jree.abs.cache.RelationAndExistenceCache;
import jree.abs.funcs.AsyncToSync;
import jree.abs.objects.PubMessageImpl;
import jree.abs.objects.SignalImpl;
import jree.abs.parts.DetailsStore;
import jree.abs.parts.IdBuilder;
import jree.abs.parts.MessageStore;
import jree.api.*;
import jree.async.RawTypeProviderStep;
import jree.async.StepByStep;
import jree.util.Assertion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static jree.abs.codes.FailReasonsCodes.RUNTIME_EXCEPTION;

final class SessionImpl<BODY, ID extends Comparable<ID>> extends SimpleAttachable implements Session<BODY, ID>, SessionContext {

    private final long clientId;
    private final long sessionId;
    private final SessionEventListener listener;
    private final MessageStore<BODY, ID> messageStore;
    private final DetailsStore<ID> detailsStore;
    private final ClientsHolder holder;
    private final ConversationSubscribersHolder<BODY, ID> subscribers;
    private final RelationController controller;
    private List<PubMessage> pubMessages = new ArrayList<>();
    private final RelationAndExistenceCache cache;
    private boolean closed;
    private Object _sync = new Object();
    private final IdBuilder<ID> idIdBuilder;

    public SessionImpl(long clientId, long sessionId, SessionEventListener listener, MessageStore<BODY, ID> messageStore, DetailsStore<ID> detailsStore, ClientsHolder holder, ConversationSubscribersHolder<BODY, ID> subscribers, RelationController controller, RelationAndExistenceCache cache, IdBuilder<ID> idIdBuilder) {
        this.clientId = clientId;
        this.sessionId = sessionId;
        this.listener = listener;
        this.messageStore = messageStore;
        this.detailsStore = detailsStore;
        this.holder = holder;
        this.subscribers = subscribers;
        this.controller = controller;
        this.cache = cache;
        this.idIdBuilder = idIdBuilder;
    }

    @Override
    public long clientId() {
        return clientId;
    }

    @Override
    public long id() {
        return sessionId;
    }

    @Override
    public void close() {
        closeByCommand();
    }

    private void onPublishedMessageStoredSuccessfully(PubMessage message, OperationResultListener<PubMessage<BODY, ID>> callback) {
        if (message.type().is(PubMessage.Type.CLIENT_TO_CONVERSATION)) {
            subscribers.publishMessage(message);
        } else if (message.type().is(PubMessage.Type.CLIENT_TO_CLIENT)) {
            holder.publishMessage(clientId, message);
            holder.publishMessage(message.recipient().client(), message);
        } else {
            onMessagePublished(message);
            holder.publishMessage(message.recipient().client(), message);
        }
        callback.onSuccess(message);
    }

    private void onSignalPublished(Signal<BODY> signal, OperationResultListener<Signal<BODY>> callback) {
        if (signal.recipient().conversation() > 0) {
            subscribers.sendSignal(signal);
            callback.onSuccess(signal);
        } else {
            holder.sendSignal(signal.recipient().client(), signal);
            callback.onSuccess(signal);
        }
    }

    @Override
    public void publishMessage(Recipient recipient, BODY message, OperationResultListener<PubMessage<BODY, ID>> callback) {
        assertIfClosed();
        StepByStep.start(new GetRelationStep())
                .then(new ValidationAndStoreMessageStep(message, recipient, false))
                .then(new AfterMessageSuccessfullyStored())
                .finish(callback);

        /*cache.checkExistenceAndGetRelation(this, recipient, new OperationResultListener<Relation>() {
            @Override
            public void onSuccess(Relation relation) {
                try {
                    if (!controller.validatePublishMessage(SessionImpl.this, recipient, relation)) {
                        callback.onFailed(new FailReason(new IllegalStateException("relation failed"), RUNTIME_EXCEPTION));
                        return;
                    }
                } catch (Throwable e) {
                    callback.onFailed(new FailReason(e, RUNTIME_EXCEPTION));
                    return;
                }
                PubMessageImpl<BODY, ID> pubMessage = new PubMessageImpl<>(idIdBuilder.newId(), message, Instant.now(), SessionImpl.this, recipient);
                messageStore.storeMessage(pubMessage, new OperationResultListener<PubMessage<BODY, ID>>() {
                    @Override
                    public void onSuccess(PubMessage<BODY, ID> result) {
                        onPublishedMessageStoredSuccessfully(result, callback);
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
        });*/
    }

    @Override
    public PubMessage<BODY, ID> publishMessage(Recipient recipient, BODY message) {
        AsyncToSync<PubMessage<BODY, ID>> syncToAsync = SharedAsyncToSync.shared().get().refresh();
        publishMessage(recipient, message, syncToAsync);
        return syncToAsync.getResult();
    }

    @Override
    public void editMessage(Recipient recipient, ID messageId, BODY newMessage, OperationResultListener<PubMessage<BODY, ID>> result) {
        messageStore.updateMessage(this, recipient, messageId, newMessage, result);
    }

    @Override
    public PubMessage<BODY, ID> editMessage(Recipient recipient, ID messageId, BODY newMessage) {
        AsyncToSync<PubMessage<BODY, ID>> syncToAsync = SharedAsyncToSync.shared().get().refresh();
        editMessage(recipient, messageId, newMessage, syncToAsync);
        return syncToAsync.getResult();
    }

    @Override
    public void removeMessage(Recipient recipient, ID messageId, OperationResultListener<PubMessage<BODY, ID>> result) {
        messageStore.removeMessage(this, recipient, messageId, result);
    }

    @Override
    public PubMessage<BODY, ID> removeMessage(Recipient recipient, ID messageId) {
        AsyncToSync<PubMessage<BODY, ID>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        messageStore.removeMessage(this, recipient, messageId, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void publishDisposableMessage(Recipient recipient, BODY message, OperationResultListener<PubMessage<BODY, ID>> callback) {
        assertIfClosed();
        PubMessageImpl<BODY, ID> pubMessage = new PubMessageImpl<>(idIdBuilder.newId(), message, Instant.now(), SessionImpl.this, recipient);
        cache.checkExistenceAndGetRelation(this, recipient, new OperationResultListener<Relation>() {
            @Override
            public void onSuccess(Relation relation) {
                try {
                    if (!controller.validatePublishMessage(SessionImpl.this, recipient, relation)) {
                        callback.onFailed(new FailReason(new IllegalStateException("relation failed"), RUNTIME_EXCEPTION));
                        return;
                    }
                } catch (Throwable e) {
                    callback.onFailed(new FailReason(e, RUNTIME_EXCEPTION));
                    return;
                }
                PubMessageImpl<BODY, ID> pubMessage = new PubMessageImpl<>(idIdBuilder.newId(), message, Instant.now(), SessionImpl.this, recipient);
                messageStore.storeAsDisposableMessage(pubMessage, new OperationResultListener<PubMessage<BODY, ID>>() {
                    @Override
                    public void onSuccess(PubMessage<BODY, ID> result) {
                        onPublishedMessageStoredSuccessfully(result, callback);
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
    public PubMessage<BODY, ID> publishDisposableMessage(Recipient recipient, BODY message) {
        AsyncToSync<PubMessage<BODY, ID>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        publishDisposableMessage(recipient, message, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void addTag(Recipient recipient, InsertTag tag, OperationResultListener<Tag> result) {
        assertIfClosed();
        messageStore.setTag(this, recipient, tag, result);
    }

    @Override
    public Tag addTag(Recipient recipient, InsertTag tag) {
        AsyncToSync<Tag> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        addTag(recipient, tag, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void setMessageOffset(ID offset, OperationResultListener<Boolean> callback) {
        assertIfClosed();
        detailsStore.setSessionOffset(this.clientId, this.sessionId, offset, callback);
    }

    @Override
    public boolean setMessageOffset(ID offset) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        setMessageOffset(offset, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void subscribe(long subscribe, OperationResultListener<Boolean> callback) {
        assertIfClosed();
        detailsStore.addToSubscribeList(clientId, subscribe, new OperationResultListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    subscribers.addSubscriber(subscribe, SessionImpl.this);
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onFailed(FailReason reason) {
                callback.onFailed(reason);
            }
        });
    }

    @Override
    public boolean subscribe(long subscribes) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        subscribe(subscribes, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void unsubscribe(long conversations, OperationResultListener<Boolean> callback) {
        detailsStore.removeFromSubscribeList(this.clientId, conversations, new OperationResultListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    subscribers.removeSubscriber(conversations, SessionImpl.this);
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onFailed(FailReason reason) {
                callback.onFailed(reason);
            }
        });
    }

    @Override
    public boolean unsubscribe(long conversations) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        unsubscribe(conversations, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void setRelationAttribute(Recipient recipient, String key, String value, OperationResultListener<Boolean> result) {
        cache.isExists(recipient, new OperationResultListener<Boolean>() {
            @Override
            public void onSuccess(Boolean isExists) {
                if (isExists) {
                    detailsStore.addRelation(SessionImpl.this, recipient, key, value, result);
                } else {
                    result.onFailed(new FailReason(2000421421));
                }
            }

            @Override
            public void onFailed(FailReason reason) {
                result.onFailed(reason);
            }
        });
    }

    @Override
    public boolean setRelationAttribute(Recipient recipient, String key, String value) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        setRelationAttribute(recipient, key, value, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void sendSignal(Recipient recipient, BODY sig, OperationResultListener<Signal<BODY>> callback) {
        assertIfClosed();
        cache.checkExistenceAndGetRelation(this, recipient, new OperationResultListener<Relation>() {
            @Override
            public void onSuccess(Relation relation) {
                try {
                    if (!controller.validatePublishMessage(SessionImpl.this, recipient, relation)) {
                        callback.onFailed(new FailReason(new IllegalStateException("relation failed"), RUNTIME_EXCEPTION));
                        return;
                    }
                } catch (Throwable e) {
                    callback.onFailed(new FailReason(e, RUNTIME_EXCEPTION));
                    return;
                }
                Signal<BODY> signal = new SignalImpl<>(sig, SessionImpl.this, recipient);
                onSignalPublished(signal, callback);
            }

            @Override
            public void onFailed(FailReason reason) {
                callback.onFailed(reason);
            }
        });
    }

    @Override
    public Signal<BODY> sendSignal(Recipient recipient, BODY signal) {
        AsyncToSync<Signal<BODY>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        sendSignal(recipient, signal, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void subscribeList(OperationResultListener<List<Long>> callback) {
        detailsStore.getSessionDetails(clientId, sessionId, new OperationResultListener<SessionDetails<ID>>() {
            @Override
            public void onSuccess(SessionDetails<ID> result) {
                callback.onSuccess(result.subscribeList());
            }

            @Override
            public void onFailed(FailReason reason) {
                callback.onFailed(reason);
            }
        });
    }

    @Override
    public List<Long> subscribeList() {
        AsyncToSync<List<Long>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        subscribeList(asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public List<Recipient> recipientsList() {
        AsyncToSync<List<Recipient>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        recipientsList(asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void recipientsList(OperationResultListener<List<Recipient>> callback) {
        messageStore.getRecipientsList(clientId, sessionId, callback);
    }

    public void onMessagePublished(PubMessage message) {
        if (pubMessages != null) {
            synchronized (pubMessages) {
                if (pubMessages != null) {
                    pubMessages.add(message);
                } else {
                    listener.onMessagePublished(this, message);
                }
            }
        } else {
            listener.onMessagePublished(this, message);
        }
    }

    public void onSignalReceived(Signal<BODY> signal) {
        listener.onSignalReceived(this, signal);
    }

    void beforeRelease(PubMessage message) {
        listener.onMessagePublished(this, message);
    }

    void assertIfClosed() {
        Assertion.ifTrue("session closed", closed);
    }

    public void release() {
        synchronized (pubMessages) {
            for (PubMessage message : pubMessages) {
                listener.onMessagePublished(this, message);
            }
            pubMessages.clear();
            pubMessages = null;
        }
    }

    void closeByException(Throwable t) {
        synchronized (_sync) {
            if (closed) {
                return;
            }
            ClientsHolder.RemoveSessionResult result = holder.removeSession(this);
            if(!result.isRemoved())
                throw new IllegalStateException("can not remove session - FATAL");
            closed = true;
            if(result.isLastSession()) {
                removeSubscriptions();
            }
            listener.onClosedByException(this, t);
        }
    }

    private void closeByCommand() {
        synchronized (_sync) {
            if (closed) {
                return;
            }
            ClientsHolder.RemoveSessionResult result = holder.removeSession(this);
            if(!result.isRemoved())
                throw new IllegalStateException("can not remove session - FATAL");
            closed = true;
            if(result.isLastSession()) {
                removeSubscriptions();
            }
            listener.onCloseByCommand(this);
        }
    }

    private final void removeSubscriptions() {
        subscribers.remove(this, OperationResultListener.EMPTY_LISTENER);
    }

    void preInitialized() {
        listener.preInitialize(this);
    }

    void onInitialized() {
        listener.onInitialized(this);
    }

    @Override
    public Session currentSession() {
        return this;
    }

    @Override
    public String toString() {
        return "MongoSession{" + "clientId=" + clientId + ", sessionId=" + sessionId + '}';
    }



    //------------------------------ shared steps ----------------------------------------


    private final class GetRelationStep extends RawTypeProviderStep<Recipient, Relation> {

        @Override
        public void doExecute(Recipient providedValue, OperationResultListener<Relation> target) {
            cache.checkExistenceAndGetRelation(SessionImpl.this, providedValue, target);
        }
    }


    //-------------------------------------------------------------------------------------


    //----------------------------- publish message steps ---------------------------------
    private final class ValidationAndStoreMessageStep extends RawTypeProviderStep<Relation, PubMessage<BODY,ID>> {

        private final BODY message;
        private final Recipient recipient;
        private final boolean disposable;

        private ValidationAndStoreMessageStep(BODY message, Recipient recipient, boolean disposable) {
            this.message = message;
            this.recipient = recipient;
            this.disposable = disposable;
        }

        @Override
        public void doExecute(Relation relation, OperationResultListener<PubMessage<BODY, ID>> target) {
            try {
                if (!controller.validatePublishMessage(SessionImpl.this, recipient, relation)) {
                    target.onFailed(new FailReason(new IllegalStateException("relation failed"), RUNTIME_EXCEPTION));
                    return;
                }
            } catch (Throwable e) {
                target.onFailed(new FailReason(e, RUNTIME_EXCEPTION));
                return;
            }
            PubMessageImpl<BODY, ID> pubMessage = new PubMessageImpl<>(idIdBuilder.newId(), message, Instant.now(), SessionImpl.this, recipient);
            if(disposable) {
                messageStore.storeAsDisposableMessage(pubMessage, target);
            } else {
                messageStore.storeMessage(pubMessage, target);
            }
        }
    }

    private final class AfterMessageSuccessfullyStored extends RawTypeProviderStep< PubMessage<BODY,ID>, PubMessage<BODY,ID>> {

        @Override
        public void doExecute(PubMessage<BODY, ID> providedValue, OperationResultListener<PubMessage<BODY, ID>> target) {
            onPublishedMessageStoredSuccessfully(providedValue, target);
        }
    }
    //----------------------------------------------------------------------------------------


}
