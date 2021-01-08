package jree.mongo_base;

import jree.api.PubMessage;
import jree.util.Assertion;
import jree.util.concurrentiter.ConcurrentIter;
import jree.util.concurrentiter.ConcurrentIterEventListener;
import jree.util.concurrentiter.IterNode;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ConversationSubscribersHolder<T> {


    private final Function<Long , ConcurrentIter<MongoSession<T>>> creator = new Function<Long, ConcurrentIter<MongoSession<T>>>() {
        @Override
        public ConcurrentIter<MongoSession<T>> apply(Long aLong) {
            return new ConcurrentIter<MongoSession<T>>()
                    .attach(aLong)
                    .setEventListener(autoRemover);
        }
    };

    private final BiFunction<Long, ConcurrentIter<MongoSession<T>>, ConcurrentIter<MongoSession<T>>> remover = new BiFunction<Long, ConcurrentIter<MongoSession<T>>, ConcurrentIter<MongoSession<T>>>() {
        @Override
        public ConcurrentIter<MongoSession<T>> apply(Long aLong, ConcurrentIter<MongoSession<T>> mongoSessionConcurrentIter) {
            return null;
        }
    };


    private final ConcurrentIterEventListener autoRemover = new ConcurrentIterEventListener<Object>() {
        @Override
        public void afterAdd(ConcurrentIter<Object> iterator, IterNode<Object> added) {

        }

        @Override
        public void afterRemove(ConcurrentIter<Object> iterator, IterNode<Object> removed) {
            if(iterator.isEmpty())
            {
                subscribers.computeIfPresent(iterator.attachment() , remover);
            }
        }
    };



    private final HashMap<Long , ConcurrentIter<MongoSession<T>>> subscribers;


    public ConversationSubscribersHolder()
    {
        subscribers = new HashMap<>();
    }


    public synchronized ConversationSubscribersHolder<T> addSubscriber(
            Iterable<Long> conversations ,
            MongoSession<T> session
    )
    {
        for(Long l:conversations)
        {
            doAdd(l , session);
        }

        return this;
    }

    public synchronized ConversationSubscribersHolder<T> addSubscriber(
            long conversation ,
            MongoSession<T> session
    )
    {
        return doAdd(conversation , session);
    }


    private final ConversationSubscribersHolder<T> doAdd(long conversation ,
                             MongoSession<T> session)
    {

        session.addSubscriptions(conversation ,
                getOrCreate(conversation).add(session));
        return this;
    }

    public synchronized ConversationSubscribersHolder<T> addSubscriberByOffsets(
            Iterable<ConversationOffset> conversations ,
            MongoSession<T> session
    )
    {
        for(ConversationOffset offset:conversations)
        {
            if(offset.conversationId().contains("_"))
                continue;


            doAdd(Long.parseLong(offset.conversationId()) , session);
        }

        return this;
    }


    public synchronized ConversationSubscribersHolder<T> publishMessage(
            PubMessage<T> pubMessage
    ){
        Assertion.ifTrue("recipient not a conversation" , pubMessage.recipient().conversation()<0);

        ConcurrentIter<MongoSession<T>> subs = subscribers.get(pubMessage.recipient().conversation());

        if(subs==null)return this;

        subs.forEach(this::publishMessageToSubscribers , pubMessage);

        return this;
    }


    private final void publishMessageToSubscribers(MongoSession<T> session , PubMessage<T> message)
    {
        session.onMessagePublished(message);
    }







    private ConcurrentIter<MongoSession<T>> getOrCreate(long i)
    {
        return subscribers.computeIfAbsent(i, creator);
    }
}
