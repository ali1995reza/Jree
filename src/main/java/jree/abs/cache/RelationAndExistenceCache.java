package jree.abs.cache;

import jree.abs.codes.FailReasonsCodes;
import jree.abs.parts.DetailsStore;
import jree.abs.parts.MessageStore;
import jree.abs.utils.StaticFunctions;
import jree.api.*;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

public class RelationAndExistenceCache<ID> {

    private final DetailsStore<ID> detailsStore;
    private final MessageStore messageStore;
    private final CacheManager manager;
    private final ECacheBase<String, Boolean> existence;
    private final ECacheBase<String, Relation> relationCache;

    public RelationAndExistenceCache(DetailsStore<ID> d, MessageStore m) {
        this.detailsStore = d;
        this.messageStore = m;
        manager = CacheManagerBuilder.newCacheManagerBuilder().withCache("Sessions", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Boolean.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100, MemoryUnit.MB)).build()).withCache("Relations", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Relation.class, ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100, MemoryUnit.MB)).build()).build();
        manager.init();
        this.existence = new ECacheBase<>(manager.getCache("Sessions", String.class, Boolean.class), new ECacheBase.DataResolver<String, Boolean>() {
            @Override
            public void resolve(String key, Object attachment, OperationResultListener<Boolean> valueListener) {
                Recipient recipient = (Recipient) attachment;
                if (recipient.session() > 0) {
                    detailsStore.isSessionExists(recipient.client(), recipient.session(), valueListener);
                } else if (recipient.client() > 0) {
                    detailsStore.isClientExists(recipient.client(), valueListener);
                } else {
                    messageStore.isConversationExists(recipient.conversation(), valueListener);
                }
            }
        });
        this.relationCache = new ECacheBase<>(manager.getCache("Relations", String.class, Relation.class), new ECacheBase.DataResolver<String, Relation>() {
            @Override
            public void resolve(String key, Object attachment, OperationResultListener<Relation> valueListener) {
                Object[] data = (Object[]) attachment;
                detailsStore.getRelation((Session) data[0], (Recipient) data[1], valueListener);
            }
        });
    }

    private String uniqueId(Recipient recipient) {
        if (recipient.session() > 0) {
            return String.valueOf(recipient.client()).concat("_").concat(String.valueOf(recipient.session()));
        } else if (recipient.client() > 0) {
            return String.valueOf(recipient.client());
        } else {
            return "CONV_" + recipient.conversation();
        }
    }

    public void getRelation(Session publisher, Recipient recipient, OperationResultListener<Relation> resultListener) {
        String conversation = StaticFunctions.uniqueConversationId(publisher, recipient);
        existence.get(uniqueId(recipient), recipient, new OperationResultListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    relationCache.get(conversation, new Object[]{publisher, recipient}, resultListener);
                } else {
                    resultListener.onFailed(new FailReason(FailReasonsCodes.SESSION_NOT_EXISTS));
                }
            }

            @Override
            public void onFailed(FailReason reason) {
                resultListener.onFailed(reason);
            }
        });
    }

    public void isExists(Recipient recipient, OperationResultListener<Boolean> callback) {
        existence.get(uniqueId(recipient), recipient, callback);
    }

}
