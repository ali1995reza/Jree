package jree.mongo_base;

import jree.api.Relation;

import java.util.Collections;
import java.util.Map;

public class MongoRelation implements Relation {

    static class Holder {

        final static Holder clientHolder(long id, Map<String, String> map) {
            return new Holder(map, true, id);
        }

        final static Holder conversationHolder(long id, Map<String, String> map) {
            return new Holder(map, false, id);
        }

        private final Map<String, String> map;
        private final boolean isClient;
        private final long id;

        private Holder(Map<String, String> map, boolean isClient, long id) {
            this.map = Collections.unmodifiableMap(map);
            this.isClient = isClient;
            this.id = id;
        }
    }

    private final Holder a;
    private final Holder b;

    public MongoRelation(Holder a, Holder b) {
        this.a = a;
        this.b = b;
    }

    public MongoRelation(Holder a) {
        this(a, null);
    }

    @Override
    public Map<String, String> setByClient(long clientId) {
        if (a != null && a.isClient && a.id == clientId)
            return a.map;
        else if (b != null && b.isClient && b.id == clientId)
            return b.map;

        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> setByConversation(long conversationId) {

        if (a != null && !a.isClient && a.id == conversationId)
            return a.map;
        else if (b != null && !b.isClient && b.id == conversationId)
            return b.map;

        return Collections.emptyMap();
    }
}
