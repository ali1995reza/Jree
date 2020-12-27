package jree.mongo_base;

public class ConversationOffset {

    private final String conversationId;
    private final long offset;

    public ConversationOffset(String conversationId, long offset) {
        this.conversationId = conversationId;
        this.offset = offset;
    }

    public long offset() {
        return offset;
    }

    public String conversationId() {
        return conversationId;
    }

    @Override
    public String toString() {
        return "Offset{" +
                "conversationId='" + conversationId + '\'' +
                ", count=" + offset +
                '}';
    }
}
