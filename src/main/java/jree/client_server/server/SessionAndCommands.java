package jree.client_server.server;

import jree.abs.objects.RecipientImpl;
import jree.api.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionAndCommands {


    private final PubSubSystem<String, String> pubSubSystem;
    private final Session<String, String> session;

    public SessionAndCommands(PubSubSystem<String, String> pubSubSystem, Session<String, String> session) {
        this.pubSubSystem = pubSubSystem;
        this.session = session;
    }

    public Session<String, String> getSession() {
        return session;
    }

    private static String result(Object o) {
        return new JSONObject().put("value", o).toString();
    }

    private ReadMessageCriteria<String> criteria(Recipient recipient, int len) {
        return SimpleReadCriteria.builder(String.class)
                .from("z")
                .backward()
                .setLength(len)
                .setSession(session)
                .setRecipient(recipient)
                .build();
    }

    public String getRecipients() {
        return result(session.recipientsList());
    }

    public String getSubscribeList() {
        return result(session.subscribeList());
    }

    public Iterable<PubMessage<String, String>> getMessagesOfAllConversations(int numberOfMessages) {
        List<ReadMessageCriteria<String>> criteria = new ArrayList<>();
        for(Recipient recipient:session.recipientsList()) {
            criteria.add(criteria(recipient, numberOfMessages));
        }
        for(Long subscribe:session.subscribeList()) {
            criteria.add(criteria(RecipientImpl.conversationRecipient(subscribe), numberOfMessages));
        }
        return pubSubSystem.messageManager().readMessages(criteria);
    }

    public void publishMessage(Recipient recipient, String message ) {
        session.publishMessage(recipient, message);
    }

    public void sendSignal(Recipient recipient, String signal) {
        session.sendSignal(recipient, signal);
    }
}
