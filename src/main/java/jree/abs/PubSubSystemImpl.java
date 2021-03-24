package jree.abs;

import jree.abs.parts.DetailsStore;
import jree.abs.parts.IdBuilder;
import jree.abs.parts.MessageStore;
import jree.api.MessageManager;
import jree.api.PubSubSystem;
import jree.api.SessionManager;

final class PubSubSystemImpl<BODY, ID extends Comparable<ID>> implements PubSubSystem<BODY, ID> {

    private final ClientsHolder clientsHolder;
    private final MessageStore<BODY, ID> messageStore;
    private final DetailsStore<ID> detailsStore;
    private final MessageManagerImpl<BODY, ID> messageManager;
    private final SessionManagerImpl<BODY, ID> sessionManager;
    private final IdBuilder<ID> idBuilder;

    public PubSubSystemImpl(MessageStore<BODY, ID> messageStore, DetailsStore<ID> detailsStore, IdBuilder<ID> idBuilder) {
        this.idBuilder = idBuilder;
        this.clientsHolder = new ClientsHolder();
        this.messageStore = messageStore;
        this.detailsStore = detailsStore;
        this.sessionManager = new SessionManagerImpl<>(messageStore, detailsStore, clientsHolder, new ConversationSubscribersHolder<>(clientsHolder, "jdbc:h2:mem:db1"), this.idBuilder);
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
