package jree.abs.cluster;

import com.hazelcast.topic.ITopic;
import jree.abs.parts.interceptor.MessageInterceptor;
import jree.api.*;

public class HazelcastMessageInterceptor implements MessageInterceptor<String, String> {

    private final ITopic<PubMessage<String, String>> messagesTopic;
    private final ITopic<Signal<String>> signalTopic;

    public HazelcastMessageInterceptor(ITopic<PubMessage<String, String>> messagesTopic, ITopic<Signal<String>> signalTopic) {
        this.messagesTopic = messagesTopic;
        this.signalTopic = signalTopic;
    }

    @Override
    public void onMessagePublish(PubMessage<String, String> message, OperationResultListener<Void> listener) {
        messagesTopic.publishAsync(message);
        listener.onSuccess(null);
    }

    @Override
    public void onSignalSend(Signal<String> signal, OperationResultListener<Void> listener) {
        signalTopic.publishAsync(signal);
        listener.onSuccess(null);
    }


}
