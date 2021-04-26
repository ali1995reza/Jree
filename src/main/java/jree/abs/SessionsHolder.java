package jree.abs;

import jree.api.PubMessage;
import jree.api.Signal;

import java.util.Collection;
import java.util.HashMap;

final class SessionsHolder {


    private final long clientId;
    private final HashMap<Long, SessionImpl> sessions;

    public SessionsHolder(long clientId) {
        this.clientId = clientId;
        sessions = new HashMap<>();
    }


    public boolean addNewSession(SessionImpl session) {
        if (sessions.containsKey(session.id()))
            return false;
        sessions.put(session.id(), session);
        return true;
    }

    public boolean removeSession(SessionImpl session) {
        return sessions.remove(session.id(), session);
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    public boolean publishMessage(PubMessage message) {
        if (message.type().is(PubMessage.Type.SESSION_TO_SESSION)) {
            long sessionId = message.publisher().client() == clientId ?
                    message.publisher().session() :
                    message.recipient().session();
            SessionImpl session = sessions.get(sessionId);
            if (session != null) {
                session.onMessagePublished(message);
                return true;
            } else {
                return false;
            }
        } else {

            for (SessionImpl session : sessions.values()) {
                session.onMessagePublished(message);
            }

            return true;
        }
    }

    public boolean sendSignal(Signal signal) {

        if (signal.recipient().conversation() > 0 || signal.recipient().session() < 0) {
            for (SessionImpl session : sessions.values()) {
                session.onSignalReceived(signal);
            }
            return true;
        } else {
            SessionImpl session = sessions.get(signal.recipient().session());
            if (session != null) {
                session.onSignalReceived(signal);
                return true;
            }
            return false;
        }
    }

    public SessionImpl findSessionById(long sessionId) {
        return sessions.get(sessionId);
    }

}
