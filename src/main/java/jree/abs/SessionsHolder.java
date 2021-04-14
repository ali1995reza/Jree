package jree.abs;

import jree.api.PubMessage;
import jree.api.Signal;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

final class SessionsHolder {


    private final long clientId;
    private final ConcurrentHashMap<Long , SessionImpl> sessions;

    public SessionsHolder(long clientId) {
        this.clientId = clientId;
        sessions = new ConcurrentHashMap<>();
    }


    public boolean addNewSession(SessionImpl session)
    {
        if(sessions.contains(session))
            return false;

        sessions.put(session.id() , session);
        return true;
    }

    public boolean removeSession(SessionImpl session)
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
                SessionImpl session = sessions.get(message.publisher().session());
                if(session!=null) session.onMessagePublished(message);
            }else
            {
                SessionImpl session = sessions.get(message.recipient().session());
                if(session!=null) session.onMessagePublished(message);
            }
        }else{

            for(SessionImpl session:sessions.values())
            {
                session.onMessagePublished(message);
            }
        }
    }

    public void sendSignal(Signal signal){
        for(SessionImpl session:sessions.values())
        {
            session.onSignalReceived(signal);
        }
    }

    public SessionImpl findSessionById(long sessionId)
    {
        return sessions.get(sessionId);
    }

    public Collection<SessionImpl> allSessions()
    {
        return sessions.values();
    }
}
