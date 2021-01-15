package jree.mongo_base.metakeeper;

import org.checkerframework.checker.units.qual.K;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LocalHashMapKeeper implements AbstractMetaKeeper.LocalKeeper {



    private final static class Key{

        public static Key forClient(long client)
        {
            return new Key(client , -1 , -1);
        }

        public static Key forSession(long client , long session)
        {
            return new Key(client , session , -1);
        }

        public static Key forConversation(long conversation)
        {
            return new Key(-1 , -1 , conversation);
        }


        private final long client;
        private final long session;
        private final long conversation;


        private Key(long client, long session, long conversation) {
            this.client = client;
            this.session = session;
            this.conversation = conversation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return client == key.client &&
                    session == key.session &&
                    conversation == key.conversation;
        }

        @Override
        public int hashCode() {
            return Objects.hash(client, session, conversation);
        }
    }

    private final ConcurrentHashMap<Key , Boolean> existMap;

    public LocalHashMapKeeper() {
        this.existMap = new ConcurrentHashMap<>();
    }

    @Override
    public Boolean isSessionExists(long client, long session) {
        return existMap.get(Key.forSession(client, session));
    }


    @Override
    public Boolean isClientExists(long client) {
        return existMap.get(Key.forClient(client));
    }

    @Override
    public Boolean isConversationExists(long conversation) {
        return existMap.get(Key.forConversation(conversation));
    }


    @Override
    public void keepClient(long client, boolean exists) {
        existMap.put(Key.forClient(client) , exists);
    }

    @Override
    public void keepConversation(long conversation, boolean exists) {

        existMap.put(Key.forConversation(conversation) , exists);
    }

    @Override
    public void keepSession(long client, long session, boolean exists) {

        existMap.put(Key.forSession(client , session) , exists);
    }
}
