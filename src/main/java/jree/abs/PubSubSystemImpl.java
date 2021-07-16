package jree.abs;

import jree.abs.cache.RelationAndExistenceCache;
import jree.abs.objects.RecipientImpl;
import jree.abs.parts.*;
import jree.abs.parts.interceptor.Interceptor;
import jree.abs.parts.interceptor.InterceptorContext;
import jree.api.*;
import jree.util.Assertion;

final class PubSubSystemImpl<BODY, ID extends Comparable<ID>> implements PubSubSystem<BODY, ID> {

    private final MessageManagerImpl<BODY, ID> messageManager;
    private final SessionManagerImpl<BODY, ID> sessionManager;
    private final IdBuilder<ID> idBuilder;
    private final ClientsHolder clientsHolder;

    public PubSubSystemImpl(MessageStore<BODY, ID> messageStore, DetailsStore<ID> detailsStore, IdBuilder<ID> idBuilder, Interceptor<BODY, ID> interceptor) {
        Assertion.ifOneNull("some provided parts are null", messageStore, detailsStore, idBuilder, interceptor);
        this.idBuilder = idBuilder;
        this.clientsHolder = new ClientsHolder();
        final ConversationSubscribersHolder<BODY, ID> subscribers =  new ConversationSubscribersHolder<>(clientsHolder, "jdbc:h2:mem:db1");
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

        private final ConversationSubscribersHolder<BODY, ID> subscribers;
        private final ClientsHolder clientsHolder;
        private final RelationAndExistenceCache<ID> cache;

        private InterceptorContextImpl(ConversationSubscribersHolder<BODY, ID> subscribers, ClientsHolder clientsHolder, RelationAndExistenceCache<ID> cache) {
            this.subscribers = subscribers;
            this.clientsHolder = clientsHolder;
            this.cache = cache;
        }


        @Override
        public void notifyMessage(PubMessage<BODY, ID> message) {
            if (message.type().is(PubMessage.Type.CLIENT_TO_CONVERSATION)) {
                subscribers.publishMessage(message);
            } else {
                clientsHolder.publishMessage(message.publisher().client(), message);
                clientsHolder.publishMessage(message.recipient().client(), message);
            }
        }

        @Override
        public void notifySignal(Signal<BODY> signal) {
            if (signal.recipient().conversation() > 0) {
                subscribers.sendSignal(signal);
            } else {
                clientsHolder.sendSignal(signal.recipient().client(), signal);
            }
        }

        @Override
        public void notifyUnsubscribe(Recipient subscriber, long conversationId) {
            subscribers.removeSubscriber(conversationId, subscriber);
        }

        @Override
        public void notifySubscribe(Recipient recipient, long conversationId) {
            subscribers.addSubscriber(conversationId, recipient);
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

    }

}
