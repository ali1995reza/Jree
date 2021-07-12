package jree.abs.cluster;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import jree.abs.parts.Interceptor;
import jree.abs.parts.MessageInterceptor;
import jree.abs.parts.SessionInterceptor;
import jree.abs.parts.SubscribeInterceptor;
import jree.api.PubMessage;

public class HazelcastClusterInterceptor implements Interceptor<String, String> {

    private final HazelcastInstance hazelcastInstance;
    private final ITopic<PubMessage<String, String>> messageTopic;
    private final HazelcastMessageInterceptor interceptor;

    public HazelcastClusterInterceptor() {
        this.hazelcastInstance = Hazelcast.newHazelcastInstance();
        messageTopic = hazelcastInstance.getTopic("messages");
        messageTopic.addMessageListener(new MessageListener<PubMessage<String, String>>() {
            @Override
            public void onMessage(Message<PubMessage<String, String>> message) {

                //to do send message to other nodes !
            }
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
