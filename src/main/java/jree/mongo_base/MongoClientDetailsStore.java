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
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;

import java.time.Instant;
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

    private final static String RELATION_DETAILS_STORE_COLLECTION_NAME =
            "RELATIONS";

    private final static String SESSIONS_DETAILS_STORE_COLLECTION_NAME =
            "SESSIONS";




    private final AsyncMongoDatabase database;
    private final MongoMessageStore messageStore;
    private final AsyncMongoCollection<Document> clientsDetailsStoreCollection;
    private final AsyncMongoCollection<Document> sessionsDetailsStoreCollection;
    private final AsyncMongoCollection<Document> relationStoreCollection;
    private final BatchContext batchContext;

    public MongoClientDetailsStore(AsyncMongoDatabase database, MongoMessageStore messageStore, BatchContext batchContext) {
        this.database = database;
        this.messageStore = messageStore;
        this.batchContext = batchContext;
        this.clientsDetailsStoreCollection =
                this.database.getCollection(CLIENT_DETAILS_STORE_COLLECTION_NAME);

        this.relationStoreCollection =
                this.database.getCollection(RELATION_DETAILS_STORE_COLLECTION_NAME);

        this.sessionsDetailsStoreCollection =
                this.database.getCollection(SESSIONS_DETAILS_STORE_COLLECTION_NAME);


        batchContext.createNewUpdateBatch("clients" , clientsDetailsStoreCollection , 2000 , 100);
        batchContext.createNewUpsertBatch("clients" , clientsDetailsStoreCollection , UpsertBatch.LONG_FETCHER , 2000 , 100);
        batchContext.createNewFindBatch("clients" , clientsDetailsStoreCollection ,1000 , 100);
        batchContext.createNewFindBatch("sessions" , sessionsDetailsStoreCollection , 1000 , 100);
        batchContext.createNewUpsertBatch("sessions" , sessionsDetailsStoreCollection , UpsertBatch.STRING_FETCHER , 1000 , 100);
        batchContext.createNewUpdateBatch("relation" , relationStoreCollection , 2000 , 100);
        batchContext.createNewFindBatch("relation" , relationStoreCollection , 1000 ,50);

        DBStaticFunctions.createIndex(
                clientsDetailsStoreCollection ,
                Indexes.ascending("client" ,
                        "sessions")
        );

        DBStaticFunctions.createIndex(
                sessionsDetailsStoreCollection ,
                Indexes.ascending(
                        "client" , "session"
                )
        );
    }





    public void addClient(long client , SingleResultCallback<Boolean> result)
    {
        batchContext.getUpsertBatch("clients")
                .upsertOne(client ,
                        result ,
                        UpsertBatch.SetValue.setValue("joinTime" , Instant.now()));
    }

    public void addSessionToClient(long client ,
                                   SingleResultCallback<Long> callback)
    {
        batchContext.getFindBatch("clients")
                .findOne(client, new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {

                        if(throwable!=null)
                        {
                            callback.onResult(null , throwable);
                        }else {
                            long sessionId = StaticFunctions.newID();
                            batchContext.getUpsertBatch("sessions")
                                    .upsertOne(
                                            String.valueOf(client).concat("_").concat(String.valueOf(sessionId)),
                                            new SingleResultCallback<Boolean>() {
                                                @Override
                                                public void onResult(Boolean aBoolean, Throwable throwable) {
                                                    if(throwable!=null)
                                                    {
                                                        callback.onResult(null , throwable);
                                                    }else {
                                                        if(aBoolean)
                                                        {
                                                            callback.onResult(sessionId , null);
                                                        }else {
                                                            callback.onResult(null , new IllegalStateException("fix it after"));
                                                        }
                                                    }
                                                }
                                            },
                                            UpsertBatch.SetValue.setValue("client", client),
                                            UpsertBatch.SetValue.setValue("session", sessionId),
                                            UpsertBatch.SetValue.setValue("offset" , "0")
                                    );
                        }
                    }
                });

    }

    public void isSessionExists(long client , long session ,
                                SingleResultCallback<Boolean> callback)
    {
        batchContext.getFindBatch("sessions")
                .findOne(String.valueOf(client).concat("_").concat(String.valueOf(session)),
                        new SingleResultCallback<Document>() {
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
        try {
            batchContext.getFindBatch("clients")
                    .findOne(client, new SingleResultCallback<Document>() {
                        @Override
                        public void onResult(Document document, Throwable throwable) {
                            if (throwable != null) {
                                callback.onResult(null, throwable);
                            } else {
                                callback.onResult(document != null, null);
                            }
                        }
                    });
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    private final static String uniqueId(Session session)
    {
        return String.valueOf(session.clientId()).concat("_").concat(String.valueOf(session.id()));
    }

    public void getSessionOffset(Session session , SingleResultCallback<String> callback)
    {
        batchContext.getFindBatch("sessions")
                .findOne(uniqueId(session), new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if(throwable!=null)
                        {
                            callback.onResult(null , new FailReason(MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                        }else if(document==null)
                        {
                            callback.onResult(null , new FailReason(MongoFailReasonsCodes.SESSION_NOT_EXISTS));
                        }else {
                            callback.onResult( document.getString("offset") , null);
                        }
                    }
                });
    }

    public void setMessageOffset(Session session , String offset , OperationResultListener<Boolean> callback)
    {
        batchContext.getUpdateBatch("sessions").updateOne(
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
