package jree.mongo_base.batch;

import com.mongodb.Function;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import jree.util.BatchExecutor;
import jutils.collection.ListMapper;
import jutils.collection.Mapper;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class UpsertBatch<T> extends BatchExecutor<UpsertBatch.AsyncUpsertModel<T>> {

    public final static Function<BsonValue, Long> LONG_FETCHER = new Function<BsonValue, Long>() {
        @Override
        public Long apply(BsonValue bsonValue) {
            return bsonValue.asInt64().getValue();
        }
    };

    public final static Function<BsonValue, String> STRING_FETCHER = new Function<BsonValue, String>() {
        @Override
        public String apply(BsonValue bsonValue) {
            return bsonValue.asString().getValue();
        }
    };

    public final static Function<BsonValue, ObjectId> OBJECT_ID_FETCHER = new Function<BsonValue, ObjectId>() {
        @Override
        public ObjectId apply(BsonValue bsonValue) {
            return bsonValue.asObjectId().getValue();
        }
    };

    private final static UpdateOptions UPSERT_OPTION =
            new UpdateOptions().upsert(true);


    public final static class AsyncUpsertModel<T> {

        private final T id;
        private final UpdateOneModel<Document> update;
        private final SingleResultCallback<Boolean> callback;

        private AsyncUpsertModel(T id,
                                 UpdateOneModel<Document> update,
                                 SingleResultCallback<Boolean> callback) {
            this.id = id;
            this.update = update;
            this.callback = callback;
        }

        public UpdateOneModel<Document> getUpdate() {
            return update;
        }

        public SingleResultCallback<Boolean> getCallback() {
            return callback;
        }
    }


    public final static class SetValue implements Bson {

        public final static SetValue setValue(String name , Object value)
        {
            return new SetValue(name, value);
        }

        private final Bson bson;

        public SetValue(String name , Object value)
        {
            bson = Updates.setOnInsert(name ,value);
        }

        @Override
        public <TDocument> BsonDocument toBsonDocument(Class<TDocument> aClass, CodecRegistry codecRegistry) {
            return bson.toBsonDocument(aClass, codecRegistry);
        }
    }


    private final class BatchCallbackHandler implements SingleResultCallback<BulkWriteResult> {

        private final Map<T, AsyncUpsertModel<T>> index;

        private BatchCallbackHandler(Map<T, AsyncUpsertModel<T>> index) {
            this.index = index;
        }


        @Override
        public void onResult(BulkWriteResult result, Throwable throwable) {

            for (BulkWriteUpsert upsert : result.getUpserts()) {
                T id = bsonValueFetcher.apply(upsert.getId());
                AsyncUpsertModel<T> upsertModel =
                        index.remove(id);

                if (upsertModel == null)
                    continue;

                upsertModel.callback.onResult(true, null);

            }

            if (throwable != null) {
                for (AsyncUpsertModel<T> upsertModel : index.values()) {
                    upsertModel.callback.onResult(null, throwable);
                }
            } else {
                for (AsyncUpsertModel<T> upsertModel : index.values()) {
                    upsertModel.callback.onResult(false, null);
                }
            }

        }
    }


    private final AsyncMongoCollection<Document> collection;
    private Map<T, AsyncUpsertModel<T>> upsertIndex;
    private Map<T, AsyncUpsertModel<T>> capturedIndex;
    private final Function<BsonValue, T> bsonValueFetcher;

    public UpsertBatch(AsyncMongoCollection<Document> collection,
                       Function<BsonValue, T> bsonValueFetcher,
                       ScheduledExecutorService service,
                       int batchSize,
                       long timeOut) {

        super(service, batchSize, timeOut);
        this.collection = collection;

        this.bsonValueFetcher = bsonValueFetcher;
    }

    @Override
    public void constructBatch(int size) {
        capturedIndex = upsertIndex;
        upsertIndex = new HashMap<>(size);
        super.constructBatch(size);
    }

    @Override
    public BatchExecutor<AsyncUpsertModel<T>> putInBatch(AsyncUpsertModel<T> tAsyncUpsertModel) {
        throw new IllegalStateException("can not use this method directly");
    }

    public BatchExecutor<AsyncUpsertModel<T>> upsertOne(T id , SingleResultCallback<Boolean> result , SetValue ... values){
        return super.putInBatch(
                new AsyncUpsertModel<T>(
                        id ,
                        new UpdateOneModel<Document>(
                                Filters.eq("_id" , id) ,
                                Updates.combine(values) ,
                                UPSERT_OPTION

                        ) ,
                        result
                )
        );
    }

    @Override
    public boolean addToBatch(AsyncUpsertModel<T> model, List<AsyncUpsertModel<T>> batch) {

        if (upsertIndex.get(model) != null) {
            model.callback.onResult(false, null);
            return true;
        }

        if (super.addToBatch(model, batch)) {
            upsertIndex.put(model.id, model);
            return true;
        }

        return false;
    }


    @Override
    public void executeBatch(List<AsyncUpsertModel<T>> batch) {
        List<UpdateOneModel<Document>> updates = ListMapper.map(batch, AsyncUpsertModel::getUpdate);
        collection.bulkWrite(updates, new BatchCallbackHandler(capturedIndex));
    }
}
