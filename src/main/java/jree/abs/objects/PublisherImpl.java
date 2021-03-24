package jree.abs.objects;

import jree.api.Publisher;

public class PublisherImpl implements Publisher {

    private final long client;
    private final long session;

    public PublisherImpl(long client, long session) {
        this.client = client;
        this.session = session;
    }


    @Override
    public long client() {
        return client;
    }

    @Override
    public long session() {
        return session;
    }

    @Override
    public String toString() {
        return "Publisher{" +
                "client=" + client +
                ", session=" + session +
                '}';
    }
}
