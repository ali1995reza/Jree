package jree.abs;

import jree.abs.cache.RelationAndExistenceCache;
import jree.abs.funcs.ForEach;
import jree.abs.objects.RecipientImpl;
import jree.abs.parts.*;
import jree.abs.parts.interceptor.Interceptor;
import jree.abs.parts.interceptor.InterceptorContext;
import jree.api.*;
import jutils.assertion.Assertion;

import java.util.List;

final class PubSubSystemImpl<BODY, ID extends Comparable<ID>> implements PubSubSystem<BODY, ID> {

    private final MessageManagerImpl<BODY, ID> messageManager;
    private final SessionManagerImpl<BODY, ID> sessionManager;
    private final IdBuilder<ID> idBuilder;
    private final ClientsHolder clientsHolder;

    public PubSubSystemImpl(MessageStore<BODY, ID> messageStore, DetailsStore<ID> detailsStore, IdBuilder<ID> idBuilder, Interceptor<BODY, ID> interceptor) {
        Assertion.ifOneNull("some provided parts are null", messageStore, detailsStore, idBuilder, interceptor);
        this.idBuilder = idBuilder;
        this.clientsHolder = new ClientsHolder();
        final SubscribersHolder subscribers =  new InMemorySubscribersHolder();
        final RelationAndExistenceCache<ID> cache = new RelationAndExistenceCache<>(
                detailsStore,
                messageStore
        );
        this.sessionManager = new SessionManagerImpl<>(messageStore, detailsStore, interceptor,
                clientsHolder,
                subscribers,
                cache ,
                this.idBuilder);
        this.messageManager = new MessageManagerImpl<>(clientsHolder, messageStore);
        InterceptorContextImpl<BODY, ID> context = new InterceptorContextImpl<>(subscribers, clientsHolder, cache);
        interceptor.initialize(context);
    }

    @Override
    public MessageManager<BODY, ID> messageManager() {
        return messageManager;
    }

    @Override
    public SessionManager<BODY, ID> sessionManager() {
        return sessionManager;
    }

    private final static class InterceptorContextImpl<BODY, ID extends Comparable<ID>> implements InterceptorContext<BODY, ID> {

        private final SubscribersHolder subscribers;
        private final ClientsHolder clientsHolder;
        private final RelationAndExistenceCache<ID> cache;

        private InterceptorContextImpl(SubscribersHolder subscribers, ClientsHolder clientsHolder, RelationAndExistenceCache<ID> cache) {
            this.subscribers = subscribers;
            this.clientsHolder = clientsHolder;
            this.cache = cache;
        }


        @Override
        public void notifyMessage(PubMessage<BODY, ID> message) {
            if (message.type().is(PubMessage.Type.CLIENT_TO_CONVERSATION)) {
                publishMessageToConversation(message.recipient().conversation(), message);
            } else {
                clientsHolder.publishMessage(message.publisher().client(), message);
                clientsHolder.publishMessage(message.recipient().client(), message);
            }
        }

        @Override
        public void notifySignal(Signal<BODY> signal) {
            if (signal.recipient().conversation() > 0) {
                sendSignalToConversation(signal.recipient().conversation(), signal);
            } else {
                clientsHolder.sendSignal(signal.recipient().client(), signal);
            }
        }

        @Override
        public void notifyUnsubscribe(Recipient subscriber, long conversationId) {
            subscribers.removeSubscriber(conversationId, subscriber.client(), OperationResultListener.EMPTY_LISTENER);
        }

        @Override
        public void notifySubscribe(Recipient recipient, long conversationId) {
            subscribers.addSubscriber(conversationId, recipient.client(), OperationResultListener.EMPTY_LISTENER);
        }

        @Override
        public void notifyRemoveSession(long clientId, long sessionId) {
            cache.removeExistenceCache(RecipientImpl.sessionRecipient(clientId, sessionId));
            clientsHolder.removeSessionAndCloseIt(clientId, sessionId);
        }

        @Override
        public void notifyRemoveClient(long clientId) {
            cache.removeExistenceCache(RecipientImpl.clientRecipient(clientId));
            clientsHolder.removeClientAndCloseAllSessions(clientId);
        }

        @Override
        public void notifyShutdown(int code) {
            //todo shutdown system
            System.exit(code);
        }

        private void publishMessageToConversation(long conversation, PubMessage message) {
            subscribers.getSubscribers(conversation, new ForEach<List<Long>>() {
                @Override
                public void accept(List<Long> subscribeList) {
                    for(Long clientId : subscribeList) {
                        clientsHolder.publishMessage(clientId, message);
                    }
                }
            });
        }

        private void sendSignalToConversation(long conversation, Signal signal) {
            subscribers.getSubscribers(conversation, new ForEach<List<Long>>() {
                @Override
                public void accept(List<Long> subscribeList) {
                    for(Long clientId : subscribeList) {
                        clientsHolder.sendSignal(clientId, signal);
                    }
                }
            });
        }

    }

}
