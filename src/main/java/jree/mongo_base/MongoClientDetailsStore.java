package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.api.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.*;

public class MongoClientDetailsStore {


    private final static class OffsetFetcher implements Block<Document> , SingleResultCallback<Void>
    {
        private List<ConversationOffset> offsets = new ArrayList<>();
        private final Session session;
        private final SingleResultCallback<List<ConversationOffset>> resultCallback;

        private OffsetFetcher(Session session, SingleResultCallback<List<ConversationOffset>> resultCallback) {
            this.session = session;
            this.resultCallback = resultCallback;
        }

        @Override
        public void apply(Document document) {
            String offsetKey = String.valueOf(session.id());
            long offset = document.containsKey(offsetKey)
                    ?document.getLong(offsetKey):
                    document.getLong("MAX")-20;

            offset = offset<0?0:offset;

            offsets.add(
                    new ConversationOffset(
                            document.getString("conversation") ,
                            offset
                    )
            );
        }

        @Override
        public void onResult(Void aVoid, Throwable throwable) {
            if(throwable!=null)
            {
                resultCallback.onResult(null , throwable);
            }else
            {
                resultCallback.onResult(offsets , null);
            }
        }
    }



    private final static String CLIENT_DETAILS_STORE_COLLECTION_NAME =
            "DETAILS";

    private final static String CLIENT_OFFSET_STORE_COLLECTION_NAME =
            "OFFSETS";




    private final AsyncMongoDatabase database;
    private final MongoMessageStore messageStore;
    private final AsyncMongoCollection<Document> clientsDetailsStoreCollection;
    private final AsyncMongoCollection<Document> clientOffsetStoreCollection;

    public MongoClientDetailsStore(AsyncMongoDatabase database, MongoMessageStore messageStore) {
        this.database = database;
        this.messageStore = messageStore;
        this.clientsDetailsStoreCollection =
                this.database.getCollection(CLIENT_DETAILS_STORE_COLLECTION_NAME);
        this.clientOffsetStoreCollection =
                this.database.getCollection(CLIENT_OFFSET_STORE_COLLECTION_NAME);

        DBStaticFunctions.createIndex(
                clientsDetailsStoreCollection ,
                Indexes.ascending("client" ,
                        "sessions")
        );

        DBStaticFunctions.createIndex(
                clientOffsetStoreCollection ,
                Indexes.ascending(
                        "client" , "session" , "conversation"
                )
        );
    }

    private final static Bson updateTags(InsertTag[] tags , String[] names)
    {
        Document document = new Document();

        int count = 0;
        for(InsertTag tag:tags)
        {
            document.append(tag.name() , tag.idIndex());
            names[count++] = tag.name();
        }

        return new Document().append("$max" , document);
    }

