package jree.api;

public class SubscribeOption {

    private boolean justThiSession = true;
    private int lastMessages;

    public SubscribeOption setJustThiSession(boolean justThiSession) {
        this.justThiSession = justThiSession;
        return this;
    }

    public SubscribeOption setLastMessages(int lastMessages) {
        this.lastMessages = lastMessages;
        return this;
    }

    public int lastMessages() {
        return lastMessages;
    }

    public boolean justThiSession() {
        return justThiSession;
    }
}
