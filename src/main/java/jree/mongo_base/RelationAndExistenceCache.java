package jree.mongo_base;

import com.mongodb.internal.async.SingleResultCallback;
import jree.api.*;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

public class RelationAndExistenceCache {


    private final MongoClientDetailsStore detailsStore;
    private final CacheManager manager;
    private final ECacheBase<String, Boolean> sessionExistence;
    private final ECacheBase<String, Relation> relationCache;

    public RelationAndExistenceCache(MongoClientDetailsStore detailsStore) {
        this.detailsStore = detailsStore;
        manager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("Sessions",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Boolean.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(100, MemoryUnit.MB)).build())
                .withCache("Relations",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Relation.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(100, MemoryUnit.MB)).build())
                .build();
        manager.init();
        this.sessionExistence = new ECacheBase<>(manager.getCache("Sessions", String.class, Boolean.class),
                new ECacheBase.DataResolver<String, Boolean>() {
                    @Override
                    public void resolve(String key, OperationResultListener<Boolean> valueListener) {


                        String[] split = key.split("_");
                        long c = Long.parseLong(split[0]);

                        if(split.length==2) {
                            long s = Long.parseLong(split[1]);
                            detailsStore.isSessionExists(c, s, new SingleResultCallback<Boolean>() {
                                @Override
                                public void onResult(Boolean aBoolean, Throwable throwable) {
                                    if (throwable != null) {
                                        valueListener.onFailed(new FailReason(throwable, MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                                    } else {
                                        valueListener.onSuccess(aBoolean);
                                    }
                                }
                            });
                        }else {
                            detailsStore.isClientExists(c , new SingleResultCallback<Boolean>() {
                                @Override
                                public void onResult(Boolean aBoolean, Throwable throwable) {
                                    if (throwable != null) {
                                        valueListener.onFailed(new FailReason(throwable, MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                                    } else {
                                        valueListener.onSuccess(aBoolean);
                                    }
                                }
                            });
                        }
                    }
                });

        this.relationCache = new ECacheBase<>(manager.getCache("Relations", String.class, Relation.class),
                detailsStore::getRelation);
    }

    private String uniqueId(Recipient recipient)
    {
        if(recipient.session()<0)
            return String.valueOf(recipient.client());
        return String.valueOf(recipient.client()).concat("_").concat(
                String.valueOf(recipient.session())
        );
    }

    public void getRelationFor(Session publisher,
                               Recipient recipient,
                               OperationResultListener<Relation> resultListener) {

        String conversation = StaticFunctions.uniqueConversationId(publisher , recipient);

        sessionExistence.get(uniqueId(recipient), new OperationResultListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {

                if(result)
                {
                    relationCache.get(conversation , resultListener);
                }else {
                    resultListener.onFailed(new FailReason(MongoFailReasonsCodes.SESSION_NOT_EXISTS));
                }

            }

            @Override
            public void onFailed(FailReason reason) {
                resultListener.onFailed(reason);
            }
        });

    }
}
