package jree.abs;

import jree.abs.cache.RelationAndExistenceCache;
import jree.abs.funcs.AsyncToSync;
import jree.abs.funcs.ForEach;
import jree.abs.objects.PubMessageImpl;
import jree.abs.objects.PublisherImpl;
import jree.abs.objects.RecipientImpl;
import jree.abs.objects.SignalImpl;
import jree.abs.parts.DetailsStore;
import jree.abs.parts.IdBuilder;
import jree.abs.parts.SubscribersHolder;
import jree.abs.parts.interceptor.Interceptor;
import jree.abs.parts.MessageStore;
import jree.api.*;
import jree.async.ExtraStep;
import jree.async.RawTypeProviderStep;
import jree.async.Step;
import jree.async.StepByStep;
import jutils.assertion.Assertion;

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
    private final SubscribersHolder subscribers;
    private final RelationController controller;
    private List<PubMessage> pubMessages = new ArrayList<>();
    private final RelationAndExistenceCache cache;
    private boolean closed;
    private Object _sync = new Object();
    private final IdBuilder<ID> idIdBuilder;
    private final Interceptor<BODY, ID> interceptor;

    public SessionImpl(long clientId, long sessionId, SessionEventListener listener, MessageStore<BODY, ID> messageStore, DetailsStore<ID> detailsStore, ClientsHolder holder, SubscribersHolder subscribers, RelationController controller, RelationAndExistenceCache cache, IdBuilder<ID> idIdBuilder, Interceptor<BODY, ID> interceptor) {
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
        this.interceptor = interceptor;
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

    private void publishMessageToConversation(long conversation, PubMessage message) {
        subscribers.getSubscribers(conversation, new ForEach<List<Long>>() {
            @Override
            public void accept(List<Long> subscribeList) {
                for(Long clientId : subscribeList) {
                    holder.publishMessage(clientId, message);
                }
            }
        });
    }

    private void sendSignalToConversation(long conversation, Signal signal) {
        subscribers.getSubscribers(conversation, new ForEach<List<Long>>() {
            @Override
            public void accept(List<Long> subscribeList) {
                for(Long clientId : subscribeList) {
                    holder.sendSignal(clientId, signal);
                }
            }
        });
    }

    private void onPublishedMessageStoredSuccessfully(PubMessage message, OperationResultListener<PubMessage<BODY, ID>> callback) {
        if (message.type().is(PubMessage.Type.CLIENT_TO_CONVERSATION)) {
            publishMessageToConversation(message.recipient().conversation(), message);
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
            sendSignalToConversation(signal.recipient().conversation(), signal);
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
                .then(new ValidateRelationStep(recipient))
                .then(new BeforePublishMessageInterceptorStep(message, recipient))
                .then(new StoreMessageStep(message, recipient, false))
                .then(new AfterMessageSuccessfullyStored())
                .then(new OnMessagePublishInterceptorStep())
                .finish(callback)
                .execute(recipient);

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
        StepByStep.start(new GetRelationStep())
                .then(new ValidateRelationStep(recipient))
                .then(new BeforePublishMessageInterceptorStep(message, recipient))
                .then(new StoreMessageStep(message, recipient, true))
                .then(new AfterMessageSuccessfullyStored())
                .then(new OnMessagePublishInterceptorStep())
                .finish(callback)
                .execute(recipient);
        /*
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
        });*/
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
    public void subscribe(long conversation, OperationResultListener<Boolean> callback) {
        assertIfClosed();

        StepByStep.start(new BeforeSubscribeInterceptorStep())
                .then(new AddSubscribeToDetailStoreStep())
                .then(new OnSubscribeInterceptorStep(conversation))
                .then(new AfterAddSubscribeOperationDoneStep(conversation))
                .finish(callback)
                .execute(conversation);

        /*
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
        });*/
    }

    @Override
    public boolean subscribe(long subscribes) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        subscribe(subscribes, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void unsubscribe(long conversation, OperationResultListener<Boolean> callback) {
        assertIfClosed();

        StepByStep.start(new BeforeUnsubscribeInterceptorStep())
                .then(new RemoveSubscribeFromDetailsStoreStep())
                .then(new OnUnsubscribeInterceptorStep(conversation))
                .then(new AfterRemoveSubscribeOperationDoneStep(conversation))
                .finish(callback)
                .execute(conversation);
        /*
        detailsStore.removeFromSubscribeList(this.clientId, conversation, new OperationResultListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    subscribers.removeSubscriber(conversation, SessionImpl.this);
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onFailed(FailReason reason) {
                callback.onFailed(reason);
            }
        });*/
    }

    @Override
    public boolean unsubscribe(long conversations) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        unsubscribe(conversations, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void setRelationAttribute(Recipient recipient, String key, String value, OperationResultListener<Boolean> callback) {
        assertIfClosed();

        StepByStep.start(new CheckExistenceStep())
                .then(
                        new AddRelationStep()
                        .forRecipient(recipient)
                        .andKeyValue(key, value)
                )
                .finish(callback)
                .execute(recipient);

        /*
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
        });*/
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
        StepByStep.start(new GetRelationStep())
                .then(new ValidateRelationStep(recipient))
                .then(new BeforeSendSignalInterceptorStep(sig, recipient))
                .then(new SendSignalStep(sig, recipient))
                .then(new OnSignalSendInterceptorStep())
                .finish(callback)
                .execute(recipient);
        /*
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
        });*/
    }

    @Override
    public Signal<BODY> sendSignal(Recipient recipient, BODY signal) {
        AsyncToSync<Signal<BODY>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        sendSignal(recipient, signal, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void subscribeList(OperationResultListener<List<Long>> callback) {
        StepByStep.start(new GetSubscribeListStep())
                .finish(callback)
                .execute(); // null value to use !
        /*
        detailsStore.getSessionDetails(clientId, sessionId, new OperationResultListener<SessionDetails<ID>>() {
            @Override
            public void onSuccess(SessionDetails<ID> result) {
                callback.onSuccess(result.subscribeList());
            }

            @Override
            public void onFailed(FailReason reason) {
                callback.onFailed(reason);
            }
        });*/
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
            listener.onClosing(this);
            closed = true;
            if(result.isLastSession()) {
                removeSubscriptions();
            }
            interceptor.sessionInterceptor().onSessionClose(this, OperationResultListener.EMPTY_LISTENER);
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
            listener.onClosing(this);
            closed = true;
            if(result.isLastSession()) {
                removeSubscriptions();
            }
            interceptor.sessionInterceptor().onSessionClose(this, OperationResultListener.EMPTY_LISTENER);
            listener.onCloseByCommand(this);
        }
    }

    private final void removeSubscriptions() {
        subscribers.removeSubscriberFromAllConversations(this.clientId);
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

    private Publisher asPublisher() {
        return new PublisherImpl(clientId, sessionId);
    }

    private Recipient asClientRecipient() {
        return RecipientImpl.clientRecipient(clientId);
    }

    //------------------------------ shared steps ----------------------------------------


    private final class GetRelationStep extends RawTypeProviderStep<Recipient, Relation> {

        @Override
        protected void doExecute(Recipient providedValue, OperationResultListener<Relation> target) {
            cache.checkExistenceAndGetRelation(SessionImpl.this, providedValue, target);
        }
    }

    private final class ValidateRelationStep extends RawTypeProviderStep<Relation, Void> {

        private final Recipient recipient;

        private ValidateRelationStep(Recipient recipient) {
            this.recipient = recipient;
        }

        @Override
        protected void doExecute(Relation relation, OperationResultListener<Void> target) {
            try {
                if (!controller.validatePublishMessage(SessionImpl.this, recipient, relation)) {
                    target.onFailed(new FailReason(new IllegalStateException("relation failed"), RUNTIME_EXCEPTION));
                    return;
                }
            } catch (Throwable e) {
                target.onFailed(new FailReason(e, RUNTIME_EXCEPTION));
                return;
            }
            target.onSuccess(null);

        }
    }


    private final class CheckExistenceStep extends RawTypeProviderStep<Recipient, Boolean> {

        @Override
        protected void doExecute(Recipient recipient, OperationResultListener<Boolean> target) {
            cache.isExists(recipient, target);
        }

    }


    //-------------------------------------------------------------------------------------


    //----------------------------- publish message steps ---------------------------------

    private final class BeforePublishMessageInterceptorStep extends RawTypeProviderStep<Void, Void> {

        private final BODY message;
        private final Recipient recipient;

        private BeforePublishMessageInterceptorStep(BODY message, Recipient recipient) {
            this.message = message;
            this.recipient = recipient;
        }

        @Override
        protected void doExecute(Void providedValue, OperationResultListener<Void> target) {
            interceptor.messageInterceptor()
                    .beforePublishMessage(message, asPublisher(), recipient, target);
        }

    }

    private final class StoreMessageStep extends RawTypeProviderStep<Void, PubMessage<BODY, ID>> {

        private final BODY message;
        private final Recipient recipient;
        private final boolean disposable;

        private StoreMessageStep(BODY message, Recipient recipient, boolean disposable) {
            this.message = message;
            this.recipient = recipient;
            this.disposable = disposable;
        }

        @Override
        protected void doExecute(Void providedValue, OperationResultListener<PubMessage<BODY, ID>> target) {
            PubMessageImpl<BODY, ID> pubMessage = new PubMessageImpl<>(idIdBuilder.newId(), message, Instant.now(), SessionImpl.this, recipient);
            if(disposable) {
                messageStore.storeAsDisposableMessage(pubMessage, target);
            } else {
                messageStore.storeMessage(pubMessage, target);
            }
        }

    }

    private final class AfterMessageSuccessfullyStored extends RawTypeProviderStep<PubMessage<BODY,ID>, PubMessage<BODY,ID>> {

        @Override
        protected void doExecute(PubMessage<BODY, ID> providedValue, OperationResultListener<PubMessage<BODY, ID>> target) {
            onPublishedMessageStoredSuccessfully(providedValue, target);
        }
    }

    private final class OnMessagePublishInterceptorStep extends ExtraStep<PubMessage<BODY, ID> , Void> {

        @Override
        protected void executeExtraStep(PubMessage<BODY, ID> message, OperationResultListener<Void> target) {
            interceptor.messageInterceptor().onMessagePublish(message, target);
        }

    }

    //----------------------------------------------------------------------------------------

    //------------------------------- send signal steps -------------------------------------

    private final class BeforeSendSignalInterceptorStep extends RawTypeProviderStep<Void, Void> {

        private final BODY signal;
        private final Recipient recipient;

        private BeforeSendSignalInterceptorStep(BODY signal, Recipient recipient) {
            this.signal = signal;
            this.recipient = recipient;
        }

        @Override
        protected void doExecute(Void providedValue, OperationResultListener<Void> target) {
            interceptor.messageInterceptor()
                    .beforeSendSignal(signal, asPublisher(), recipient, target);
        }

    }

    private final class SendSignalStep extends RawTypeProviderStep<Void, Signal<BODY>> {

        private final BODY signal;
        private final Recipient recipient;

        private SendSignalStep(BODY signal, Recipient recipient) {
            this.signal = signal;
            this.recipient = recipient;
        }

        @Override
        protected void doExecute(Void providedValue, OperationResultListener<Signal<BODY>> target) {
            Signal<BODY> signal = new SignalImpl<>(this.signal, SessionImpl.this, recipient);
            onSignalPublished(signal, target);
        }

    }

    private final class OnSignalSendInterceptorStep extends ExtraStep<Signal<BODY>, Void> {

        @Override
        protected void executeExtraStep(Signal<BODY> signal, OperationResultListener<Void> target) {
            interceptor.messageInterceptor()
                    .onSignalSend(signal, target);
        }

    }

    //------------------------------- subscribe operations -----------------------------------

    private class BeforeSubscribeInterceptorStep extends ExtraStep<Long, Void> {

        @Override
        protected void executeExtraStep(Long conversation, OperationResultListener<Void> target) {
            interceptor.subscriptionInterceptor().beforeSubscribe(asClientRecipient(), conversation, target);
        }

    }

    private class AddSubscribeToDetailStoreStep extends RawTypeProviderStep<Long,Boolean> {

        @Override
        protected void doExecute(Long conversation, OperationResultListener<Boolean> target) {
            detailsStore.addToSubscribeList(clientId, conversation, target);
        }

    }

    private class OnSubscribeInterceptorStep extends ExtraStep<Boolean, Void> {

        private final long conversationId;

        private OnSubscribeInterceptorStep(long conversationId) {
            this.conversationId = conversationId;
        }

        @Override
        protected void executeExtraStep(Boolean result, OperationResultListener<Void> target) {
            if(result) {
                interceptor.subscriptionInterceptor().onSubscribe(
                        asClientRecipient(),
                        conversationId,
                        target
                );
            } else {
                target.onFailed(new FailReason(1212));
            }
        }

    }

    private class AfterAddSubscribeOperationDoneStep extends RawTypeProviderStep<Boolean, Boolean> {

        private final long conversation;

        private AfterAddSubscribeOperationDoneStep(long conversation) {
            this.conversation = conversation;
        }

        @Override
        protected void doExecute(Boolean providedValue, OperationResultListener<Boolean> target) {
            if(providedValue) {
                //do it means success !
                subscribers.addSubscriber(conversation, SessionImpl.this.clientId, EMPTY_LISTENER);
                target.onSuccess(true);
            } else {
                target.onSuccess(false);
            }
        }

    }

    private class BeforeUnsubscribeInterceptorStep extends ExtraStep<Long, Void> {

        @Override
        protected void executeExtraStep(Long conversationId, OperationResultListener<Void> target) {
            interceptor.subscriptionInterceptor().beforeUnsubscribe(asClientRecipient(), conversationId, target);
        }

    }

    private class RemoveSubscribeFromDetailsStoreStep extends RawTypeProviderStep<Long, Boolean> {

        @Override
        protected void doExecute(Long conversation, OperationResultListener<Boolean> target) {
            detailsStore.removeFromSubscribeList(clientId, conversation,target);
        }

    }

    private class OnUnsubscribeInterceptorStep extends ExtraStep<Boolean, Void> {

        private final long conversationId;

        private OnUnsubscribeInterceptorStep(long conversationId) {
            this.conversationId = conversationId;
        }

        @Override
        protected void executeExtraStep(Boolean result, OperationResultListener<Void> target) {
            if(result) {
                interceptor.subscriptionInterceptor().onUnsubscribe(asClientRecipient(), conversationId, target);
            } else {
                target.onFailed(new FailReason(32132));
            }
        }

    }

    private class AfterRemoveSubscribeOperationDoneStep extends RawTypeProviderStep<Boolean, Boolean> {

        private final long conversation;

        private AfterRemoveSubscribeOperationDoneStep(long conversation) {
            this.conversation = conversation;
        }

        @Override
        public void doExecute(Boolean result, OperationResultListener<Boolean> target) {
            if (result) {
                subscribers.removeSubscriber(conversation, SessionImpl.this.clientId, EMPTY_LISTENER);
                target.onSuccess(true);
            } else {
                target.onSuccess(false);
            }
        }

    }

    //---------------------------------------------------------------------------------------------


    //----------------------------------- relation steps --------------------------------------------
    private class AddRelationStep extends RawTypeProviderStep<Boolean, Boolean> {

        private Recipient recipient;
        private String key;
        private String value;

        private AddRelationStep forRecipient(Recipient recipient) {

            this.recipient = recipient;
            return this;
        }

        private AddRelationStep andKeyValue(String key, String value) {
            this.key = key;
            this.value = value;
            return this;
        }

        @Override
        protected void doExecute(Boolean isExists, OperationResultListener<Boolean> target) {
            if (isExists) {
                detailsStore.addRelation(SessionImpl.this, recipient, key, value, target);
            } else {
                target.onFailed(new FailReason(2000421421));
            }
        }

    }

    //----------------------------------- subscribe list steps---------------------------------------

    private class GetSubscribeListStep extends Step<Void, SessionDetails<ID>, List<Long>> {

        @Override
        protected void doExecute(Void providedValue, OperationResultListener<SessionDetails<ID>> target) {
            detailsStore.getSessionDetails(clientId, sessionId, target);
        }

        @Override
        protected List<Long> finished(SessionDetails<ID> result) {
            return result.subscribeList();
        }

    }
}
