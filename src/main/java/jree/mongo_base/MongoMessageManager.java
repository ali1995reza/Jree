package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.internal.async.SingleResultCallback;
import jree.api.*;
import jree.util.Assertion;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;

import static jree.mongo_base.MongoFailReasonsCodes.RUNTIME_EXCEPTION;

public class MongoMessageManager<T> implements MessageManager<T , String> {

    private final static class MessageIterable<T> implements Iterable<PubMessage<T , String>> ,
            Iterator<PubMessage<T , String>> ,
            Block<PubMessage> ,
            SingleResultCallback<Void> {

        private boolean consumed = false;
        private final BlockingQueue blockingQueue = new LinkedBlockingQueue();
        private Object _sync = new Object();
        private int number = 0;
        private boolean done = false;

        @Override
        public Iterator<PubMessage<T , String>> iterator() {
            synchronized (_sync) {
                Assertion.ifTrue("this iterable used", consumed);
                consumed = true;
                return this;
            }
        }

        @Override
        public boolean hasNext() {
            try {
                synchronized (_sync) {
                    if (number <= 0 && done)
                        return false;
                    if(number==0)
                        _sync.wait();
                    if(--number<0 && done)
                        return false;

                    return true;
                }
            }catch (InterruptedException e)
            {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public PubMessage<T , String> next() {
            try {
                Object o = blockingQueue.take();
                if(o instanceof PubMessage)
                {
                    return (PubMessage<T , String>) o;
                }else
                {

                    throw new IllegalStateException((Throwable) o);
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void apply(PubMessage message) {
            blockingQueue.add(message);
            synchronized (_sync)
            {
                ++number;
                _sync.notify();
            }
        }

        @Override
        public void onResult(Void aVoid, Throwable throwable) {
            if(throwable!=null)
            {
                blockingQueue.add(throwable);
            }else {
                synchronized (_sync)
                {
                    _sync.notify();
                    done = true;
                }
            }
        }
    }




    private final static SingleResultCallback EMPTY = new SingleResultCallback() {
        @Override
        public void onResult(Object o, Throwable throwable) {

        }
    };


    private final class RandomConversationIdGenerator implements OperationResultListener<Long>
    {
        private final OperationResultListener<Long> callBack;

        private RandomConversationIdGenerator(OperationResultListener<Long> callBack) {
            this.callBack = callBack;
        }

        @Override
        public void onSuccess(Long result) {
            callBack.onSuccess(result);
        }

        @Override
        public void onFailed(FailReason reason) {
            if(reason.code() == RUNTIME_EXCEPTION)
            {
                callBack.onFailed(reason);
            }else{
                messageStore.createNewConversationIndex(
                        StaticFunctions.newID() ,
                        this
                );
            }
        }
    }



    private final ClientsHolder holder;
    private final MongoClientDetailsStore detailsStore;
    private final MongoMessageStore messageStore;
    private final BodySerializer<T> serializer;

    private final Function creator = new Function<Long, Set<Session>>() {
        @Override
        public Set<Session> apply(Long aLong) {
            return Collections.newSetFromMap(new ConcurrentHashMap<>());
        }
    };

    public MongoMessageManager(ClientsHolder holder, MongoClientDetailsStore detailsStore, MongoMessageStore messageStore, BodySerializer<T> serializer) {
        this.holder = holder;
        this.detailsStore = detailsStore;
        this.messageStore = messageStore;
        this.serializer = serializer;
    }


    @Override
    public void createConversation(long id, OperationResultListener<Long> callback) {
        messageStore.createNewConversationIndex(id , callback);
    }

    @Override
    public long createConversation(long id) {
        AsyncToSync<Long> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createConversation(id , asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createConversation(OperationResultListener<Long> callback) {
        long id = StaticFunctions.newID();
        messageStore.createNewConversationIndex(id , new RandomConversationIdGenerator(callback));
    }

    @Override
    public long createConversation() {
        AsyncToSync<Long> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createConversation(asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void readMessages(List<ReadMessageCriteria> criteria, Consumer<PubMessage<T,String>> forEach) {
        messageStore.readStoredMessageByCriteria(
                criteria,
                serializer,
                forEach::accept,
                EMPTY
        );
    }

    @Override
    public Iterable<PubMessage<T , String>> readMessages(List<ReadMessageCriteria> criteria) {
        MessageIterable<T> messageIterable = new MessageIterable<>();
        messageStore.readStoredMessageByCriteria(
                criteria ,
                serializer ,
                messageIterable ,
                messageIterable
        );

        return messageIterable;
    }

    @Override
    public Iterable<PubMessage<T , String>> readMessages(ReadMessageCriteria... criteria) {
        return readMessages(Arrays.asList(criteria));
    }
}
