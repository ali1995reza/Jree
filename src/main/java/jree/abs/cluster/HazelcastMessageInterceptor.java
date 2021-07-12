package jree.abs.cluster;

import com.hazelcast.topic.ITopic;
import jree.abs.parts.MessageInterceptor;
import jree.api.OperationResultListener;
import jree.api.PubMessage;
import jree.api.Recipient;
import jree.api.Session;

public class HazelcastMessageInterceptor implements MessageInterceptor<String, String> {

    private final ITopic<PubMessage<String, String>> topic;

    public HazelcastMessageInterceptor(ITopic<PubMessage<String, String>> topic) {
        this.topic = topic;
    }

    @Override
    public void beforePublishMessage(String s, Session publisher, Recipient recipient, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

    @Override
    public void onMessagePublish(PubMessage<String, String> message, OperationResultListener<Void> listener) {
        topic.publish(message);
    }

    @Override
    public void afterMessagePublished(PubMessage<String, String> message, OperationResultListener<Void> listener) {
        listener.onSuccess(null);
    }

}
