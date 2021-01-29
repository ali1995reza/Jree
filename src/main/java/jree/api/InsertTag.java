package jree.api;

public final class InsertTag {

    private String name;
    private long idIndex;
    private boolean attachToMessages;

    public InsertTag attachToMessages() {
        this.attachToMessages = true;
        return this;
    }

    public InsertTag setAttachToMessages(boolean attachToMessages) {
        this.attachToMessages = attachToMessages;
        return this;
    }

    public InsertTag dontAttachToMessage()
    {
        this.attachToMessages = false;
        return this;
    }

    public InsertTag setIdIndex(long idIndex) {
        this.idIndex = idIndex;
        return this;
    }

    public InsertTag setName(String name) {
        this.name = name;
        return this;
    }


    public long idIndex() {
        return idIndex;
    }

    public String name() {
        return name;
    }

    public boolean wantAttachToMessages() {
        return attachToMessages;
    }
}