    public void setTag(Session session, Recipient recipient , InsertTag insertTag,
                       SingleResultCallback<InsertTagResult> callback)
    {


        final FindOneAndUpdateOptions deliveryOption = new FindOneAndUpdateOptions()
                .upsert(true)
                .projection(include(insertTag.name()));

        String conversation = StaticFunctions.uniqueConversationId(session , recipient);


        clientOffsetStoreCollection.findOneAndUpdate(
                and(eq("client", session.clientId()) ,
                        eq("conversation" , conversation)),
                max(insertTag.name() , insertTag.idIndex()) ,
                deliveryOption,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        try {

                            if (throwable != null) {
                                callback.onResult(null, throwable);
                            } else {
                                long oldCount = document == null ?
                                        0 :
                                        (long) document.getOrDefault(insertTag.name(), 0l);

                                final Tag tag = new TagImpl(
                                        insertTag.name() ,
                                        Instant.now() ,
                                        session.clientId()
                                );

                                if(insertTag.wantAttachToMessages())
                                {

                                    messageStore.setTagOnMessages(
                                            session ,
                                            recipient ,
                                            tag,
                                            oldCount,
                                            insertTag.idIndex(),
                                            new SingleResultCallback<UpdateResult>() {
                                                @Override
                                                public void onResult(UpdateResult updateResult, Throwable throwable) {

                                                    if(throwable!=null)
                                                    {
                                                        callback.onResult(null , throwable);
                                                    }else
                                                    {
                                                        callback.onResult(new InsertTagResultImpl(
                                                                updateResult.getModifiedCount() , tag
                                                        ) , null);
                                                    }
                                                }
                                            }
                                    );
                                }else
                                {
                                    callback.onResult(new InsertTagResultImpl(
                                            0 , tag
                                    ) , null);
                                }
                            }
                        }catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }


    public void addClient(long client , SingleResultCallback<UpdateResult> result)
    {
        clientsDetailsStoreCollection
                .updateOne(eq("client" , client) ,
                        set("client" , client) ,
                        new UpdateOptions().upsert(true) ,
                        result);
    }

    public void addSessionToClient(long client ,
                                   SingleResultCallback<Long> callback)
    {
        FindOneAndUpdateOptions oneAndUpdateOptions = new FindOneAndUpdateOptions();
        oneAndUpdateOptions.upsert(false);
        oneAndUpdateOptions.projection(include("sessionCount"));
        clientsDetailsStoreCollection
                .findOneAndUpdate(eq("client", client),
                        inc("sessionCount", 1l),
                        oneAndUpdateOptions,
                        new SingleResultCallback<Document>() {
                            @Override
                            public void onResult(Document document, Throwable throwable) {

                                if(throwable!=null)
                                {
                                    callback.onResult(null , throwable);
                                    return;
                                }

                                final long session = ((long) document.getOrDefault(
                                        "sessionCount" , 0l
                                ))+1;

                                clientsDetailsStoreCollection
                                        .updateOne(
                                                eq("client", client),
                                                addToSet("sessions", session),
                                                new SingleResultCallback<UpdateResult>() {
                                                    @Override
                                                    public void onResult(UpdateResult updateResult, Throwable throwable) {

                                                        if(throwable!=null)
                                                        {
                                                            callback.onResult(null , throwable);
                                                        }else {
                                                            callback.onResult(session,  null);
                                                        }
                                                    }
                                                }
                                        );
                            }
                        }
                );


    }

    public void isSessionExists(long client , long session ,
                                SingleResultCallback<Boolean> callback)
    {
        clientsDetailsStoreCollection
                .find(
                        and(eq("client" , client) ,
                                eq("sessions" , session))
                ).first(new SingleResultCallback<Document>() {
            @Override
            public void onResult(Document document, Throwable throwable) {
                if(throwable!=null)
                {
                    callback.onResult(null , throwable);
                }else
                {
                    callback.onResult(document!=null , null);
                }
            }
        });
    }

    public void getConversationOffsets(Session session , SingleResultCallback<List<ConversationOffset>> callback)
    {
        OffsetFetcher offsetFetcher = new OffsetFetcher(session, callback);
        clientOffsetStoreCollection.find(
                and(eq("client" , session.clientId()) ,
                        or(eq("session" ,session.id()) ,
                                exists("session" , false)))
        ).projection(include(String.valueOf(session.id()) ,
                "MAX" , "conversation")).forEach(offsetFetcher , offsetFetcher);
    }

    public void setMessageOffset(Session session , Recipient recipient , long offset , SingleResultCallback<UpdateResult> callback)
    {
        clientOffsetStoreCollection.updateOne(
                and(eq("client" , session.clientId()),
                        eq("conversation" , StaticFunctions.uniqueConversationId(session, recipient))) ,
                combine(max(String.valueOf(session.id()) , offset) ,
                        max("MAX", offset)),
                callback
        );
    }


    public void subscribe(Session session , String conversation , InsertTag tag ,
                          SingleResultCallback<ConversationOffset> result)
    {

    }

    @Deprecated
    void storeMessageZeroOffset(PubMessage message)
    {
        if (message.id() == 1 && message.type().isNot(PubMessage.Type.CLIENT_TO_CONVERSATION)) {

            String conversation = StaticFunctions.relatedConversation(message);
            List<Document> list = null;

            if(message.type().is(PubMessage.Type.CLIENT_TO_CLIENT)) {
                list = Arrays.asList(
                        new Document().append("client", message.publisher().client())
                                .append("conversation", conversation)
                                .append("MAX", 0l),
                        new Document().append("client", message.recipient().client())
                                .append("conversation", conversation)
                                .append("MAX", 0l)
                );
            }else
            {
                //SESSION_TO_SESSION :)))
                list = Arrays.asList(
                        new Document().append("client", message.publisher().client())
                                .append("session" , message.publisher().session())
                                .append("conversation", conversation)
                                .append("MAX", 0l),
                        new Document().append("client", message.recipient().client())
                                .append("session" , message.recipient().session())
                                .append("conversation", conversation)
                                .append("MAX", 0l)
                );
            }

            clientOffsetStoreCollection.insertMany(list, new SingleResultCallback<InsertManyResult>() {
                @Override
                public void onResult(InsertManyResult insertManyResult, Throwable throwable) {

                }
            });
        }

    }

    void storeMessageZeroOffset(Session publisher ,
                                Recipient recipient ,
                                PubMessage.Type type ,
                                SingleResultCallback<InsertManyResult> callback) {

        if (type.isNot(PubMessage.Type.CLIENT_TO_CONVERSATION)) {

            String conversation = StaticFunctions.relatedConversation(publisher, recipient, type);
            List<Document> list = null;

            if (type.is(PubMessage.Type.CLIENT_TO_CLIENT)) {
                list = Arrays.asList(
                        new Document().append("client", publisher.clientId())
                                .append("conversation", conversation)
                                .append("MAX", 0l),
                        new Document().append("client", recipient.client())
                                .append("conversation", conversation)
                                .append("MAX", 0l)
                );
            } else {
                //SESSION_TO_SESSION :)))
                list = Arrays.asList(
                        new Document().append("client", publisher.clientId())
                                .append("session", publisher.id())
                                .append("conversation", conversation)
                                .append("MAX", 0l),
                        new Document().append("client", recipient.client())
                                .append("session", recipient.session())
                                .append("conversation", conversation)
                                .append("MAX", 0l)
                );
            }

            clientOffsetStoreCollection.insertMany(list, callback);
        }
    }

}
