package jree.mongo_base.batch;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.*;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import jree.util.BatchExecutor;
import jutils.collection.ListMapper;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public class UpdateBatch extends BatchExecutor<UpdateBatch.AsyncUpdateModel> {


    public final static class AsyncUpdateModel {
        private final WriteModel<Document> update;
        public final SingleResultCallback<Void> callback;

        public AsyncUpdateModel(UpdateManyModel<Document> update, SingleResultCallback<Void> callback) {
            this.update = update;
            this.callback = callback;
        }

        public AsyncUpdateModel(UpdateOneModel<Document> update, SingleResultCallback<Void> callback) {
            this.update = update;
            this.callback = callback;
        }

        public AsyncUpdateModel(DeleteOneModel<Document> delete, SingleResultCallback<Void> callback) {
            this.update = delete;
            this.callback = callback;
        }

        public AsyncUpdateModel(DeleteManyModel<Document> delete, SingleResultCallback<Void> callback) {
            this.update = delete;
            this.callback = callback;
        }

        public WriteModel<Document> getUpdate() {
            return update;
        }

        public SingleResultCallback<Void> getCallback() {
            return callback;
        }
    }


    private final static class BatchCallbackHandler implements SingleResultCallback<BulkWriteResult> {

        private final List<AsyncUpdateModel> batch;
        private final List<WriteModel<Document>> writeModels;
        private final AsyncMongoCollection<Document> collection;
        private int retries;

        private BatchCallbackHandler(List<AsyncUpdateModel> batch,
                                     List<WriteModel<Document>> writeModels,
                                     AsyncMongoCollection<Document> collection,
                                     int retries) {
            this.batch = batch;
            this.writeModels = writeModels;
            this.collection = collection;
            this.retries = retries;
        }


        @Override
        public void onResult(BulkWriteResult bulkWriteResult, Throwable throwable) {
            if (throwable != null) {
                if (--retries > 0) {
                    collection.bulkWrite(
                            writeModels,
                            this
                    );
                    return;
                }
                for (AsyncUpdateModel m : batch) {
                    m.callback.onResult(null, throwable);
                }
            } else {
                for (AsyncUpdateModel m : batch) {
                    m.callback.onResult(null, null);
                }
            }
        }
    }


    private final AsyncMongoCollection<Document> collection;

    public UpdateBatch(AsyncMongoCollection<Document> collection,
                       ScheduledExecutorService service,
                       int batchSize, long timeOut) {
        super(service, batchSize, timeOut);
        this.collection = collection;
    }

    public UpdateBatch putInBatch(UpdateManyModel<Document> update, SingleResultCallback<Void> callback) {
        putInBatch(new AsyncUpdateModel(update, callback));
        return this;
    }

    public UpdateBatch putInBatch(UpdateOneModel<Document> update, SingleResultCallback<Void> callback) {
        putInBatch(new AsyncUpdateModel(update, callback));
        return this;
    }

    public UpdateBatch putInBatch(DeleteOneModel<Document> delete, SingleResultCallback<Void> callback) {
        putInBatch(new AsyncUpdateModel(delete, callback));
        return this;
    }

    public UpdateBatch putInBatch(DeleteManyModel<Document> delete, SingleResultCallback<Void> callback) {
        putInBatch(new AsyncUpdateModel(delete, callback));
        return this;
    }

    public UpdateBatch updateOne(Bson filter, Bson update, UpdateOptions options, SingleResultCallback<Void> callback) {
        return putInBatch(new UpdateOneModel<Document>(
                filter, update, options
        ), callback);
    }

    public UpdateBatch updateOne(Bson filter, Bson update, SingleResultCallback<Void> callback) {
        return putInBatch(new UpdateOneModel<Document>(
                filter, update
        ), callback);
    }

    public UpdateBatch updateMany(Bson filter, Bson update, UpdateOptions options, SingleResultCallback<Void> callback) {
        return putInBatch(new UpdateManyModel<Document>(
                filter, update, options
        ), callback);
    }

    public UpdateBatch updateMany(Bson filter, Bson update, SingleResultCallback<Void> callback) {
        return putInBatch(new UpdateManyModel<Document>(
                filter, update
        ), callback);
    }

    public UpdateBatch deleteOne(Bson filter, SingleResultCallback<Void> callback) {
        return putInBatch(new DeleteOneModel<>(filter), callback);
    }

    public UpdateBatch deleteMany(Bson filter, SingleResultCallback<Void> callback) {
        return putInBatch(new DeleteManyModel<>(filter), callback);
    }

    @Override
    public void executeBatch(List<AsyncUpdateModel> batch) {
        List<WriteModel<Document>> writeList = ListMapper.map(batch, AsyncUpdateModel::getUpdate);
        collection.bulkWrite(
                writeList,
                new BatchCallbackHandler(batch, writeList, collection, 2)
        );
    }


}
