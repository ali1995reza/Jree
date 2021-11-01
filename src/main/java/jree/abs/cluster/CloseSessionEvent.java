package jree.abs.cluster;

public class CloseSessionEvent {

    private final long clientId;
    private final long sessionId;

    public CloseSessionEvent(long clientId, long sessionId) {
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
