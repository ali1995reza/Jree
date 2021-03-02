package jree.mongo_base;

import com.couchbase.client.java.ReactiveScope;
import com.mongodb.internal.async.SingleResultCallback;
import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.api.Relation;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.EhcacheManager;

import java.security.acl.LastOwnerException;

public class RelationAndExistenceCache {


    private final MongoClientDetailsStore detailsStore;
    private final CacheManager manager;
    private final ECacheBase<String , Boolean> sessionExistence;

    public RelationAndExistenceCache(MongoClientDetailsStore detailsStore) {
        this.detailsStore = detailsStore;
        manager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("Sessions" ,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Boolean.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .offheap(100, MemoryUnit.MB)).build())
                        .build();
        this.sessionExistence = new ECacheBase<>(manager.getCache("Sessions",String.class, Boolean.class),
                new ECacheBase.DataResolver<String, Boolean>() {
                    @Override
                    public void resolve(String key, OperationResultListener<Boolean> valueListener) {
                        String[] split = key.split("_");
                        long c = Long.parseLong(split[0]);
                        long s = Long.parseLong(split[1]);
                        detailsStore.isSessionExists(c, s, new SingleResultCallback<Boolean>() {
                            @Override
                            public void onResult(Boolean aBoolean, Throwable throwable) {
                                if(throwable!=null)
                                {
                                    valueListener.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                                }else {
                                    valueListener.onSuccess(aBoolean);
                                }
                            }
                        });
                    }
                });
    }


    public void getRelationFor(String conversationId , OperationResultListener<Relation> result)
    {

    }
}
