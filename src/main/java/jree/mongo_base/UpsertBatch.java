package jree.mongo_base;

import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import jree.util.BatchExecutor;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class UpsertBatch<T> extends BatchExecutor<UpsertBatch.AsyncUpsertModel<T>> {

    public final static class AsyncUpsertModel<T>{

        private final T id;
        private final UpdateOneModel<Document> update;
        private final SingleResultCallback<Boolean> callback;

        public AsyncUpsertModel(T id,
                                UpdateOneModel<Document> update,
                                SingleResultCallback<Boolean> callback) {
            this.id = id;
            this.update = update;
            this.callback = callback;
        }
    }



    private final AsyncMongoCollection<Document> collection;
    private Map<T , AsyncUpsertModel<T>> upsertIndex;

    public UpsertBatch(AsyncMongoCollection<Document> collection,
                       ScheduledExecutorService service,
                       int batchSize,
                       long timeOut) {

        super(service, batchSize, timeOut);
        this.collection = collection;

    }

    @Override
    public void constructBatch(int size) {
        upsertIndex = new HashMap<>(size);
        super.constructBatch(size);
    }

    @Override
    public BatchExecutor<AsyncUpsertModel<T>> putInBatch(AsyncUpsertModel<T> tAsyncUpsertModel) {
        throw new IllegalStateException("can not use this method directly");
    }

    @Override
    public boolean addToBatch(AsyncUpsertModel<T> tAsyncUpsertModel, List<AsyncUpsertModel<T>> batch) {
        return super.addToBatch(tAsyncUpsertModel, batch);
    }

    @Override
    public void executeBatch(List<AsyncUpsertModel<T>> batch) {

    }
}
