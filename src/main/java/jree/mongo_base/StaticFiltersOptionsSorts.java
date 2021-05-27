package jree.mongo_base;

import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

final class StaticFiltersOptionsSorts {

    final static UpdateOptions UPDATE_OPTIONS_WITH_UPSERT = new UpdateOptions().upsert(true);

    final static Bson ID_SORT = new Document("_id" , 1);

    final static Bson ID_SORT_REVERSE = new Document("_id", -1);

}
