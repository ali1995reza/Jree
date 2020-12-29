package jree.mongo_base;

public final class MessageIndex {

    private final String conversation;
    private long maxMessageIndex;

    public MessageIndex(String conversation, long maxMessageIndex) {
        this.conversation = conversation;
        this.maxMessageIndex = maxMessageIndex;
    }

    public long maxMessageIndex() {
        return maxMessageIndex;
    }

    public MessageIndex decreaseMessageIndex(long l)
    {
        maxMessageIndex -= l;
        maxMessageIndex = maxMessageIndex<0?0l:maxMessageIndex;
        return this;
    }

    public String conversation() {
        return conversation;
    }
}
