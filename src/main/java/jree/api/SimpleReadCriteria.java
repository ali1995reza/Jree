package jree.api;

public class SimpleReadCriteria implements ReadMessageCriteria {

    public final static Builder builder()
    {
        return new Builder();
    }


    public final static class Builder{

        private Session session;
        private Recipient recipient;
        private long offset;
        private long length;
        private boolean backward;


        public Builder setBackward(boolean backward) {
            this.backward = backward;
            return this;
        }

        public Builder setLength(long length) {
            this.length = length;
            return this;
        }

        public Builder setOffset(long offset) {
            this.offset = offset;
            return this;
        }

        public Builder setRecipient(Recipient recipient) {
            this.recipient = recipient;
            return this;
        }


        public Builder setSession(Session session) {
            this.session = session;
            return this;
        }

        public Builder refresh()
        {
            session = null;
            recipient = null;
            offset = 0;
            length = 0;
            backward = false;

            return this;
        }


        public ReadMessageCriteria build()
        {
            return new SimpleReadCriteria(
                    session ,
                    recipient ,
                    offset ,
                    length ,
                    backward
            );
        }

    }


    private final Session session;
    private final Recipient recipient;
    private final long offset;
    private final long length;
    private final boolean backward;

    public SimpleReadCriteria(Session session,
                              Recipient recipient,
                              long offset,
                              long length ,
                              boolean backward) {
        this.session = session;
        this.recipient = recipient;
        this.offset = offset;
        this.length = length;
        this.backward = backward;
    }


    @Override
    public Session session() {
        return session;
    }

    @Override
    public Recipient recipient() {
        return recipient;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public boolean backward() {
        return backward;
    }
}
