package jree.mongo_base;

import jree.api.*;

import java.time.Instant;
import java.util.List;

public class PubMessageImpl<T> extends SimpleAttachable implements PubMessage<T , String> {

    private T body;
    private String id;
    private Instant time;
    private PubMessage.Type type;
    private Publisher publisher;
    private Recipient recipient;

    public PubMessageImpl(String id, T body, Instant time, Type type, Session session, Recipient recipient) {
        this.body = body;
        this.id = id;
        this.time = time;
        this.type = type;
        this.publisher = new PublisherImpl(session.clientId() , session.id());
        this.recipient = recipient;
    }


    public PubMessageImpl(String id, T body, Instant time, Type type, long clientId, long sessionId, Recipient recipient) {
        this.body = body;
        this.id = id;
        this.time = time;
        this.type = type;
        this.recipient = recipient;
        this.publisher = new PublisherImpl(clientId, sessionId);
    }

    public PubMessageImpl(){}


    @Override
    public String id() {
        return id;
    }

    @Override
    public T body() {
        return body;
    }

    @Override
    public Instant time() {
        return time;
    }

    @Override
    public Publisher publisher() {
        return publisher;
    }

    @Override
    public Recipient recipient() {
        return recipient;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public List<Tag> tags() {
        return null;
    }


    @Override
    public String toString() {
        return "PubMessage{" +
                "body=" + body +
                ", id=" + id +
                ", time=" + time +
                ", type=" + type +
                ", publisher=" + publisher +
                ", recipient=" + recipient +
                '}';
    }
}
