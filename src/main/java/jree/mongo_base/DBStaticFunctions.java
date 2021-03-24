package jree.mongo_base;

import com.mongodb.internal.async.client.AsyncMongoCollection;
import org.bson.conversions.Bson;

public class DBStaticFunctions {


    public final static String createIndex(AsyncMongoCollection collection ,
                                           Bson index){
        SyncSingleResultCallback<String> sync = new SyncSingleResultCallback<>();
        collection.createIndex(index , sync);
        return sync.getResult();
    }
}
