package jree.mongo_base;

import jree.api.PubMessage;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class SessionsHolder {


    private final long clientId;
    private final ConcurrentHashMap<Long , MongoSession> sessions;

    public SessionsHolder(long clientId) {
        this.clientId = clientId;
        sessions = new ConcurrentHashMap<>();
    }


    public boolean addNewSession(MongoSession session)
    {
        if(sessions.contains(session))
            return false;

        sessions.put(session.id() , session);
        return true;
    }

    public boolean removeSession(MongoSession session)
    {
        return sessions.remove(session.id()  , session);
    }

    public void publishMessage(PubMessage message)
    {
        if(message.type().is(PubMessage.Type.SESSION_TO_SESSION))
        {
            if(message.publisher().client()==clientId)
            {
                //so own message !
                MongoSession session = sessions.get(message.publisher().session());
                if(session!=null) session.onMessagePublished(message);
            }else
            {
                MongoSession session = sessions.get(message.recipient().session());
                if(session!=null) session.onMessagePublished(message);
            }
        }else{

            for(MongoSession session:sessions.values())
            {
                session.onMessagePublished(message);
            }
        }
    }

    public MongoSession findSessionById(long sessionId)
    {
        return sessions.get(sessionId);
    }

    public Collection<MongoSession> allSessions()
    {
        return sessions.values();
    }
}
