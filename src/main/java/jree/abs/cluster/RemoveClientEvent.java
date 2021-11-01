package jree.abs.cluster;

public class RemoveClientEvent {

    private final long clientId;


    public RemoveClientEvent(long clientId) {
        this.clientId = clientId;
    }

    public long clientId() {
        return clientId;
    }
}
