package jree.mongo_base;

import com.mongodb.internal.async.client.AsyncMongoClient;
import com.mongodb.internal.async.client.AsyncMongoClients;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.api.BodySerializer;
import jree.api.MessageManager;
import jree.api.PubSubSystem;
import jree.api.SessionManager;
import jree.util.Assertion;

public class MongoPubSubSystem<T> implements PubSubSystem<T , String> {

    private final SerializerReference<T> serializer;
    private final MongoMessageManager<T> messageManager;
    private final MongoSessionManager<T> sessionManager;
    private final ClientsHolder holder;
    private final ConversationSubscribersHolder<T> subscribersHolder;
    private final AsyncMongoDatabase database;
    private final MongoClientDetailsStore detailsStore;
    private final MongoMessageStore messageStore;
    private final String id;

    public MongoPubSubSystem(String id , BodySerializer<T> serializer) {
        Assertion.ifNullString("id can not be empty or null" , id);
        this.id = id;
        AsyncMongoClient client = AsyncMongoClients.create();
        database = client.getDatabase("MSG_"+id);
        this.serializer = new SerializerReference<>(serializer);
        this.holder = new ClientsHolder();
        messageStore = new MongoMessageStore(database);
        detailsStore = new MongoClientDetailsStore(database , messageStore);
        messageStore.setDetailsStore(detailsStore);


        subscribersHolder = new ConversationSubscribersHolder<>(holder ,"jdbc:h2:E:\\h2db\\db");


        messageManager = new MongoMessageManager<>(
                holder ,
                detailsStore ,
                messageStore ,
                this.serializer
        );

        sessionManager = new MongoSessionManager<>(
                messageStore ,
                detailsStore ,
                holder ,
                subscribersHolder ,
                serializer
        );


    }


    @Override
    public void setBodySerializer(BodySerializer<T> serializer) {
        this.serializer.setReference(serializer);
    }

    @Override
    public MessageManager<T , String> messageManager() {
        return messageManager;
    }

    @Override
    public SessionManager sessionManager() {
        return sessionManager;
    }
}
