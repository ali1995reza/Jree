package jree.mongo_base;

import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import jree.api.*;
import jree.util.Assertion;
import java.util.ArrayList;
import java.util.List;

import static jree.mongo_base.MongoFailReasonsCodes.RUNTIME_EXCEPTION;

public class MongoSession<T> extends AttachableImpl implements Session<T>, SessionContext {

    private List<PubMessage> pubMessages = new ArrayList<>();
    private final long clientId;
    private final long sessionId;
    private final SessionEventListener listener;
    private final MongoMessageStore messageStore;
    private final MongoClientDetailsStore detailsStore;
    private final ClientsHolder holder;
    private final BodySerializer<T> serializer;
    private boolean closed;


    public MongoSession(long clientId, long sessionId, SessionEventListener listener, MongoMessageStore messageStore, MongoClientDetailsStore detailsStore, ClientsHolder holder, BodySerializer<T> serializer) {
        this.clientId = clientId;
        this.sessionId = sessionId;
        this.listener = listener;
        this.messageStore = messageStore;
        this.detailsStore = detailsStore;
        this.holder = holder;
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
        assertIfClosed();
        closed = true;
        holder.removeSession(this);
        //get out kid ;))
    }

    @Override
    public void publishMessage(Recipient recipient, T message, OperationResultListener<PubMessage<T>> callback) {
        assertIfClosed();
        messageStore.storeMessage(this,
                recipient, message, serializer,
                new OperationResultListener<PubMessage>() {
                    @Override
                    public void onSuccess(PubMessage message) {
                        //detailsStore.storeMessageZeroOffset(message);
                        SessionsHolder sessionsHolder = holder.getSessionsForClient(clientId);
                        if(sessionsHolder!=null) sessionsHolder.publishMessage(message);
                        sessionsHolder = holder.getSessionsForClient(recipient.client());
                        if(sessionsHolder!=null) sessionsHolder.publishMessage(message);
                        callback.onSuccess(message);
                    }

                    @Override
                    public void onFailed(FailReason reason) {
                        callback.onFailed(reason);
                    }
                });
    }

    @Override
    public PubMessage<T> publishMessage(Recipient recipient, T message) {
        AsyncToSync<PubMessage<T>> syncToAsync = SharedAsyncToSync.shared().get().refresh();
        publishMessage(recipient, message , syncToAsync);
        return syncToAsync.getResult();
    }

    @Override
    public void publishDisposableMessage(Recipient recipient, T message, OperationResultListener<PubMessage<T>> callback) {
        assertIfClosed();
        messageStore.storeDisposableMessage(this,
                recipient, message, serializer,
                new OperationResultListener<PubMessage>() {
                    @Override
                    public void onSuccess(PubMessage message) {
                        //detailsStore.storeMessageZeroOffset(message);
                        SessionsHolder sessionsHolder = holder.getSessionsForClient(clientId);
                        if(sessionsHolder!=null) sessionsHolder.publishMessage(message);
                        sessionsHolder = holder.getSessionsForClient(recipient.client());
                        if(sessionsHolder!=null) sessionsHolder.publishMessage(message);
                        callback.onSuccess(message);
                    }

                    @Override
                    public void onFailed(FailReason reason) {
                        callback.onFailed(reason);
                    }
                });
    }

    @Override
    public PubMessage<T> publishDisposableMessage(Recipient recipient, T message) {
        AsyncToSync<PubMessage<T>> asyncToSync = SharedAsyncToSync.shared().get().refresh();
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
    public void subscribe(List<Subscribe> subscribes, OperationResultListener<Boolean> callback) {
        assertIfClosed();
        messageStore.getMessageIds(subscribes, new SingleResultCallback<List<MessageIndex>>() {
            @Override
            public void onResult(List<MessageIndex> messageIndexes, Throwable throwable) {
                if(throwable!=null)
                {
                    callback.onFailed(new FailReason(throwable , RUNTIME_EXCEPTION));
                }else {

                    for(MessageIndex index:messageIndexes)
                    {
                        index.decreaseMessageIndex(1);
                    }

                    detailsStore.storeMessageOffset(MongoSession.this,
                            false, messageIndexes,
                            new SingleResultCallback<InsertManyResult>() {
                                @Override
                                public void onResult(InsertManyResult insertManyResult, Throwable throwable) {

                                    if(throwable!=null)
                                    {
                                        callback.onFailed(new FailReason(throwable , RUNTIME_EXCEPTION));
                                    }else {
                                        callback.onSuccess(true);
                                    }
                                }
                            });
                }
            }
        });

    }

    @Override
    public boolean subscribe(List<Subscribe> subscribes) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        subscribe(subscribes , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void unsubscribe(List<Long> conversations, OperationResultListener<Boolean> result) {

    }

    @Override
    public boolean unsubscribe(List<Long> conversations) {
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

    public SessionEventListener eventListener() {
        return listener;
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
