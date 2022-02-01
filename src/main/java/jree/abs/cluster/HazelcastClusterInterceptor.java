package jree.abs.cluster;

import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import jree.abs.cluster.serializers.*;
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
    private HazelcastSessionInterceptor sessionInterceptor;
    private ITopic sessionEventTopic;

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
                ).addSerializerConfig(
                        new SerializerConfig()
                        .setImplementation(new OpenSessionEventSerializer())
                        .setTypeClass(OpenSessionEvent.class)
                ).addSerializerConfig(
                        new SerializerConfig()
                        .setImplementation(new CloseSessionEventSerializer())
                        .setTypeClass(CloseSessionEvent.class)
                ).addSerializerConfig(
                        new SerializerConfig()
                                .setImplementation(new RemoveSessionSerializer())
                                .setTypeClass(RemoveSessionEvent.class)
                ).addSerializerConfig(
                        new SerializerConfig()
                                .setImplementation(new RemoveClientSerializer())
                                .setTypeClass(RemoveClientEvent.class)
                );
        this.hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        this.hazelcastInstance.getCluster()
                .addMembershipListener(new MembershipListener() {
                    @Override
                    public void memberAdded(MembershipEvent membershipEvent) {

                    }

                    @Override
                    public void memberRemoved(MembershipEvent membershipEvent) {
                        if(membershipEvent.getMember().localMember()) {
                            context.notifyShutdown(200);
                        }
                    }
                });
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
        sessionEventTopic = hazelcastInstance.getReliableTopic("sessions");
        sessionEventTopic.addMessageListener(m->{
           if(m.getPublishingMember().localMember())
               return;

           Object messageObject = m.getMessageObject();

           if(messageObject instanceof RemoveSessionEvent) {
               RemoveSessionEvent event = (RemoveSessionEvent) messageObject;
               context.notifyRemoveSession(event.clientId(), event.sessionId());
           }else if(messageObject instanceof RemoveClientEvent) {
               RemoveClientEvent event = (RemoveClientEvent) messageObject;
               context.notifyRemoveClient(event.clientId());
           }

        });
        messageInterceptor = new HazelcastMessageInterceptor(messageTopic, signalTopic);
        subscriptionInterceptor = new HazelcastSubscriptionInterceptor(subscriptionTopic);
        sessionInterceptor = new HazelcastSessionInterceptor(sessionEventTopic);
    }

    @Override
    public MessageInterceptor<String, String> messageInterceptor() {
        return messageInterceptor;
    }

    @Override
    public SessionInterceptor<String, String> sessionInterceptor() {
        return sessionInterceptor;
    }

    @Override
    public SubscriptionInterceptor<String, String> subscriptionInterceptor() {
        return subscriptionInterceptor;
    }

}
