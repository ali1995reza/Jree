package jree.mongo_base.batch;

import com.mongodb.Function;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import org.bson.BsonValue;
import org.bson.Document;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

public class BatchContext {

    private final Map<String, UpdateBatch> updateBatches = new ConcurrentHashMap<>();
    private final Map<String, InsertBatch> insertBatches = new ConcurrentHashMap<>();
    private final Map<String, FindByIdBatch> findBatches = new ConcurrentHashMap<>();
    private final Map<String, UpsertBatch> upsertBatches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService;

    public BatchContext(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }


    public synchronized UpdateBatch createNewUpdateBatch(String name, AsyncMongoCollection<Document> collection, int size, int timeout) {
        if (updateBatches.containsKey(name))
            throw new IllegalStateException("this batch already exists");

        UpdateBatch updateBatch = new UpdateBatch(
                collection, executorService, size, timeout
        );
        updateBatches.put(name, updateBatch);

        return updateBatch;

    }

    public synchronized InsertBatch createNewInsertBatch(String name, AsyncMongoCollection<Document> collection, int size, int timeout) {
        if (insertBatches.containsKey(name))
            throw new IllegalStateException("this batch already exists");

        InsertBatch insertBatch = new InsertBatch(
                collection, executorService, size, timeout
        );
        insertBatches.put(name, insertBatch);

        return insertBatch;

    }

    public synchronized <T> FindByIdBatch<T> createNewFindBatch(String name, AsyncMongoCollection<Document> collection, int size, int timeout) {
        if (findBatches.containsKey(name))
            throw new IllegalStateException("this batch already exists");

        FindByIdBatch<T> findBatch = new FindByIdBatch<>(collection, executorService, size, timeout);

        findBatches.put(name, findBatch);

        return findBatch;
    }


    public synchronized <T> UpsertBatch<T> createNewUpsertBatch(String name,
                                                                AsyncMongoCollection<Document> collection,
                                                                Function<BsonValue, T> valueFetcher,
                                                                int size,
                                                                int timeout) {

        if (upsertBatches.containsKey(name))
            throw new IllegalStateException("this batch already exists");

        UpsertBatch<T> upsertBatch = new UpsertBatch<>(
                collection,
                valueFetcher,
                executorService,
                size,
                timeout
        );

        upsertBatches.put(name, upsertBatch);

        return upsertBatch;
    }

    public UpdateBatch getUpdateBatch(String name) {
        return updateBatches.get(name);
    }

    public InsertBatch getInsertBatch(String name) {
        return insertBatches.get(name);
    }

    public <T> FindByIdBatch<T> getFindBatch(String name) {
        return findBatches.get(name);
    }

    public <T> UpsertBatch<T> getUpsertBatch(String name){
        return upsertBatches.get(name);
    }
}
