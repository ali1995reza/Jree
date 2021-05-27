package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.abs.SessionDetails;
import jree.abs.codes.FailReasonsCodes;
import jree.abs.parts.DetailsStore;
import jree.abs.utils.StaticFunctions;
import jree.api.*;
import jree.mongo_base.batch.BatchContext;
import jree.mongo_base.batch.UpsertBatch;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static jree.mongo_base.SingleResultCallbackToOperationResultCallback.alwaysTrueCallback;
import static jree.mongo_base.SingleResultCallbackToOperationResultCallback.wrapCallBack;
import static jree.mongo_base.CollectionNames.*;

public class MongoDetailsStore<ID extends Comparable<ID>> implements DetailsStore<ID> {

    //private final AsyncMongoDatabase database;
    private final CollectionInfo clientsDetailsCollection;
    private final CollectionInfo sessionsDetailsCollection;
    private final CollectionInfo relationCollection;
    private final CollectionInfo subscribesCollection;
    //private final BatchContext batchContext;

    public MongoDetailsStore(AsyncMongoDatabase database, BatchContext batchContext) {
        //this.database = database;
        //this.batchContext = batchContext;

        this.clientsDetailsCollection = new CollectionInfo(CLIENT_DETAILS_COLLECTION_NAME, database);
        this.relationCollection = new CollectionInfo(RELATION_DETAILS_COLLECTION_NAME, database);
        this.sessionsDetailsCollection = new CollectionInfo(SESSIONS_DETAILS_COLLECTION_NAME, database);
        this.subscribesCollection = new CollectionInfo(SUBSCRIBES_COLLECTION_NAME, database);

        batchContext.createNewUpdateBatch(clientsDetailsCollection, 2000, 100);
        batchContext.createNewUpsertBatch(clientsDetailsCollection, UpsertBatch.LONG_FETCHER, 2000, 100);
        batchContext.createNewFindBatch(clientsDetailsCollection, 1000, 100);
        batchContext.createNewFindBatch(sessionsDetailsCollection, 1000, 100);
        batchContext.createNewUpsertBatch(sessionsDetailsCollection, UpsertBatch.STRING_FETCHER, 1000, 100);
        batchContext.createNewUpdateBatch(sessionsDetailsCollection, 1000, 100);
        batchContext.createNewUpdateBatch(relationCollection ,2000, 100);
        batchContext.createNewFindBatch(relationCollection, 1000, 50);
        batchContext.createNewUpsertBatch(subscribesCollection, UpsertBatch.STRING_FETCHER, 1000, 100);


        DBStaticFunctions.createIndex(clientsDetailsCollection.collection(),
                Indexes.ascending("client", "sessions"));
        DBStaticFunctions.createIndex(sessionsDetailsCollection.collection(),
                Indexes.ascending("client", "session"));
    }

    private void doAddClient(long client, OperationResultListener<Boolean> callback) {
        clientsDetailsCollection.upsertBatch().upsertOne(client,
                wrapCallBack(callback),
                UpsertBatch.SetValue.setValue("joinTime", Instant.now()));
    }

