package jree.abs;

import jree.abs.parts.DetailsStore;
import jree.abs.parts.IdBuilder;
import jree.abs.parts.MessageStore;
import jree.api.PubSubSystem;
import jree.util.Assertion;

public final class PubSubSystemBuilder<BODY, ID extends Comparable<ID>> {

    public static <BODY, ID extends Comparable<ID>> PubSubSystemBuilder<BODY, ID> newBuilder(Class<BODY> bodyClass, Class<ID> idClass) {
        return new PubSubSystemBuilder<>();
    }

    private MessageStore<BODY, ID> messageStore;
    private DetailsStore<ID> detailsStore;
    private IdBuilder<ID> idBuilder;

    private PubSubSystemBuilder() {
    }

    public PubSubSystemBuilder<BODY, ID> setDetailsStore(DetailsStore<ID> detailsStore) {
        this.detailsStore = detailsStore;
        return this;
    }

    public PubSubSystemBuilder<BODY, ID> setMessageStore(MessageStore<BODY, ID> messageStore) {
        this.messageStore = messageStore;
        return this;
    }

    public PubSubSystemBuilder<BODY, ID> setIdBuilder(IdBuilder<ID> idBuilder) {
        this.idBuilder = idBuilder;
        return this;
    }

    public PubSubSystem<BODY, ID> build() {
        Assertion.ifOneNull("message store or details store is null", messageStore, detailsStore, idBuilder);
        return new PubSubSystemImpl<>(messageStore, detailsStore, idBuilder);
    }

}
