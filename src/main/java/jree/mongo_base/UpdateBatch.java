package jree.mongo_base;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import jree.util.BatchExecutor;
import jree.util.Converter;
import jree.util.ConverterList;
import org.bson.Document;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public class UpdateBatch extends BatchExecutor<UpdateBatch.AsyncUpdateManyModel> {


    public final static class AsyncUpdateManyModel{
        private final UpdateManyModel<Document> update;
        public final SingleResultCallback<Void> callback;

        public AsyncUpdateManyModel(UpdateManyModel<Document> update, SingleResultCallback<Void> callback) {
            this.update = update;
            this.callback = callback;
        }
    }



    private final static class BatchCallbackHandler implements SingleResultCallback<BulkWriteResult>{

        private final List<AsyncUpdateManyModel> batch;
        private final List<WriteModel<Document>> writeModels;
        private final AsyncMongoCollection<Document> collection;
        private int retries;

        private BatchCallbackHandler(List<AsyncUpdateManyModel> batch,
                                     List<WriteModel<Document>> writeModels, AsyncMongoCollection<Document> collection,
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
                for(AsyncUpdateManyModel m:batch)
                {
                    m.callback.onResult(null , null);
                }
            }else {
                for(AsyncUpdateManyModel m:batch)
                {
                    m.callback.onResult(null , throwable);
                }
            }
        }
    }

    private final static Converter<UpdateBatch.AsyncUpdateManyModel, WriteModel<Document>> CONVERTER = new Converter<UpdateBatch.AsyncUpdateManyModel, WriteModel<Document>>() {
        @Override
        public WriteModel<Document> convert(UpdateBatch.AsyncUpdateManyModel asyncInsertModel) {
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

    public UpdateBatch putInBatch(UpdateManyModel<Document> insert , SingleResultCallback<Void> callback)
    {
        putInBatch(new AsyncUpdateManyModel(insert, callback));
        return this;
    }

    @Override
    public void executeBatch(List<AsyncUpdateManyModel> batch) {
        List<WriteModel<Document>> writeList = new ConverterList<>(batch,CONVERTER);
        collection.bulkWrite(
                writeList ,
                new BatchCallbackHandler(batch, writeList, collection , 2)
        );
    }


}
