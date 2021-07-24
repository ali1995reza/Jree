package jree.abs.cluster;

public class OpenSessionEvent {

    private final long clientId;
    private final long sessionId;

    public OpenSessionEvent(long clientId, long sessionId) {
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
