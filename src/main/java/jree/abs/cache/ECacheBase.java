package jree.abs.cache;

import jree.api.FailReason;
import jree.api.OperationResultListener;
import org.ehcache.Cache;

public class ECacheBase<K, V> {

    public interface DataResolver<K, V> {

        DataResolver EMPTY = new DataResolver() {
            @Override
            public void resolve(Object key, Object attachment, OperationResultListener valueListener) {
                valueListener.onSuccess(null);
            }
        };

        void resolve(K key, Object attachment, OperationResultListener<V> valueListener);

    }

    private final Cache<K, V> cache;
    private final DataResolver<K, V> resolver;

    public ECacheBase(Cache<K, V> cache, DataResolver<K, V> resolver) {
        this.cache = cache;
        this.resolver = resolver;
    }

    void get(K key, Object attachment, OperationResultListener<V> listener) {
        V v = cache.get(key);
        if (v != null) {
            listener.onSuccess(v);
        } else {
            resolver.resolve(key, attachment, new OperationResultListener<V>() {
                @Override
                public void onSuccess(V result) {
                    cache.put(key, result);
                    listener.onSuccess(result);
                }

                @Override
                public void onFailed(FailReason reason) {
                    listener.onFailed(reason);
                }
            });
        }
    }

    void get(K key, OperationResultListener<V> listener) {
        get(key, null, listener);
    }

    void remove(K key){
        cache.remove(key);
    }

    public Cache<K, V> getCache() {
        return cache;
    }
}
