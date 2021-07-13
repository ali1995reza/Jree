package jree.abs.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.config.TopicConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import jree.abs.parts.*;
import jree.api.PubMessage;

import java.util.Arrays;

public class HazelcastClusterInterceptor implements Interceptor<String, String> {

    private HazelcastInstance hazelcastInstance;
    private ITopic<PubMessage<String, String>> messageTopic;
    private HazelcastMessageInterceptor interceptor;

    @Override
    public void initialize(InterceptorContext<String, String> context) {
        Config config = new Config();
        config.getSerializationConfig()
                .addSerializerConfig(
                        new SerializerConfig()
                        .setImplementation(new PubMessageSerializer())
                        .setTypeClass(PubMessage.class)
                );
        this.hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        messageTopic = hazelcastInstance.getReliableTopic("messages");
        messageTopic.addMessageListener(m->{
            if(m.getPublishingMember().localMember())
                return;
            context.notifyMessage(m.getMessageObject());
        });
        interceptor = new HazelcastMessageInterceptor(messageTopic);
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
    public SubscribeInterceptor<String, String> subscribeInterceptor() {
        return SubscribeInterceptor.EMPTY;
    }

}
