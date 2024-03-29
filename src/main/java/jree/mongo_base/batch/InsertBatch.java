package jree.mongo_base.batch;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import jree.util.BatchExecutor;
import jutils.collection.ListMapper;
import org.bson.Document;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public class InsertBatch extends BatchExecutor<InsertBatch.AsyncInsertModel> {

    public static class AsyncInsertModel{
        public final InsertOneModel<Document> insert;
        public final SingleResultCallback<Document> callback;

        public AsyncInsertModel(InsertOneModel<Document> insert, SingleResultCallback<Document> callback) {
            this.insert = insert;
            this.callback = callback;
        }

        public InsertOneModel<Document> getInsert() {
            return insert;
        }

        public SingleResultCallback<Document> getCallback() {
            return callback;
        }
    }

    private final static class BatchCallBackHandler implements SingleResultCallback<BulkWriteResult>{



        private final List<AsyncInsertModel> batch;

        private BatchCallBackHandler(List<AsyncInsertModel> batch) {
            this.batch = batch;
        }


        @Override
        public void onResult(BulkWriteResult bulkWriteResult, Throwable throwable) {
            int totalInserted = bulkWriteResult.getInsertedCount();

            for(int i=0;i<totalInserted;i++)
            {
                batch.get(i).callback
                        .onResult(batch.get(i).insert.getDocument() , null);
            }

            if(totalInserted==batch.size()) return;

            throwable = throwable==null?
                    new IllegalStateException("document insertion fail"):throwable;


            for(int i=totalInserted;i<batch.size();i++)
            {
                batch.get(i).callback
                        .onResult(null , throwable);
            }
        }
    }




    private final AsyncMongoCollection<Document> collection;

    public InsertBatch(
            AsyncMongoCollection<Document> collection,
            ScheduledExecutorService service, int batchSize, long timeOut) {
        super(service, batchSize, timeOut);
        this.collection = collection;
    }

    public InsertBatch putInBatch(InsertOneModel<Document> insert , SingleResultCallback<Document> callback)
    {
        putInBatch(new AsyncInsertModel(insert, callback));
        return this;
    }

    public InsertBatch insertOne(Document insert , SingleResultCallback<Document> callback)
    {
        return putInBatch(new InsertOneModel<Document>(insert) , callback);
    }


    @Override
    public void executeBatch(List<AsyncInsertModel> batch) {
        List<WriteModel<Document>> writeList = ListMapper.map(batch, AsyncInsertModel::getInsert);
        collection.bulkWrite(writeList,new BatchCallBackHandler(batch));
    }

}
