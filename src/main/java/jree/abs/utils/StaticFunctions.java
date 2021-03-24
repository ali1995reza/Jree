package jree.abs.utils;

import jree.api.PubMessage;
import jree.api.Publisher;
import jree.api.Recipient;
import jree.api.Session;

import java.security.SecureRandom;
import java.util.Random;

public class StaticFunctions {

    private final static Random RANDOM = new SecureRandom();

    public static String uniqueConversationId(Publisher publisher, Recipient recipient) {
        if (recipient.conversation() != -1) {
            return String.valueOf(recipient.conversation());
        } else {
            if (recipient.session() != -1) {
                if (publisher.client() > recipient.client()) {
                    return "" + publisher.client() + "_" + publisher.session() + "_" + recipient.client() + "_" +
                            recipient.session();
                } else {
                    return "" + recipient.client() + "_" + recipient.session() + "_" + publisher.client() + "_" +
                            publisher.session();
                }
            } else {
                if (publisher.client() > recipient.client()) {
                    return "" + publisher.client() + "_" + recipient.client();
                } else {
                    return "" + recipient.client() + "_" + publisher.client();
                }
            }
        }
    }

    public static String uniqueConversationId(Session session, Recipient recipient) {
        if (recipient.conversation() != -1) {
            return String.valueOf(recipient.conversation());
        } else {
            if (recipient.session() != -1) {
                if (session.clientId() > recipient.client()) {
                    return "" + session.clientId() + "_" + session.id() + "_" + recipient.client() + "_" +
                            recipient.session();
                } else {
                    return "" + recipient.client() + "_" + recipient.session() + "_" + session.clientId() + "_" +
                            session.id();
                }
            } else {
                if (session.clientId() > recipient.client()) {
                    return "" + session.clientId() + "_" + recipient.client();
                } else {
                    return "" + recipient.client() + "_" + session.clientId();
                }
            }
        }
    }

    public static String uniqueConversationId(PubMessage message) {
        return uniqueConversationId(message.publisher(), message.recipient());
    }

    public static PubMessage.Type getType(Recipient recipient) {
        if (recipient.conversation() != -1) {
            return PubMessage.Type.CLIENT_TO_CONVERSATION;
        } else if (recipient.session() != -1) {
            return PubMessage.Type.SESSION_TO_SESSION;
        } else {
            return PubMessage.Type.CLIENT_TO_CLIENT;
        }
    }

    public static long newLongId() {
        return Math.abs(RANDOM.nextLong());
    }

}
