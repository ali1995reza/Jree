package jree.abs;

import jree.abs.parts.*;
import jree.api.MessageManager;
import jree.api.PubMessage;
import jree.api.PubSubSystem;
import jree.api.SessionManager;
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
        this.sessionManager = new SessionManagerImpl<>(messageStore, detailsStore, interceptor,
                clientsHolder,
                subscribers,
                this.idBuilder);
        this.messageManager = new MessageManagerImpl<>(clientsHolder, messageStore);
        InterceptorContextImpl<BODY, ID> context = new InterceptorContextImpl<>(subscribers, clientsHolder);
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

        private InterceptorContextImpl(ConversationSubscribersHolder<BODY, ID> subscribers, ClientsHolder clientsHolder) {
            this.subscribers = subscribers;
            this.clientsHolder = clientsHolder;
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
    }

}
