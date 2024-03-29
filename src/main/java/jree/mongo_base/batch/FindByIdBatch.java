package jree.mongo_base.batch;

import com.mongodb.Block;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import jree.util.BatchExecutor;
import jutils.collection.ListMapper;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static com.mongodb.client.model.Filters.in;

public class FindByIdBatch<T> extends BatchExecutor<FindByIdBatch.AsyncFindByIdModel<T>> {

    private final static String ID = "_id";

    public final static class AsyncFindByIdModel<T> {

        private final T id;
        private Object listener;


        public AsyncFindByIdModel(T id, SingleResultCallback<Document> listener) {
            this.listener = listener;
            this.id = id;
        }

        public T getId() {
            return id;
        }

        private void batchListener(SingleResultCallback<Document> listener) {
            if (this.listener instanceof SingleResultCallback) {
                List list = new ArrayList(5);
                list.add(this.listener);
                list.add(listener);

                this.listener = list;
            } else {

                List list = (List) this.listener;
                list.add(listener);
            }
        }

        private void executeWithException(Throwable t) {
            if (listener instanceof SingleResultCallback) {
                SingleResultCallback callback = ((SingleResultCallback) listener);
                callback.onResult(null, t);
            } else {
                List<SingleResultCallback> callbacks = (List<SingleResultCallback>) listener;
                for (SingleResultCallback callback : callbacks) {
                    callback.onResult(null, t);
                }
            }
        }

        private void executeWithResult(Object t) {

            if (listener instanceof SingleResultCallback) {
                SingleResultCallback callback = ((SingleResultCallback) listener);
                callback.onResult(t, null);
            } else {
                List<SingleResultCallback> callbacks = (List<SingleResultCallback>) listener;
                for (SingleResultCallback callback : callbacks) {
                    callback.onResult(t, null);
                }
            }
        }
    }


    private final class BatchCallbackHandler implements Block<Document>, SingleResultCallback<Void> {

        private final Map<T, AsyncFindByIdModel<T>> index;

        private BatchCallbackHandler(Map<T, AsyncFindByIdModel<T>> index) {
            this.index = index;
        }


        @Override
        public void apply(Document document) {
            T id = (T) document.get(ID);
            AsyncFindByIdModel<T> model = index.remove(id);
            if (model == null)
                return;
            model.executeWithResult(document);
        }

        @Override
        public void onResult(Void aVoid, Throwable throwable) {
            if (throwable != null) {
                for (AsyncFindByIdModel<T> model : index.values()) {
                    model.executeWithException(throwable);
                }
            } else {

                for (AsyncFindByIdModel<T> model : index.values()) {
                    model.executeWithResult(null);
                }
            }
        }
    }


    private Map<T, AsyncFindByIdModel<T>> batchIndex;
    private Map<T, AsyncFindByIdModel<T>> capturedIndex;
    private final AsyncMongoCollection<Document> collection;

    public FindByIdBatch(AsyncMongoCollection<Document> collection, ScheduledExecutorService service, int batchSize, long timeOut) {
        super(service, batchSize, timeOut);
        this.collection = collection;
    }


    @Override
    public void constructBatch(int size) {
        capturedIndex = batchIndex;
        batchIndex = new HashMap<>(size);
        super.constructBatch(size);
    }

    @Override
    public boolean addToBatch(AsyncFindByIdModel<T> asyncFindByIdModel, List<AsyncFindByIdModel<T>> batch) {
        AsyncFindByIdModel<T> currentModel = batchIndex.get(asyncFindByIdModel.id);

        if (currentModel != null) {
            currentModel.batchListener((SingleResultCallback<Document>) asyncFindByIdModel.listener);
            return true;

        } else {
            if(super.addToBatch(asyncFindByIdModel, batch)){
                batchIndex.put(asyncFindByIdModel.id, asyncFindByIdModel);
                return true;
            }
            return false;
        }

    }

    public BatchExecutor<AsyncFindByIdModel<T>> findOne(T id, SingleResultCallback<Document> callback)
    {
        return putInBatch(new AsyncFindByIdModel<>(id , callback));
    }

    @Override
    public void executeBatch(List<AsyncFindByIdModel<T>> batch) {
        List<T> ids = ListMapper.map(batch, AsyncFindByIdModel::getId);
        BatchCallbackHandler handler = new BatchCallbackHandler(capturedIndex);
        collection.find(in(ID, ids)).forEach(handler, handler);
    }
}
