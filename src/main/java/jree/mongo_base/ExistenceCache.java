package jree.mongo_base;

import org.ehcache.Cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ExistenceCache {

    private final static class ClientsExistence{
        private final long client;
        private boolean exists;
        private final Map<Long,Boolean> sessions;

        private ClientsExistence(long client , boolean exists) {
            this.client = client;
            this.exists = exists;
            this.sessions = new HashMap<>();
        }

        public void setExists(boolean exists) {
            this.exists = exists;
        }

        public boolean isExists() {
            return exists;
        }

        public void setSessionExistence(long s, boolean exists)
        {
            sessions.put(s , exists);
        }

        public Boolean hasSession(long s)
        {
            return sessions.get(s);
        }
    }



    private final MongoClientDetailsStore detailsStore;
    private final Cache<Long , ClientsExistence> cache;

    public ExistenceCache(MongoClientDetailsStore detailsStore, Cache<Long, ClientsExistence> cache) {
        this.detailsStore = detailsStore;
        this.cache = cache;
    }

}
