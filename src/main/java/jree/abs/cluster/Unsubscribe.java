package jree.abs.cluster;

public class Unsubscribe {
    private final long subscriber;
    private final long conversation;

    public Unsubscribe(long subscriber, long conversation) {
        this.subscriber = subscriber;
        this.conversation = conversation;
    }

    public long conversation() {
        return conversation;
    }

    public long subscriber() {
        return subscriber;
    }
}
