package jree.mongo_base;

import com.mongodb.internal.async.client.AsyncMongoCollection;
import org.bson.Document;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

public class BatchContext {

    private final Map<String , UpdateBatch> updateBatches = new ConcurrentHashMap<>();
    private final Map<String , InsertBatch> insertBatches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService;

    public BatchContext(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }


    public synchronized UpdateBatch createNewUpdateBatch(String name , AsyncMongoCollection<Document> collection , int size , int timeout)
    {
        if(updateBatches.containsKey(name))
            throw new IllegalStateException("this batch already exists");

        UpdateBatch updateBatch = new UpdateBatch(
                collection , executorService , size , timeout
        );
        updateBatches.put(name , updateBatch);

        return updateBatch;

    }

    public synchronized InsertBatch createNewInsertBatch(String name , AsyncMongoCollection<Document> collection , int size , int timeout)
    {
        if(insertBatches.containsKey(name))
            throw new IllegalStateException("this batch already exists");

        InsertBatch insertBatch = new InsertBatch(
                collection , executorService , size , timeout
        );
        insertBatches.put(name , insertBatch);

        return insertBatch;

    }

    public UpdateBatch getUpdateBatch(String name)
    {
        return updateBatches.get(name);
    }

    public InsertBatch getInsertBatch(String name)
    {
        return insertBatches.get(name);
    }
}
