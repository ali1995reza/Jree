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
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static jree.mongo_base.SingleResultCallbackToOperationResultCallback.alwaysTrueCallback;
import static jree.mongo_base.SingleResultCallbackToOperationResultCallback.wrapCallBack;

public class MongoDetailsStore<ID extends Comparable<ID>> implements DetailsStore<ID> {

    private final static String CLIENT_DETAILS_STORE_COLLECTION_NAME = "DETAILS";

    private final static String RELATION_DETAILS_STORE_COLLECTION_NAME = "RELATIONS";

    private final static String SESSIONS_DETAILS_STORE_COLLECTION_NAME = "SESSIONS";

    private final static String SUBSCRIBES_STORE_COLLECTION_NAME = "SUBSCRIBES";

    private final AsyncMongoDatabase database;
    private final AsyncMongoCollection<Document> clientsDetailsStoreCollection;
    private final AsyncMongoCollection<Document> sessionsDetailsStoreCollection;
    private final AsyncMongoCollection<Document> relationStoreCollection;
    private final AsyncMongoCollection<Document> subscribesCollection;
    private final BatchContext batchContext;

    public MongoDetailsStore(AsyncMongoDatabase database, BatchContext batchContext) {
        this.database = database;
        this.batchContext = batchContext;
        this.clientsDetailsStoreCollection = this.database.getCollection(
                CLIENT_DETAILS_STORE_COLLECTION_NAME);
        this.relationStoreCollection = this.database.getCollection(
                RELATION_DETAILS_STORE_COLLECTION_NAME);
        this.sessionsDetailsStoreCollection = this.database.getCollection(
                SESSIONS_DETAILS_STORE_COLLECTION_NAME);
        this.subscribesCollection = this.database.getCollection(
                SUBSCRIBES_STORE_COLLECTION_NAME);
        this.batchContext.createNewUpdateBatch("clients",
                clientsDetailsStoreCollection, 2000, 100);
        this.batchContext.createNewUpsertBatch("clients",
                clientsDetailsStoreCollection, UpsertBatch.LONG_FETCHER, 2000,
                100);
        this.batchContext.createNewFindBatch("clients",
                clientsDetailsStoreCollection, 1000, 100);
        this.batchContext.createNewFindBatch("sessions",
                sessionsDetailsStoreCollection, 1000, 100);
        this.batchContext.createNewUpsertBatch("sessions",
                sessionsDetailsStoreCollection, UpsertBatch.STRING_FETCHER,
                1000, 100);
        this.batchContext.createNewUpdateBatch("sessions",
                sessionsDetailsStoreCollection, 1000, 100);
        this.batchContext.createNewUpdateBatch("relation",
                relationStoreCollection, 2000, 100);
        this.batchContext.createNewFindBatch("relation",
                relationStoreCollection, 1000, 50);
        this.batchContext.createNewUpsertBatch("subscribe",
                subscribesCollection, UpsertBatch.STRING_FETCHER, 1000, 100);
        DBStaticFunctions.createIndex(clientsDetailsStoreCollection,
                Indexes.ascending("client", "sessions"));
        DBStaticFunctions.createIndex(sessionsDetailsStoreCollection,
                Indexes.ascending("client", "session"));
    }

    private void doAddClient(long client, OperationResultListener<Boolean> callback) {
        batchContext.getUpsertBatch("clients").upsertOne(client,
                wrapCallBack(callback),
                UpsertBatch.SetValue.setValue("joinTime", Instant.now()));
    }

    private void doRemoveClient(long client, OperationResultListener<Boolean> callback) {
        batchContext.getUpdateBatch("clients").updateOne(eq("_id", client),
                set("removed", true), new SingleResultCallback<Void>() {
                    @Override
                    public void onResult(Void unused, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            batchContext.getUpdateBatch("sessions")
                                    .deleteMany(eq("client", client),
                                            alwaysTrueCallback(callback));
                        }
                    }
                });
    }

    public void doAddSessionToClient(long client, OperationResultListener<Long> callback) {
        batchContext.getFindBatch("clients").findOne(client,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            long sessionId = StaticFunctions.newLongId();
                            batchContext.getUpsertBatch("sessions").upsertOne(
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
        batchContext.getFindBatch("relation").findOne(relationId,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            result.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
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
                                Map<String, String> map = (Map<String, String>) document.get(
                                        key);
                                a = MongoRelation.Holder.clientHolder(id, map);
                            } else if (key.startsWith("C")) {
                                long id = Long.parseLong(key.split("_")[1]);
                                Map<String, String> map = (Map<String, String>) document.get(
                                        key);
                                b = MongoRelation.Holder.conversationHolder(id,
                                        map);
                            }
                        }
                        result.onSuccess(new MongoRelation(a, b));
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
    public void isSessionExists(long client, long session, OperationResultListener<Boolean> callback) {
        batchContext.getFindBatch("sessions").findOne(
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
        batchContext.getFindBatch("clients").findOne(client,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(new FailReason(throwable,
                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            if(document==null) {
                                callback.onSuccess(false);
                            } else {
                                boolean exists = !document.getBoolean("removed" , false);
                                callback.onSuccess(exists);
                            }
                        }
                    }
                });
    }

    @Override
    public void getSessionOffset(long client, long session, OperationResultListener<ID> callback) {
        batchContext.getFindBatch("sessions").findOne(uniqueId(client, session),
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
        batchContext.getFindBatch("sessions").findOne(uniqueId(client, session),
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
                            subscribesCollection.find(
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
        batchContext.getUpdateBatch("sessions").updateOne(
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
        batchContext.getUpdateBatch("relation").updateOne(eq("_id", id),
                set("CL_".concat(String.valueOf(session.clientId())).concat(
                        ".").concat(key), value),
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
        batchContext.getUpsertBatch("subscribe").upsertOne(
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
        subscribesCollection.deleteOne(eq("_id",
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
