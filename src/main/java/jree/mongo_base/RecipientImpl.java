package jree.mongo_base;

import jree.api.Recipient;

public class RecipientImpl implements Recipient {


    public final static RecipientImpl sessionRecipient(long client , long session)
    {
        return new RecipientImpl(-1 , client , session);
    }

    public final static RecipientImpl clientRecipient(long client)
    {
        return new RecipientImpl(-1 , client , -1);
    }

    public final static RecipientImpl conversationRecipient(long conversation)
    {
        return new RecipientImpl(conversation , -1 , -1);
    }



    private long conversation = -1;
    private long client = -1;
    private long session = -1;

    public RecipientImpl(long conversation, long client, long session) {
        this.conversation = conversation;
        this.client = client;
        this.session = session;
    }

    public RecipientImpl(){}

    @Override
    public long conversation() {
        return conversation;
    }

    @Override
    public long client() {
        return client;
    }

    @Override
    public long session() {
        return session;
    }


    public void setClient(long client) {
        this.client = client;
    }

    public void setConversation(long conversation) {
        this.conversation = conversation;
    }

    public void setSession(long session) {
        this.session = session;
    }

    @Override
    public String toString() {
        return "Recipient{" +
                "conversation=" + conversation +
                ", client=" + client +
                ", session=" + session +
                '}';
    }
}