    private void doRemoveClient(long client, OperationResultListener<Boolean> callback) {
        clientsDetailsCollection.updateBatch().updateOne(eq("_id", client),
                set("removed", true), new SingleResultCallback<Void>() {
                    @Override
                    public void onResult(Void unused, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            sessionsDetailsCollection.updateBatch()
                                    .deleteMany(eq("client", client),
                                            alwaysTrueCallback(callback));
                        }
                    }
                });
    }

    public void doAddSessionToClient(long client, OperationResultListener<Long> callback) {
        clientsDetailsCollection.findByIdBatch().findOne(client,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            long sessionId = StaticFunctions.newLongId();
                            sessionsDetailsCollection.upsertBatch().upsertOne(
                                    String.valueOf(client).concat("_").concat(
                                            String.valueOf(sessionId)),
                                    new SingleResultCallback<Boolean>() {
                                        @Override
                                        public void onResult(Boolean aBoolean, Throwable throwable) {
                                            if (throwable != null) {
                                                callback.onFailed(
                                                        new FailReason(
                                                                throwable,
                                                                FailReasonsCodes.RUNTIME_EXCEPTION));
                                            } else {
                                                if (aBoolean) {
                                                    callback.onSuccess(
                                                            sessionId);
                                                } else {
                                                    callback.onFailed(
                                                            new FailReason(
                                                                    new IllegalStateException(
                                                                            "fix it after"),
                                                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                                                }
                                            }
                                        }
                                    }, UpsertBatch.SetValue.setValue("client",
                                            client),
                                    UpsertBatch.SetValue.setValue("session",
                                            sessionId),
                                    UpsertBatch.SetValue.setValue("offset",
                                            "0"));
                        }
                    }
                });
    }

    private final static String uniqueId(Session session) {
        return String.valueOf(session.clientId()).concat("_").concat(
                String.valueOf(session.id()));
    }

    private static String uniqueId(long client, long session) {
        return String.valueOf(client).concat("_").concat(
                String.valueOf(session));
    }

    public void getRelation(String relationId, OperationResultListener<Relation> result) {
        relationCollection.findByIdBatch().findOne(relationId,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            result.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                            return;
                        }
                        if (document == null ||  document.size()==1 /*just contains id !*/) {
                            result.onSuccess(Relation.EMPTY);
                            return;
                        }

                        final Map<String, String> attars = new HashMap<>();

                        for (Map.Entry<String, Object> entry : document.entrySet()) {
                            if(entry.getKey().equals("_id"))
                                continue;
                            attars.put(entry.getKey(), (String) entry.getValue());
                        }
                        result.onSuccess(new MongoRelation(attars));
                    }
                });
    }

    @Override
    public void addClient(long client, OperationResultListener<Boolean> result) {
        doAddClient(client, result);
    }

    @Override
    public void removeClient(long client, OperationResultListener<Boolean> callback) {
        doRemoveClient(client, callback);
    }

    @Override
    public void addSessionToClient(long client, OperationResultListener<Long> callback) {
        doAddSessionToClient(client, callback);
    }

    @Override
    public void removeSession(long client, long sessionId, OperationResultListener<Boolean> callback) {
        sessionsDetailsCollection.updateBatch()
                .deleteMany(and(eq("client", client), eq("session", sessionId)),
                        alwaysTrueCallback(callback));
    }

    @Override
    public void isSessionExists(long client, long session, OperationResultListener<Boolean> callback) {
        sessionsDetailsCollection.findByIdBatch().findOne(
                String.valueOf(client).concat("_").concat(
                        String.valueOf(session)),
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            callback.onSuccess(document != null);
                        }
                    }
                });
    }

    @Override
    public void isClientExists(long client, OperationResultListener<Boolean> callback) {
        clientsDetailsCollection.findByIdBatch().findOne(client,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            if (document == null) {
                                callback.onSuccess(false);
                            } else {
                                boolean exists = !document.getBoolean("removed",
                                        false);
                                callback.onSuccess(exists);
                            }
                        }
                    }
                });
    }

    @Override
    public void getSessionOffset(long client, long session, OperationResultListener<ID> callback) {
        sessionsDetailsCollection.findByIdBatch().findOne(uniqueId(client, session),
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else if (document == null) {
                            callback.onFailed(new FailReason(
                                    FailReasonsCodes.SESSION_NOT_EXISTS));
                        } else {
                            callback.onSuccess((ID) document.get("offset"));
                        }
                    }
                });
    }

    @Override
    public void getSessionDetails(long client, long session, OperationResultListener<SessionDetails<ID>> callback) {
        final SessionDetails<ID> details = new SessionDetails<>();
        sessionsDetailsCollection.findByIdBatch().findOne(uniqueId(client, session),
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else if (document == null) {
                            callback.onSuccess(details.setSessionExists(false));
                        } else {
                            details.setSessionExists(true).setOffset(
                                    (ID) document.get("offset"));
                            List<Long> conversations = new ArrayList<>();
                            subscribesCollection.collection().find(
                                    eq("client", client)).projection(
                                    Projections.include(
                                            "conversation")).forEach(
                                    new Block<Document>() {
                                        @Override
                                        public void apply(Document document) {
                                            conversations.add(document.getLong(
                                                    "conversation"));
                                        }
                                    }, new SingleResultCallback<Void>() {
                                        @Override
                                        public void onResult(Void unused, Throwable throwable) {
                                            if (throwable != null) {
                                                callback.onFailed(
                                                        new FailReason(
                                                                throwable,
                                                                FailReasonsCodes.RUNTIME_EXCEPTION));
                                            } else {
                                                callback.onSuccess(
                                                        details.setSubscribeList(
                                                                conversations));
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    @Override
    public void setSessionOffset(long client, long session, ID offset, OperationResultListener<Boolean> callback) {
        sessionsDetailsCollection.updateBatch().updateOne(
                and(eq("client", client), eq("session", session)),
                set("offset", offset),
                alwaysTrueCallback(callback));
    }

    @Override
    public void addRelation(Session session, Recipient recipient, String key, String value, OperationResultListener<Boolean> callback) {
        String id = null;
        if (recipient.conversation() < 0) {
            id = StaticFunctions.uniqueConversationId(session, recipient);
        } else {
            id = "C_" + session.clientId() + "_" + recipient.conversation();
        }
        relationCollection.updateBatch().updateOne(eq("_id", id),
                set(key, value),
                new UpdateOptions().upsert(true), alwaysTrueCallback(callback));
    }

    @Override
    public void getRelation(Session session, Recipient recipient, OperationResultListener<Relation> callback) {
        if (recipient.conversation() < 0) {
            getRelation(
                    StaticFunctions.uniqueConversationId(session, recipient),
                    callback);
        } else {
            String relationId = "C_" + session.clientId() + "_" + recipient.conversation();
            getRelation(relationId, callback);
        }
    }

    @Override
    public void addToSubscribeList(long client, long conversation, OperationResultListener<Boolean> callback) {
        subscribesCollection.upsertBatch().upsertOne(
                String.valueOf(client).concat("_").concat(
                        String.valueOf(conversation)),
                new SingleResultCallback<Boolean>() {
                    @Override
                    public void onResult(Boolean aBoolean, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            callback.onSuccess(aBoolean);
                        }
                    }
                }, UpsertBatch.SetValue.setValue("client", client),
                UpsertBatch.SetValue.setValue("conversation", conversation));
    }

    @Override
    public void removeFromSubscribeList(long client, long conversation, OperationResultListener<Boolean> callback) {
        subscribesCollection.collection().deleteOne(eq("_id",
                String.valueOf(client).concat("_").concat(
                        String.valueOf(conversation))),
                new SingleResultCallback<DeleteResult>() {
                    @Override
                    public void onResult(DeleteResult deleteResult, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            callback.onSuccess(
                                    deleteResult.getDeletedCount() > 0);
                        }
                    }
                });
    }

    @Override
    public void close() {
    }

}
