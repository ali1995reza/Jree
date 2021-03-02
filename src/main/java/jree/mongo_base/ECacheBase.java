package jree.mongo_base;

import jree.api.FailReason;
import jree.api.OperationResultListener;
import org.ehcache.Cache;

public class ECacheBase<K , V> {

    public interface DataResolver<K , V>{

        DataResolver EMPTY = new DataResolver() {
            @Override
            public void resolve(Object key, OperationResultListener valueListener) {
                valueListener.onSuccess(null);
            }
        };

        void resolve(K key , OperationResultListener<V> valueListener);

    }


    private final Cache<K , V> cache;
    private final DataResolver<K , V> resolver;

    public ECacheBase(Cache<K, V> cache, DataResolver<K, V> resolver) {
        this.cache = cache;
        this.resolver = resolver;
    }


    void get(K key , OperationResultListener<V> listener){

        V v = cache.get(key);
        if(v!=null)
        {
            listener.onSuccess(v);
        }else {

            resolver.resolve(key, new OperationResultListener<V>() {
                @Override
                public void onSuccess(V result) {
                    cache.put(key , result);
                    listener.onSuccess(result);
                }

                @Override
                public void onFailed(FailReason reason) {
                    listener.onFailed(reason);
                }
            });
        }

    }
}
