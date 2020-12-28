package jree.mongo_base;

public final class MessageIndex {

    private final String conversation;
    private final long lastMessage;

    public MessageIndex(String conversation, long lastMessage) {
        this.conversation = conversation;
        this.lastMessage = lastMessage;
    }

    public long lastMessage() {
        return lastMessage;
    }

    public String conversation() {
        return conversation;
    }
}
