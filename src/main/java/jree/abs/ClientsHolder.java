package jree.abs;


import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

final class ClientsHolder {

    private final ConcurrentHashMap<Long, SessionsHolder> holders;
    private final Function creator = new Function<Long, SessionsHolder>() {
        @Override
        public SessionsHolder apply(Long aLong) {
            return new SessionsHolder(aLong);
        }
    };

    public ClientsHolder() {
        holders = new ConcurrentHashMap<>();
    }

    public boolean addNewSession(SessionImpl session) {
        SessionsHolder holder =
                holders.computeIfAbsent(session.clientId(), creator);

        return holder.addNewSession(session);

    }

    public void removeSession(SessionImpl session) {
        SessionsHolder holder =
                holders.get(session.clientId());

        holder.removeSession(session);
    }

    public SessionsHolder getSessionsForClient(long client) {
        return holders.get(client);
    }

    public boolean isSessionAlive(long client, long session) {
        SessionsHolder holder = holders.get(client);
        if (holder == null)
            return false;

        return holder.findSessionById(session) != null;
    }
}
