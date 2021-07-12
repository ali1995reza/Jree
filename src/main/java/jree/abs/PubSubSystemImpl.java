package jree.abs;

import jree.abs.parts.DetailsStore;
import jree.abs.parts.IdBuilder;
import jree.abs.parts.Interceptor;
import jree.abs.parts.MessageStore;
import jree.api.MessageManager;
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
        this.sessionManager = new SessionManagerImpl<>(messageStore, detailsStore, interceptor, clientsHolder, new ConversationSubscribersHolder<>(clientsHolder, "jdbc:h2:mem:db1"), this.idBuilder);
        this.messageManager = new MessageManagerImpl<>(clientsHolder, messageStore);
    }

    @Override
    public MessageManager<BODY, ID> messageManager() {
        return messageManager;
    }

    @Override
    public SessionManager<BODY, ID> sessionManager() {
        return sessionManager;
    }

}
