package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.client.model.*;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.api.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.*;

public class MongoClientDetailsStore {






    private final static String CLIENT_DETAILS_STORE_COLLECTION_NAME =
            "DETAILS";

    private final static String CLIENT_OFFSET_STORE_COLLECTION_NAME =
            "OFFSETS";

    private final static String RELATION_DETAILS_STORE_COLLECTION_NAME =
            "RELATION";




    private final AsyncMongoDatabase database;
    private final MongoMessageStore messageStore;
    private final AsyncMongoCollection<Document> clientsDetailsStoreCollection;
    private final AsyncMongoCollection<Document> clientOffsetStoreCollection;
    private final AsyncMongoCollection<Document> relationStoreCollection;
    private final BatchContext batchContext;

    public MongoClientDetailsStore(AsyncMongoDatabase database, MongoMessageStore messageStore, BatchContext batchContext) {
        this.database = database;
        this.messageStore = messageStore;
        this.batchContext = batchContext;
        this.clientsDetailsStoreCollection =
                this.database.getCollection(CLIENT_DETAILS_STORE_COLLECTION_NAME);
        this.clientOffsetStoreCollection =
                this.database.getCollection(CLIENT_OFFSET_STORE_COLLECTION_NAME);

        this.relationStoreCollection =
                this.database.getCollection(RELATION_DETAILS_STORE_COLLECTION_NAME);

        batchContext.createNewUpdateBatch("offset" , clientOffsetStoreCollection , 2000, 100);
        batchContext.createNewUpdateBatch("details" , clientsDetailsStoreCollection , 2000 , 100);
        batchContext.createNewUpdateBatch("relation" , relationStoreCollection , 2000 , 100);
        batchContext.createNewFindBatch("relation" , relationStoreCollection , 1000 ,50);

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





    public void addClient(long client , SingleResultCallback<UpdateResult> result)
    {
        batchContext.getUpdateBatch("details")
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

    public void isClientExists(long client ,
                               SingleResultCallback<Boolean> callback)
    {
        clientsDetailsStoreCollection
                .find(eq("client" , client))
                .first(new SingleResultCallback<Document>() {
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

    public void getSessionOffset(Session session , SingleResultCallback<String> callback)
    {
        clientOffsetStoreCollection.find(
                and(eq("client" , session.clientId()) ,
                        eq("session" ,session.id()))
        ).first(
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if(throwable!=null)
                        {
                            callback.onResult(null , new FailReason(MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                        }else if(document==null)
                        {
                            callback.onResult("0" , null);
                        }else {
                            callback.onResult(
                                    (String) document.getOrDefault("offset" , "0") ,
                                    null
                            );
                        }
                    }
                }
        );
    }

    public void setMessageOffset(Session session , String offset , OperationResultListener<Boolean> callback)
    {
        batchContext.getUpdateBatch("offset").updateOne(
                and(eq("client" , session.clientId()),
                        eq("session" , session.id())),
                set("offset" , offset) ,
                new UpdateOptions().upsert(true) ,
                new AttachableConditionSingleResultCallback<Void, OperationResultListener<Boolean>>()
                .attach(callback)
                .ifSuccess(new BiConsumer<Void, OperationResultListener<Boolean>>() {
                    @Override
                    public void accept(Void aVoid, OperationResultListener<Boolean> booleanOperationResultListener) {
                        callback.onSuccess(true);
                    }
                })
                .ifFail(FailCaller.RUNTIME_FAIL_CALLER)
        );
    }


    @Deprecated
    void storeMessageZeroOffset(PubMessage message)
    {
        if (true && message.type().isNot(PubMessage.Type.CLIENT_TO_CONVERSATION)) {

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


    @Deprecated
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



    private final static UpdateOptions WITH_UPSERT = new UpdateOptions()
            .upsert(true);


    @Deprecated
    void storeMessageOffset(Session session ,
                            boolean justThisSession ,
                            String conversation ,
                            long messageIndex ,
                            SingleResultCallback<UpdateResult> callback){

        Bson filter = and(eq("client" , session.clientId()) ,
                eq("conversation" , conversation));

        Bson update = setOnInsert("MAX" , messageIndex);

        if(justThisSession)
        {
            update = combine(update , setOnInsert("session" , session.id()));
        }else {
            update = combine(update , unset("session"));
        }


        clientOffsetStoreCollection.updateOne(filter , update , WITH_UPSERT , callback);

    }


    public void addRelation(Session session , Recipient recipient , String key , String value , SingleResultCallback<Void> callback)
    {
        //so handle them please !

        String id = StaticFunctions.uniqueConversationId(session, recipient);

        batchContext.getUpdateBatch("relation")
                .updateOne(
                        eq("_id" , id),
                        set("CL_".concat(String.valueOf(session.clientId())).concat(".").concat(key) ,
                                value) ,
                        new UpdateOptions().upsert(true) ,
                        callback
                );
    }

    public void getRelation(String conversationId , OperationResultListener<Relation> result) {
        batchContext.getFindBatch("relation")
                .findOne(conversationId, new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            result.onFailed(new FailReason(throwable, MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                            return;
                        }

                        if (document == null) {
                            result.onSuccess(Relation.EMPTY);
                            return;
                        }

                        MongoRelation.Holder a = null;
                        MongoRelation.Holder b = null;
                        for (String key : document.keySet()) {
                            if (key.startsWith("CL")) {
                                long id = Long.parseLong(key.split("_")[1]);
                                Map<String, String> map = (Map<String, String>) document.get(key);
                                a = MongoRelation.Holder.clientHolder(id, map);
                            } else if (key.startsWith("C")) {
                                long id = Long.parseLong(key.split("_")[1]);
                                Map<String, String> map = (Map<String, String>) document.get(key);
                                b = MongoRelation.Holder.conversationHolder(id, map);
                            }
                        }

                        result.onSuccess(new MongoRelation(a, b));
                    }
                });
    }
}
