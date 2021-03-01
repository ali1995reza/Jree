package jree.mongo_base;

import jree.api.OperationResultListener;
import jree.api.Relation;

public class RelationCache {


    private final MongoClientDetailsStore detailsStore;

    public RelationCache(MongoClientDetailsStore detailsStore) {
        this.detailsStore = detailsStore;
    }


    public void getRelationFor(String conversationId , OperationResultListener<Relation> result)
    {

    }
}
