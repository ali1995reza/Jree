package jree.abs.objects;

import jree.api.*;
import jree.abs.utils.StaticFunctions;

import java.time.Instant;
import java.util.List;

public class PubMessageImpl<BODY, ID> extends SimpleAttachable implements PubMessage<BODY, ID> {

    private BODY body;
    private ID id;
    private Instant time;
    private PubMessage.Type type;
    private Publisher publisher;
    private Recipient recipient;

    public PubMessageImpl(ID id, BODY body, Instant time, Session session, Recipient recipient) {
        this.body = body;
        this.id = id;
        this.time = time;
        this.type = StaticFunctions.getType(recipient);
        this.publisher = new PublisherImpl(session.clientId(), session.id());
        this.recipient = recipient;
    }

    public PubMessageImpl(ID id, BODY body, Instant time, long clientId, long sessionId, Recipient recipient) {
        this.body = body;
        this.id = id;
        this.time = time;
        this.type = StaticFunctions.getType(recipient);
        this.recipient = recipient;
        this.publisher = new PublisherImpl(clientId, sessionId);
    }

    public PubMessageImpl(ID id, BODY body, Instant time, Publisher publisher, Recipient recipient) {
        this.body = body;
        this.id = id;
        this.time = time;
        this.type = StaticFunctions.getType(recipient);
        this.recipient = recipient;
        this.publisher = publisher;
    }

    public PubMessageImpl() {
    }

    @Override
    public ID id() {
        return id;
    }

    @Override
    public BODY body() {
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
        return "PubMessage{" + "body=" + body + ", id=" + id + ", time=" + time + ", type=" + type + ", publisher=" +
                publisher + ", recipient=" + recipient + '}';
    }

}
