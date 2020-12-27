package jree.mongo_base;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ClientsHolder {

    private final ConcurrentHashMap<Long , SessionsHolder> holders;
    private final Function creator =  new Function<Long, SessionsHolder>() {
        @Override
        public SessionsHolder apply(Long aLong) {
            return new SessionsHolder(aLong);
        }
    };

    public ClientsHolder() {
        holders = new ConcurrentHashMap<>();
    }

    public boolean addNewSession(MongoSession session)
    {
        SessionsHolder holder =
                holders.computeIfAbsent(session.clientId(), creator);

        return holder.addNewSession(session);

    }

    public void removeSession(MongoSession session)
    {
        SessionsHolder holder =
                holders.get(session.clientId());

        holder.removeSession(session);
    }
    public SessionsHolder getSessionsForClient(long client)
    {
        return holders.get(client);
    }
}
