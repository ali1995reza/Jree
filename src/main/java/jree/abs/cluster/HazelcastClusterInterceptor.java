package jree.abs.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import jree.abs.cluster.serializers.PubMessageSerializer;
import jree.abs.cluster.serializers.SignalSerializer;
import jree.abs.cluster.serializers.SubscribeSerializer;
import jree.abs.cluster.serializers.UnsubscribeSerializer;
import jree.abs.objects.RecipientImpl;
import jree.abs.parts.interceptor.*;
import jree.api.PubMessage;
import jree.api.Signal;

public class HazelcastClusterInterceptor implements Interceptor<String, String> {

    private HazelcastInstance hazelcastInstance;
    private ITopic<PubMessage<String, String>> messageTopic;
    private ITopic<Signal<String>> signalTopic;
    private ITopic subscriptionTopic;
    private HazelcastMessageInterceptor messageInterceptor;
    private HazelcastSubscriptionInterceptor subscriptionInterceptor;

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
                ).addSerializerConfig(
                        new SerializerConfig()
                        .setImplementation(new SubscribeSerializer())
                        .setTypeClass(Subscribe.class)
                ).addSerializerConfig(
                        new SerializerConfig()
                        .setImplementation(new UnsubscribeSerializer())
                        .setTypeClass(Unsubscribe.class)
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
        subscriptionTopic = hazelcastInstance.getReliableTopic("subscription");
        subscriptionTopic.addMessageListener(m->{
            if(m.getPublishingMember().localMember())
                return;
            Object messageObject = m.getMessageObject();
            if(messageObject instanceof Subscribe) {
                Subscribe subscribe = (Subscribe) messageObject;
                context.notifySubscribe(RecipientImpl.clientRecipient(subscribe.subscriber()), subscribe.conversation());
            } else {
                Unsubscribe unsubscribe = (Unsubscribe) messageObject;
                context.notifyUnsubscribe(RecipientImpl.clientRecipient(unsubscribe.subscriber()), unsubscribe.conversation());
            }
        });
        messageInterceptor = new HazelcastMessageInterceptor(messageTopic, signalTopic);
        subscriptionInterceptor = new HazelcastSubscriptionInterceptor(subscriptionTopic);
    }

    @Override
    public MessageInterceptor<String, String> messageInterceptor() {
        return messageInterceptor;
    }

    @Override
    public SessionInterceptor<String, String> sessionInterceptor() {
        return SessionInterceptor.EMPTY;
    }

    @Override
    public SubscriptionInterceptor<String, String> subscriptionInterceptor() {
        return subscriptionInterceptor;
    }

}
