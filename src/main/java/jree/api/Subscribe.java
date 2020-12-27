package jree.api;

public class Subscribe {

    private long conversation;
    private SubscribeOption option;


    public Subscribe setConversation(long conversation) {
        this.conversation = conversation;
        return this;
    }

    public Subscribe setOption(SubscribeOption option) {
        this.option = option;
        return this;
    }

    public long conversation() {
        return conversation;
    }

    public SubscribeOption option() {
        return option;
    }
}
