package jree.abs.cluster;

public class Subscribe {

    private final long subscriber;
    private final long conversation;

    public Subscribe(long subscriber, long conversation) {
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
