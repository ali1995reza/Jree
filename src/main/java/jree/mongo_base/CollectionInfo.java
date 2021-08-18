package jree.mongo_base;

import com.mongodb.internal.async.client.AsyncMongoCollection;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.mongo_base.batch.FindByIdBatch;
import jree.mongo_base.batch.InsertBatch;
import jree.mongo_base.batch.UpdateBatch;
import jree.mongo_base.batch.UpsertBatch;
import jutils.assertion.Assertion;
import org.bson.Document;

public final class CollectionInfo {

    private final AsyncMongoCollection<Document> collection;
    private FindByIdBatch findByIdBatch;
    private InsertBatch insertBatch;
    private UpdateBatch updateBatch;
    private UpsertBatch upsertBatch;

    CollectionInfo(String name, AsyncMongoDatabase database) {
        this.collection = database.getCollection(name);
    }

    private String collectionName() {
        return collection.getNamespace().getCollectionName();
    }

    public AsyncMongoCollection<Document> collection() {
        return collection;
    }

    public <T> FindByIdBatch<T> findByIdBatch(Class<T> idType) {
        return findByIdBatch;
    }

    public <T> FindByIdBatch<T> findByIdBatch() {
        return findByIdBatch;
    }

    public void setFindByIdBatch(FindByIdBatch findByIdBatch) {
        Assertion.ifNotNull("already a find batch exists for collection "+collectionName(),
                this.findByIdBatch);
        this.findByIdBatch = findByIdBatch;
    }

    public InsertBatch insertBatch() {
        return insertBatch;
    }

    public void setInsertBatch(InsertBatch insertBatch) {
        Assertion.ifNotNull("already a insert batch exists for collection "+collectionName(),
                this.insertBatch);
        this.insertBatch = insertBatch;
    }

    public UpdateBatch updateBatch() {
        return updateBatch;
    }

    public void setUpdateBatch(UpdateBatch updateBatch) {
        Assertion.ifNotNull("already a update batch exists for collection "+collectionName(),
                this.updateBatch);
        this.updateBatch = updateBatch;
    }

    public <T> UpsertBatch<T> upsertBatch(Class<T> idType) {
        return upsertBatch;
    }

    public <T> UpsertBatch<T> upsertBatch() {
        return upsertBatch;
    }

    public void setUpsertBatch(UpsertBatch upsertBatch) {
        Assertion.ifNotNull("already a upsert batch exists for collection "+collectionName(),
                this.upsertBatch);
        this.upsertBatch = upsertBatch;
    }

}
