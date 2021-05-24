package jree.api;

public class SimpleReadCriteria<ID extends Comparable<ID>> implements ReadMessageCriteria<ID> {

    public final static <ID extends Comparable<ID>> Builder<ID> builder(Class<ID> idType)
    {
        return new Builder<>();
    }


    public final static class Builder<ID extends Comparable<ID>>{

        private Session session;
        private Recipient recipient;
        private ID from;
        private int length;
        private boolean backward;
        private boolean containsDisposable;


        public Builder<ID> setBackward(boolean backward) {
            this.backward = backward;
            return this;
        }

        public Builder<ID> backward()
        {
            return setBackward(true);
        }

        public Builder<ID> forward()
        {
            return setBackward(false);
        }

        public Builder<ID> setLength(int length) {
            this.length = length;
            return this;
        }

        public Builder<ID> from(ID from) {
            this.from = from;
            return this;
        }

        public Builder<ID> setRecipient(Recipient recipient) {
            this.recipient = recipient;
            return this;
        }


        public Builder<ID> setSession(Session session) {
            this.session = session;
            return this;
        }

        public Builder<ID> withDisposables()
        {
            containsDisposable = true;
            return this;
        }

        public Builder<ID> withoutDisposables()
        {
            containsDisposable = false;
            return this;
        }

        public Builder refresh()
        {
            session = null;
            recipient = null;
            from = null;
            length = 0;
            backward = false;

            return this;
        }


        public ReadMessageCriteria<ID> build()
        {
            return new SimpleReadCriteria(
                    session ,
                    recipient ,
                    from ,
                    length ,
                    backward,
                    containsDisposable);
        }

    }


    private final Session session;
    private final Recipient recipient;
    private final ID from;
    private final int length;
    private final boolean backward;
    private final boolean containsDisposable;

    public SimpleReadCriteria(Session session,
                              Recipient recipient,
                              ID from,
                              int length,
                              boolean backward, boolean containsDisposable) {
        this.session = session;
        this.recipient = recipient;
        this.from = from;
        this.length = length;
        this.backward = backward;
        this.containsDisposable = containsDisposable;
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
    public ID from() {
        return from;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public boolean containsDisposables() {
        return containsDisposable;
    }

    @Override
    public boolean backward() {
        return backward;
    }
}
