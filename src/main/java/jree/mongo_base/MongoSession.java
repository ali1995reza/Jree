package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import jree.api.*;
import jree.util.Assertion;
import jree.util.concurrentiter.IterNode;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static jree.mongo_base.MongoFailReasonsCodes.ALREADY_SUBSCRIBER;
import static jree.mongo_base.MongoFailReasonsCodes.RUNTIME_EXCEPTION;

public class MongoSession<T> extends SimpleAttachable implements Session<T , String>, SessionContext {

    private List<PubMessage> pubMessages = new ArrayList<>();
    private final long clientId;
    private final long sessionId;
    private final SessionEventListener listener;
    private final MongoMessageStore messageStore;
    private final MongoClientDetailsStore detailsStore;
    private final ClientsHolder holder;
    private final ConversationSubscribersHolder<T> subscribers;
    private final BodySerializer<T> serializer;
    private boolean closed;
    private Object _sync = new Object();


    public MongoSession(long clientId, long sessionId, SessionEventListener listener, MongoMessageStore messageStore, MongoClientDetailsStore detailsStore, ClientsHolder holder, ConversationSubscribersHolder<T> subscribers, BodySerializer<T> serializer) {
        this.clientId = clientId;
        this.sessionId = sessionId;
        this.listener = listener;
        this.messageStore = messageStore;
        this.detailsStore = detailsStore;
        this.holder = holder;
        this.subscribers = subscribers;
        this.serializer = serializer;
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


    private void onPublishedMessageStoredSuccessfully(PubMessage message , OperationResultListener<PubMessage<T , String>> callback)
    {
        if(message.type().is(PubMessage.Type.CLIENT_TO_CONVERSATION))
        {
            subscribers.publishMessage(message);
            callback.onSuccess(message);
        }else {
            SessionsHolder sessionsHolder = holder.getSessionsForClient(clientId);
            if (sessionsHolder != null) sessionsHolder.publishMessage(message);
            sessionsHolder = holder.getSessionsForClient(message.recipient().client());
            if (sessionsHolder != null) sessionsHolder.publishMessage(message);
            callback.onSuccess(message);
        }
    }

    @Override
    public void publishMessage(Recipient recipient, T message, OperationResultListener<PubMessage<T , String>> callback) {
        assertIfClosed();
        messageStore.storeMessage(this,
                recipient, message, serializer,
                new ConditionOperationResultListener<PubMessage, OperationResultListener<PubMessage<T , String>>>()
                        .attach(callback)
        .ifSuccess(this::onPublishedMessageStoredSuccessfully)
        .ifFail(new BiConsumer<FailReason, OperationResultListener<PubMessage<T , String>>>() {
            @Override
            public void accept(FailReason failReason, OperationResultListener<PubMessage<T , String>> pubMessageOperationResultListener) {
                callback.onFailed(failReason);
            }
        }));
    }

    @Override
    public PubMessage<T , String> publishMessage(Recipient recipient, T message) {
        AsyncToSync<PubMessage<T , String>> syncToAsync = SharedAsyncToSync.shared().get().refresh();
        publishMessage(recipient, message , syncToAsync);
        return syncToAsync.getResult();
    }

    @Override
    public void editMessage(Recipient recipient, String messageId, T newMessage, OperationResultListener<PubMessage<T , String>> result) {
        messageStore.updateMessage(this , recipient , messageId , newMessage , serializer, result);
    }

    @Override
    public PubMessage<T , String> editMessage(Recipient recipient, String messageId, T newMessage) {
        AsyncToSync<PubMessage<T , String>> syncToAsync = SharedAsyncToSync.shared().get().refresh();
        editMessage(recipient, messageId, newMessage , syncToAsync);
        return syncToAsync.getResult();
    }

    @Override
    public void removeMessage(Recipient recipient, String messageId, OperationResultListener<PubMessage<T , String>> result) {
        messageStore.removeMessage(this , recipient , messageId , serializer , result);
    }

    @Override
    public PubMessage<T , String> removeMessage(Recipient recipient, String messageId) {
        AsyncToSync<PubMessage<T , String>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        messageStore.removeMessage(this , recipient , messageId , serializer , asyncToSync);

        return asyncToSync.getResult();

    }

    @Override
    public void publishDisposableMessage(Recipient recipient, T message, OperationResultListener<PubMessage<T , String>> callback) {
        assertIfClosed();
        messageStore.storeDisposableMessage(this,
                recipient, message, serializer,
                new OperationResultListener<PubMessage>() {
                    @Override
                    public void onSuccess(PubMessage message) {
                        if(message.type().is(PubMessage.Type.CLIENT_TO_CONVERSATION))
                        {
                            subscribers.publishMessage(message);
                        }else {
                            SessionsHolder sessionsHolder = holder.getSessionsForClient(clientId);
                            if (sessionsHolder != null) sessionsHolder.publishMessage(message);
                            sessionsHolder = holder.getSessionsForClient(recipient.client());
                            if (sessionsHolder != null) sessionsHolder.publishMessage(message);
                            callback.onSuccess(message);
                        }
                    }

                    @Override
                    public void onFailed(FailReason reason) {
                        callback.onFailed(reason);
                    }
                });
    }

    @Override
    public PubMessage<T , String> publishDisposableMessage(Recipient recipient, T message) {
        AsyncToSync<PubMessage<T , String>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        publishDisposableMessage(recipient , message , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void addTag(Recipient recipient, InsertTag tag, OperationResultListener<InsertTagResult> result) {
        assertIfClosed();
        detailsStore.setTag(this,
                recipient ,
                tag,
                new SingleResultCallback<InsertTagResult>() {
                    @Override
                    public void onResult(InsertTagResult insertTagResult, Throwable throwable) {
                        if(throwable!=null)
                        {
                            result.onFailed(new FailReason(throwable ,
                                    MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                        }else {
                            result.onSuccess(insertTagResult);
                        }
                    }
                }
        );
    }

    @Override
    public InsertTagResult addTag(Recipient recipient, InsertTag tag) {
        AsyncToSync<InsertTagResult> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        addTag(recipient, tag , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void setMessageOffset(Recipient recipient, long offset, OperationResultListener<Boolean> callback) {
        assertIfClosed();
        detailsStore.setMessageOffset(
                this,
                recipient,
                offset,
                new SingleResultCallback<UpdateResult>() {
                    @Override
                    public void onResult(UpdateResult updateResult, Throwable throwable) {
                        if(throwable!=null)
                        {
                            callback.onFailed(new FailReason(throwable , RUNTIME_EXCEPTION));
                        }else {
                            callback.onSuccess(true);
                        }
                    }
                }
        );
    }

    @Override
    public boolean setMessageOffset(Recipient recipient, long offset) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        setMessageOffset(recipient, offset , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void subscribe(Subscribe subscribe, OperationResultListener<Boolean> callback) {
        assertIfClosed();


        messageStore.getCurrentMessageId(String.valueOf(subscribe.conversation()), new SingleResultCallback<Long>() {
            @Override
            public void onResult(Long aLong, Throwable throwable) {
                if(throwable!=null)
                {
                    callback.onFailed(new FailReason(throwable , RUNTIME_EXCEPTION));
                }else {

                    final long messageIndex =
                            Math.max(0 , aLong-subscribe.option().lastMessages());

                    detailsStore.storeMessageOffset(
                            MongoSession.this,
                            subscribe.option().justThiSession(),
                            String.valueOf(subscribe.conversation()),
                            messageIndex,
                            new AttachableConditionSingleResultCallback<UpdateResult,OperationResultListener<Boolean>>()
                            .attach(callback)
                            .ifSuccess(((updateResult, booleanOperationResultListener) -> {
                                if(updateResult.getMatchedCount()<1 && updateResult.getUpsertedId()==null)
                                {
                                    callback.onFailed(new FailReason(RUNTIME_EXCEPTION));
                                }else if(updateResult.getMatchedCount()==1 && updateResult.getModifiedCount()==0){
                                    callback.onFailed(new FailReason(ALREADY_SUBSCRIBER));
                                }else {

                                    subscribers.addSubscriber(subscribe.conversation() ,
                                            MongoSession.this , OperationResultListener.EMPTY_LISTENER);

                                    callback.onSuccess(true);

                                    messageStore.readStoredMessage(new ConversationOffset(String.valueOf(subscribe.conversation()) , messageIndex), serializer
                                            , MongoSession.this::onMessagePublished
                                            , new ConditionSingleResultCallback<Void>()
                                                    .ifFail(MongoSession.this::closeByException));
                                }
                            }))
                            .ifFail(FailCaller.RUNTIME_FAIL_CALLER)
                    );
                }


            }
        });


    }


    @Override
    public boolean subscribe(Subscribe subscribes) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        subscribe(subscribes , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void unsubscribe(long conversations, OperationResultListener<Boolean> result) {

    }

    @Override
    public boolean unsubscribe(long conversations) {
        return false;
    }


    public void onMessagePublished(PubMessage message)
    {
        if(pubMessages!=null) {
            synchronized (pubMessages) {
                if(pubMessages!=null) {
                    pubMessages.add(message);
                }else
                {
                    listener.onMessagePublished(this , message);
                }
            }
        }else {


            listener.onMessagePublished(this , message);
        }
    }

    void beforeRelease(PubMessage message)
    {
        listener.onMessagePublished(this ,message);
    }

    void assertIfClosed()
    {
        Assertion.ifTrue("session closed" , closed);
    }

    public void release()
    {
        synchronized (pubMessages)
        {
            for(PubMessage message:pubMessages)
            {
                listener.onMessagePublished(this , message);
            }

            pubMessages.clear();
            pubMessages = null;
        }
    }

    void closeByException(Throwable t)
    {
        synchronized (_sync) {
            if(closed)return;
            closed = true;
            holder.removeSession(this);
            removeSubscriptions();
            listener.onClosedByException(this, t);
        }
    }

    private void closeByCommand()
    {
        synchronized (_sync) {
            if(closed)return;
            closed = true;
            holder.removeSession(this);
            removeSubscriptions();
            listener.onCloseByCommand(this);
        }
    }

    private final void removeSubscriptions()
    {
        subscribers.remove(this , OperationResultListener.EMPTY_LISTENER);
    }

    void preInitialized()
    {
        listener.preInitialize(this);
    }

    void onInitialized()
    {
        listener.onInitialized(this);
    }




    @Override
    public Session currentSession() {
        return this;
    }

    @Override
    public String toString() {
        return "MongoSession{" +
                "clientId=" + clientId +
                ", sessionId=" + sessionId +
                '}';
    }
}
