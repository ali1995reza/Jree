package jree.mongo_base;

import jree.api.PubMessage;
import jree.util.Assertion;

import java.util.*;
import java.util.function.Function;

public class ConversationSubscribersHolder<T> {

    private final HashMap<Long , HashSet<MongoSession<T>>> subscribers;


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
            getOrCreate(l).add(session);
        }

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


            getOrCreate(Long.parseLong(offset.conversationId())).add(session);
        }

        return this;
    }

    private final List<Long> clearSubs = new ArrayList<>();
    public synchronized ConversationSubscribersHolder<T> removeSubscriber(
            MongoSession<T> session
    ){

        for(Map.Entry<Long , HashSet<MongoSession<T>>> entry:subscribers.entrySet())
        {
            entry.getValue().remove(session);

            if(entry.getValue().isEmpty())
            {
                clearSubs.add(entry.getKey());
            }
        }

        for(Long l:clearSubs)
        {
            subscribers.remove(l);
        }

        clearSubs.clear();

        return this;
    }

    public synchronized ConversationSubscribersHolder<T> publishMessage(
            PubMessage<T> pubMessage
    ){
        Assertion.ifTrue("recipient not a conversation" , pubMessage.recipient().conversation()<0);

        HashSet<MongoSession<T>> subs = subscribers.get(pubMessage.recipient().conversation());

        if(subs==null)return this;

        for(MongoSession<T> session:subs)
        {
            session.onMessagePublished(pubMessage);
        }

        return this;
    }




    private final Function<Long , HashSet<MongoSession<T>>> creator = new Function<Long, HashSet<MongoSession<T>>>() {
        @Override
        public HashSet<MongoSession<T>> apply(Long aLong) {
            return new HashSet<>();
        }
    };


    private HashSet<MongoSession<T>> getOrCreate(long i)
    {
        return subscribers.computeIfAbsent(i, creator);
    }
}
