package jree.abs.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import jree.abs.parts.interceptor.*;
import jree.api.PubMessage;
import jree.api.Signal;

public class HazelcastClusterInterceptor implements Interceptor<String, String> {

    private HazelcastInstance hazelcastInstance;
    private ITopic<PubMessage<String, String>> messageTopic;
    private ITopic<Signal<String>> signalTopic;
    private HazelcastMessageInterceptor interceptor;

    @Override
    public void initialize(InterceptorContext<String, String> context) {
        Config config = new Config();
        config.getSerializationConfig()
                .addSerializerConfig(
                        new SerializerConfig()
                        .setImplementation(new PubMessageSerializer())
                        .setTypeClass(PubMessage.class)
                ).addSerializerConfig(
                        new SerializerConfig()
                        .setImplementation(new SignalSerializer())
                        .setTypeClass(Signal.class)
                );
        this.hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        messageTopic = hazelcastInstance.getReliableTopic("messages");
        messageTopic.addMessageListener(m->{
            if(m.getPublishingMember().localMember())
                return;
            context.notifyMessage(m.getMessageObject());
        });
        signalTopic = hazelcastInstance.getReliableTopic("signals");
        signalTopic.addMessageListener(m->{
            if(m.getPublishingMember().localMember())
                return;
            context.notifySignal(m.getMessageObject());
        });
        interceptor = new HazelcastMessageInterceptor(messageTopic, signalTopic);
    }

    @Override
    public MessageInterceptor<String, String> messageInterceptor() {
        return interceptor;
    }

    @Override
    public SessionInterceptor<String, String> sessionInterceptor() {
        return SessionInterceptor.EMPTY;
    }

    @Override
    public SubscriptionInterceptor<String, String> subscriptionInterceptor() {
        return SubscriptionInterceptor.EMPTY;
    }

}
