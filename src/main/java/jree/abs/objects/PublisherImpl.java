package jree.abs.objects;

import jree.api.Publisher;
import jree.api.Session;

public class PublisherImpl implements Publisher {

    public static Publisher fromSession(Session session){
        return new PublisherImpl(session.clientId() , session.id());
    }

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
