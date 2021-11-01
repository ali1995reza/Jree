package jree.abs.cluster;

import com.hazelcast.topic.ITopic;
import jree.abs.parts.interceptor.SubscriptionInterceptor;
import jree.api.OperationResultListener;
import jree.api.Recipient;

public class HazelcastSubscriptionInterceptor implements SubscriptionInterceptor<String, String> {

    private final ITopic subscriptionTopic;

    public HazelcastSubscriptionInterceptor(ITopic subscriptionTopic) {
        this.subscriptionTopic = subscriptionTopic;
    }

    @Override
    public void onSubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        subscriptionTopic.publishAsync(
                new Subscribe(subscriber.client(), conversation)
        );
        listener.onSuccess(null);
    }

    @Override
    public void onUnsubscribe(Recipient subscriber, long conversation, OperationResultListener<Void> listener) {
        subscriptionTopic.publishAsync(
                new Unsubscribe(subscriber.client(), conversation)
        );
        listener.onSuccess(null);
    }

}
