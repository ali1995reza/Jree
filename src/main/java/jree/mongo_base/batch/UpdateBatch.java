package jree.mongo_base.batch;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import jree.util.BatchExecutor;
import jree.util.Converter;
import jree.util.ConverterList;
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

        public AsyncUpdateModel(UpdateOneModel<Document> update, SingleResultCallback<Void> callback)
        {
            this.update = update;
            this.callback = callback;
        }
    }



    private final static class BatchCallbackHandler implements SingleResultCallback<BulkWriteResult>{

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
            if(throwable!=null)
            {
                if(--retries>0)
                {
                    collection.bulkWrite(
                            writeModels ,
                            this
                    );
                    return;
                }
                for(AsyncUpdateModel m:batch)
                {
                    m.callback.onResult(null , throwable);
                }
            }else {
                for(AsyncUpdateModel m:batch)
                {
                    m.callback.onResult(null , null);
                }
            }
        }
    }

    private final static Converter<AsyncUpdateModel, WriteModel<Document>> CONVERTER = new Converter<AsyncUpdateModel, WriteModel<Document>>() {
        @Override
        public WriteModel<Document> convert(AsyncUpdateModel asyncInsertModel) {
            return asyncInsertModel.update;
        }
    };



    private final AsyncMongoCollection<Document> collection;

    public UpdateBatch(AsyncMongoCollection<Document> collection ,
                       ScheduledExecutorService service,
                       int batchSize, long timeOut) {
        super(service, batchSize, timeOut);
        this.collection = collection;
    }

    public UpdateBatch putInBatch(UpdateManyModel<Document> update , SingleResultCallback<Void> callback)
    {
        putInBatch(new AsyncUpdateModel(update, callback));
        return this;
    }

    public UpdateBatch putInBatch(UpdateOneModel<Document> update , SingleResultCallback<Void> callback)
    {
        putInBatch(new AsyncUpdateModel(update, callback));
        return this;
    }

    public UpdateBatch updateOne(Bson filter , Bson update , UpdateOptions options , SingleResultCallback<Void> callback)
    {
        return putInBatch(new UpdateOneModel<Document>(
                filter , update , options
        ) , callback);
    }

    public UpdateBatch updateOne(Bson filter , Bson update , SingleResultCallback<Void> callback)
    {
        return putInBatch(new UpdateOneModel<Document>(
                filter , update
        ) , callback);
    }

    public UpdateBatch updateMany(Bson filter , Bson update , UpdateOptions options , SingleResultCallback<Void> callback)
    {
        return putInBatch(new UpdateManyModel<Document>(
                filter , update , options
        ) , callback);
    }

    public UpdateBatch updateMany(Bson filter , Bson update , SingleResultCallback<Void> callback)
    {
        return putInBatch(new UpdateManyModel<Document>(
                filter , update
        ) , callback);
    }

    @Override
    public void executeBatch(List<AsyncUpdateModel> batch) {
        List<WriteModel<Document>> writeList = new ConverterList<>(batch,CONVERTER);
        collection.bulkWrite(
                writeList ,
                new BatchCallbackHandler(batch, writeList, collection , 2)
        );
    }


}
