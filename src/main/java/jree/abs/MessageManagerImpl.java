package jree.abs;

import jree.abs.funcs.AsyncToSync;
import jree.abs.funcs.ForEach;
import jree.abs.parts.MessageStore;
import jree.api.*;
import jree.abs.utils.StaticFunctions;
import jutils.assertion.Assertion;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import static jree.abs.codes.FailReasonsCodes.RUNTIME_EXCEPTION;

final class MessageManagerImpl<BODY, ID extends Comparable<ID>> implements MessageManager<BODY, ID> {

    private final static class MessageIterable<BODY, ID> implements Iterable<PubMessage<BODY, ID>>, Iterator<PubMessage<BODY, ID>>, ForEach<PubMessage<BODY, ID>> {

        private boolean consumed = false;
        private final BlockingQueue blockingQueue = new LinkedBlockingQueue();
        private Object _sync = new Object();
        private int number = 0;
        private boolean done = false;

        @Override
        public Iterator<PubMessage<BODY, ID>> iterator() {
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
                    if (number <= 0 && done) {
                        return false;
                    }
                    if (number == 0) {
                        _sync.wait();
                    }
                    if (--number < 0 && done) {
                        return false;
                    }
                    return true;
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public PubMessage<BODY, ID> next() {
            try {
                Object o = blockingQueue.take();
                if (o instanceof PubMessage) {
                    return (PubMessage<BODY, ID>) o;
                } else {
                    throw new IllegalStateException((Throwable) o);
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void accept(PubMessage<BODY, ID> pubMessage) {
            blockingQueue.add(pubMessage);
            synchronized (_sync) {
                ++number;
                _sync.notify();
            }
        }

        @Override
        public void done(Throwable e) {
            if (e != null) {
                blockingQueue.add(e);
            } else {
                synchronized (_sync) {
                    _sync.notify();
                    done = true;
                }
            }
        }

    }


    private final class RandomConversationIdGenerator implements OperationResultListener<Long> {

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
            if (reason.code() == RUNTIME_EXCEPTION) {
                callBack.onFailed(reason);
            } else {
                messageStore.addConversation(StaticFunctions.newLongId(), this);
            }
        }

    }

    private final ClientsHolder holder;
    private final MessageStore messageStore;

    public MessageManagerImpl(ClientsHolder holder, MessageStore<BODY, ID> messageStore) {
        this.holder = holder;
        this.messageStore = messageStore;
    }

    @Override
    public void createConversation(long id, OperationResultListener<Long> callback) {
        messageStore.addConversation(id, callback);
    }

    @Override
    public long createConversation(long id) {
        AsyncToSync<Long> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createConversation(id, asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void createConversation(OperationResultListener<Long> callback) {
        long id = StaticFunctions.newLongId();
        messageStore.addConversation(id, new RandomConversationIdGenerator(callback));
    }

    @Override
    public long createConversation() {
        AsyncToSync<Long> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        createConversation(asyncToSync);
        return asyncToSync.getResult();
    }

    @Override
    public void readMessages(List<ReadMessageCriteria<ID>> criteria, Consumer<PubMessage<BODY, ID>> forEach) {
        messageStore.readStoredMessageByCriteria(criteria, ForEach.fromConsumer(forEach));
    }

    @Override
    public Iterable<PubMessage<BODY, ID>> readMessages(List<ReadMessageCriteria<ID>> criteria) {
        MessageIterable<BODY, ID> messageIterable = new MessageIterable<>();
        messageStore.readStoredMessageByCriteria(criteria, messageIterable);
        return messageIterable;
    }

    @Override
    public Iterable<PubMessage<BODY, ID>> readMessages(ReadMessageCriteria<ID>... criteria) {
        return readMessages(Arrays.asList(criteria));
    }

}
