package jree.abs.cluster;

public class RemoveSessionEvent {
    private final long clientId;
    private final long sessionId;


    public RemoveSessionEvent(long clientId, long sessionId) {
        this.clientId = clientId;
        this.sessionId = sessionId;
    }

    public long clientId() {
        return clientId;
    }

    public long sessionId() {
        return sessionId;
    }
}
