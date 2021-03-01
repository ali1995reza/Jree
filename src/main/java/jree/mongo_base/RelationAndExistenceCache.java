package jree.mongo_base;

import jree.api.OperationResultListener;
import jree.api.Relation;

public class RelationAndExistenceCache {


    private final MongoClientDetailsStore detailsStore;

    public RelationAndExistenceCache(MongoClientDetailsStore detailsStore) {
        this.detailsStore = detailsStore;
    }


    public void getRelationFor(String conversationId , OperationResultListener<Relation> result)
    {

    }
}
