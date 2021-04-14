package jree.abs.objects;

import jree.api.Publisher;
import jree.api.Recipient;
import jree.api.Session;
import jree.api.Signal;

public class SignalImpl<BODY> implements Signal<BODY> {


    private final BODY body;
    private final Publisher publisher;
    private final Recipient recipient;

    public SignalImpl(BODY body, Publisher publisher, Recipient recipient) {
        this.body = body;
        this.publisher = publisher;
        this.recipient = recipient;
    }

    public SignalImpl(BODY body, Session publisher, Recipient recipient) {
        this(body, PublisherImpl.fromSession(publisher), recipient);
    }

    @Override
    public BODY body() {
        return body;
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
    public String toString() {
        return "{" +
                "body=" + body +
                ", publisher=" + publisher +
                ", recipient=" + recipient +
                '}';
    }
}
