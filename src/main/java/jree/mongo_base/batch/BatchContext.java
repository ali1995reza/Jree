package jree.mongo_base.batch;

import com.mongodb.Function;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import jree.mongo_base.CollectionInfo;
import org.bson.BsonValue;
import org.bson.Document;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

public class BatchContext {

    private final Map<Object, UpdateBatch> updateBatches = new ConcurrentHashMap<>();
    private final Map<Object, InsertBatch> insertBatches = new ConcurrentHashMap<>();
    private final Map<Object, FindByIdBatch> findBatches = new ConcurrentHashMap<>();
    private final Map<Object, UpsertBatch> upsertBatches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService;

    public BatchContext(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }


    public synchronized UpdateBatch createNewUpdateBatch(Object key, AsyncMongoCollection<Document> collection, int size, int timeout) {
        if (updateBatches.containsKey(key))
            throw new IllegalStateException("this batch already exists");

        UpdateBatch updateBatch = new UpdateBatch(
                collection, executorService, size, timeout
        );
        updateBatches.put(key, updateBatch);

        return updateBatch;

    }

    public synchronized UpdateBatch createNewUpdateBatch(CollectionInfo info, int size, int timeout) {
        UpdateBatch batch = createNewUpdateBatch(info, info.collection(), size, timeout);
        info.setUpdateBatch(batch);
        return batch;
    }

    public synchronized InsertBatch createNewInsertBatch(Object key, AsyncMongoCollection<Document> collection, int size, int timeout) {
        if (insertBatches.containsKey(key))
            throw new IllegalStateException("this batch already exists");

        InsertBatch insertBatch = new InsertBatch(
                collection, executorService, size, timeout
        );
        insertBatches.put(key, insertBatch);

        return insertBatch;

    }

    public synchronized InsertBatch createNewInsertBatch(CollectionInfo info, int size, int timeout) {
        InsertBatch batch = createNewInsertBatch(info, info.collection(), size, timeout);
        info.setInsertBatch(batch);
        return batch;
    }

    public synchronized <T> FindByIdBatch<T> createNewFindBatch(Object key, AsyncMongoCollection<Document> collection, int size, int timeout) {
        if (findBatches.containsKey(key))
            throw new IllegalStateException("this batch already exists");

        FindByIdBatch<T> findBatch = new FindByIdBatch<>(collection, executorService, size, timeout);

        findBatches.put(key, findBatch);

        return findBatch;
    }

    public synchronized <T> FindByIdBatch<T> createNewFindBatch(CollectionInfo info, int size, int timeout) {
        FindByIdBatch<T> batch = createNewFindBatch(info, info.collection(), size, timeout);
        info.setFindByIdBatch(batch);
        return batch;
    }


    public synchronized <T> UpsertBatch<T> createNewUpsertBatch(Object key,
                                                                AsyncMongoCollection<Document> collection,
                                                                Function<BsonValue, T> valueFetcher,
                                                                int size,
                                                                int timeout) {

        if (upsertBatches.containsKey(key))
            throw new IllegalStateException("this batch already exists");

        UpsertBatch<T> upsertBatch = new UpsertBatch<>(
                collection,
                valueFetcher,
                executorService,
                size,
                timeout
        );

        upsertBatches.put(key, upsertBatch);

        return upsertBatch;
    }

    public synchronized <T> UpsertBatch<T> createNewUpsertBatch(CollectionInfo info,
                                                                Function<BsonValue, T> valueFetcher,
                                                                int size,
                                                                int timeout) {

        UpsertBatch<T> upsertBatch = createNewUpsertBatch(info, info.collection(), valueFetcher, size, timeout);
        info.setUpsertBatch(upsertBatch);
        return upsertBatch;
    }

    public UpdateBatch getUpdateBatch(Object key) {
        return updateBatches.get(key);
    }

    public InsertBatch getInsertBatch(Object key) {
        return insertBatches.get(key);
    }

    public <T> FindByIdBatch<T> getFindBatch(Object key) {
        return findBatches.get(key);
    }

    public <T> UpsertBatch<T> getUpsertBatch(Object key){
        return upsertBatches.get(key);
    }
}
