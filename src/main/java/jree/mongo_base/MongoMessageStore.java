package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.PushOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import com.mongodb.internal.operation.OrderBy;
import jree.abs.codes.FailReasonsCodes;
import jree.abs.funcs.ForEach;
import jree.abs.objects.PubMessageImpl;
import jree.abs.objects.RecipientImpl;
import jree.abs.objects.TagImpl;
import jree.abs.parts.MessageStore;
import jree.abs.utils.StaticFunctions;
import jree.api.*;
import jree.mongo_base.batch.BatchContext;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class MongoMessageStore<BODY, ID extends Comparable<ID>> implements MessageStore<BODY, ID> {

    private final static UpdateOptions UPDATE_OPTIONS_WITH_UPSERT = new UpdateOptions().upsert(
            true);

    private final static String CONVERSATION_COLLECTION_NAME = "MSG_COUNTER";
    private final static String MESSAGE_COLLECTION_NAME = "MSG";

    private final AsyncMongoDatabase database;
    private final AsyncMongoCollection<Document> messageCollection;
    private final AsyncMongoCollection<Document> conversationCollection;
    private final BatchContext batchContext;

    public MongoMessageStore(AsyncMongoDatabase database, BatchContext batchContext) {
        this.database = database;
        this.batchContext = batchContext;
        messageCollection = this.database.getCollection(
                MESSAGE_COLLECTION_NAME);
        conversationCollection = this.database.getCollection(
                CONVERSATION_COLLECTION_NAME);

        /*DBStaticFunctions.createIndex(
                messageCollection,
                Indexes.ascending(
                        "type",
                        "publisher.client",
                        "publisher.session" ,
                        "_id"
                )
        );

        DBStaticFunctions.createIndex(
                messageCollection,
                Indexes.ascending(
                        "type",
                        "recipient.client",
                        "recipient.session",
                        "_id"
                )
        );

        DBStaticFunctions.createIndex(
                messageCollection,
                Indexes.ascending(
                        "type",
                        "recipient.conversation",
                        "_id"
                )
        );*/

        DBStaticFunctions.createIndex(messageCollection ,
                Indexes.ascending(
                        "recipientIndex" ,
                        "_id"
                ));

        DBStaticFunctions.createIndex(messageCollection,
                Indexes.ascending("conversation"));

        this.batchContext.createNewUpdateBatch(
                "message",
                messageCollection, 2000,
                150);
        this.batchContext.createNewInsertBatch(
                "message",
                messageCollection, 2000,
                100);
        this.batchContext.createNewFindBatch(
                "conversation",
                conversationCollection,
                1000, 100);
    }

    public Document convertRecipient(Recipient recipient) {
        Document document = new Document();
        document.append("conversation",
                recipient.conversation()).append(
                "client",
                recipient.client()).append(
                "session",
                recipient.session());
        return document;
    }

    @Override
    public void readStoredMessageByCriteria(List<ReadMessageCriteria<String>> criteria, ForEach<PubMessage<BODY, ID>> forEach) {
        messageCollection.find(
                criteriaFilter(
                        criteria)).forEach(
                new Block<Document>() {
                    @Override
                    public void apply(Document document) {
                        try {
                            forEach.accept(
                                    parseMessage(
                                            document));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, (v, t) -> {
                    forEach.done(t);
                });
    }

    private final static Bson ID_SORT = new Document("_id" , 1);

    @Override
    public void readStoredMessage(Session session, ID offset, List<Long> conversations, ForEach<PubMessage<BODY, ID>> forEach) {

        messageCollection.find(
                fullMessageQuery(session , offset , conversations)
        ).sort(ID_SORT).forEach(
                d->forEach.accept(parseMessage(d)) ,
                (v,t)->forEach.done(t)
        );

        if(Boolean.TRUE)
            return;

        messageCollection.find(
                sessionToSessionFilter(offset , session)).sort(ID_SORT).forEach(
                new Block<Document>() {
                    @Override
                    public void apply(Document document) {
                        try {
                            forEach.accept(
                                    parseMessage(
                                            document));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, (v, t) -> {
                    if(t!=null) {
                        forEach.done(t);
                    }else {
                        messageCollection.find(
                                clientToClientFilter(offset , session)).sort(ID_SORT).forEach(
                                new Block<Document>() {
                                    @Override
                                    public void apply(Document document) {
                                        try {
                                            forEach.accept(
                                                    parseMessage(
                                                            document));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, (v1, t1) -> {
                                    if(t1!=null) {
                                        forEach.done(t1);
                                    }else {
                                        messageCollection.find(
                                                clientToConversationFilter(offset , conversations)).sort(ID_SORT).forEach(
                                                new Block<Document>() {
                                                    @Override
                                                    public void apply(Document document) {
                                                        try {
                                                            forEach.accept(
                                                                    parseMessage(
                                                                            document));
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }, (v2, t2) -> {
                                                    forEach.done(t2);
                                                });
                                    }
                                });
                    }
                });
    }

    public void addConversation(final long conversation, OperationResultListener<Long> callback) {
        conversationCollection.updateOne(
                eq("_id", conversation),
                setOnInsert("dump",
                        true),
                UPDATE_OPTIONS_WITH_UPSERT,
                new SingleResultCallback<UpdateResult>() {
                    @Override
                    public void onResult(UpdateResult updateResult, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(
                                    new FailReason(
                                            throwable,
                                            FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            if (updateResult.getMatchedCount() > 0) {
                                callback.onFailed(
                                        new FailReason(
                                                FailReasonsCodes.CONVERSATION_ALREADY_EXISTS));
                            } else {
                                if (updateResult.getUpsertedId() == null) {
                                    callback.onFailed(
                                            new FailReason(
                                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                                } else {
                                    callback.onSuccess(
                                            conversation);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public void isConversationExists(long conversation, OperationResultListener<Boolean> callback) {
        batchContext.getFindBatch(
                "conversation")
                .findOne(conversation,
                        new SingleResultCallback<Document>() {
                            @Override
                            public void onResult(Document document, Throwable throwable) {
                                if (throwable != null) {
                                    callback.onFailed(
                                            new FailReason(
                                                    throwable,
                                                    FailReasonsCodes.RUNTIME_EXCEPTION));
                                } else {
                                    callback.onSuccess(
                                            document != null);
                                }
                            }
                        });
    }

    @Override
    public void storeMessage(PubMessage<BODY, ID> message, OperationResultListener<PubMessage<BODY, ID>> result) {
        doStoreMessage(message, false,
                result);
    }

    @Override
    public void storeAsDisposableMessage(PubMessage<BODY, ID> message, OperationResultListener<PubMessage<BODY, ID>> result) {
        doStoreMessage(message, true,
                result);
    }

    @Override
    public void updateMessage(Session editor, Recipient recipient, ID messageId, BODY message, OperationResultListener<PubMessage<BODY, ID>> callback) {
        doUpdateMessage(editor,
                recipient, messageId,
                message, callback);
    }

    @Override
    public void removeMessage(Session session, Recipient recipient, ID messageId, OperationResultListener<PubMessage<BODY, ID>> result) {
        doRemoveMessage(session,
                recipient, messageId,
                result);
    }

    @Override
    public void setTag(Session session, Recipient recipient, InsertTag insertTag, OperationResultListener<Tag> callback) {
        String conversation = StaticFunctions.uniqueConversationId(
                session, recipient);
        Instant now = Instant.now();
        Document document = new Document().append(
                "name",
                insertTag.name()).append(
                "value",
                insertTag.value()).append(
                "client",
                session.clientId()).append(
                "time", now);
        batchContext.getUpdateBatch(
                "message").updateMany(
                and(eq("conversation",
                        conversation),
                        gte("_id",
                                insertTag.from()),
                        lte("_id",
                                insertTag.to())),
                push("tags", document),
                new SingleResultCallback<Void>() {
                    @Override
                    public void onResult(Void aVoid, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(
                                    new FailReason(
                                            FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            callback.onSuccess(
                                    new TagImpl(
                                            insertTag.name(),
                                            insertTag.value(),
                                            now,
                                            session.clientId()));
                        }
                    }
                });
    }

    @Override
    public void close() {
    }

    public void isConversationExists(long conversation, SingleResultCallback<Boolean> callback) {
        conversationCollection.find(
                eq("_id",
                        conversation)).first(
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onResult(
                                    null,
                                    throwable);
                        } else {
                            callback.onResult(
                                    document != null,
                                    null);
                        }
                    }
                });
    }

    private void doUpdateMessage(Session editor, Recipient recipient, ID messageId, BODY message, OperationResultListener<PubMessage<BODY, ID>> callback) {
        Bson update = set("body",
                message);
        String conversationId = StaticFunctions.uniqueConversationId(
                editor, recipient);
        PubMessage.Type type = StaticFunctions.getType(
                recipient);
        Bson filter = null;
        if (type.is(
                PubMessage.Type.SESSION_TO_SESSION)) {
            filter = and(
                    eq("conversation",
                            conversationId),
                    eq("id", messageId),
                    eq("publisher.client",
                            editor.clientId()),
                    eq("publisher.session",
                            editor.id()));
        } else {
            filter = and(
                    eq("conversation",
                            conversationId),
                    eq("id", messageId),
                    eq("publisher.client",
                            editor.clientId()));
        }
        messageCollection.findOneAndUpdate(
                filter, update,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(
                                    new FailReason(
                                            throwable,
                                            FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            if (document == null) {
                                callback.onFailed(
                                        new FailReason(
                                                FailReasonsCodes.MESSAGE_NOT_FOUND));
                            } else {
                                callback.onSuccess(
                                        parseMessage(
                                                document));
                            }
                        }
                    }
                });
    }

    private void doRemoveMessage(Session remover, Recipient recipient, ID messageId, OperationResultListener<PubMessage<BODY, ID>> callback) {
        String conversationId = StaticFunctions.uniqueConversationId(
                remover, recipient);
        PubMessage.Type type = StaticFunctions.getType(
                recipient);
        Bson filter = null;
        if (type.is(
                PubMessage.Type.SESSION_TO_SESSION)) {
            filter = and(
                    eq("conversation",
                            conversationId),
                    eq("id", messageId),
                    eq("publisher.client",
                            remover.clientId()),
                    eq("publisher.session",
                            remover.id()));
        } else {
            filter = and(
                    eq("conversation",
                            conversationId),
                    eq("id", messageId),
                    eq("publisher.client",
                            remover.clientId()));
        }
        messageCollection.findOneAndDelete(
                filter,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(
                                    new FailReason(
                                            throwable,
                                            FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            if (document == null) {
                                callback.onFailed(
                                        new FailReason(
                                                FailReasonsCodes.MESSAGE_NOT_FOUND));
                            } else {
                                callback.onSuccess(
                                        parseMessage(
                                                document));
                            }
                        }
                    }
                });
    }

    private void doStoreMessage(PubMessage<BODY, ID> message, boolean disposable, OperationResultListener<PubMessage<BODY, ID>> callback) {
        Instant time = Instant.now();
        PubMessage.Type type = StaticFunctions.getType(
                message.recipient());
        Document messageDocument = new Document(
                "_id", message.id());
        String conversationId = StaticFunctions.uniqueConversationId(
                message);
        messageDocument.append(
                "conversation",
                conversationId);
        if(type.is(PubMessage.Type.CLIENT_TO_CLIENT)){
            String[] recipientIndex = new String[2];
            recipientIndex[0] = String.valueOf(message.publisher().client());
            recipientIndex[1] = String.valueOf(message.recipient().client());
            messageDocument.append("recipientIndex" , Arrays.asList(recipientIndex));
        }else if(type.is(PubMessage.Type.SESSION_TO_SESSION)){
            String[] recipientIndex = new String[2];
            recipientIndex[0] = message.publisher().client()+"_"+message.publisher().session();
            recipientIndex[1] = message.recipient().client()+"_"+message.recipient().session();
            messageDocument.append("recipientIndex" , Arrays.asList(recipientIndex));
        }else {
            String [] recipientIndex = new String[1];
            recipientIndex[0] = "C"+message.recipient().conversation();
            messageDocument.append("recipientIndex" , Arrays.asList(recipientIndex));
        }
        messageDocument.append("body",
                message.body()).append(
                "type",
                type.code()).append(
                "recipient",
                convertRecipient(
                        message.recipient())).append(
                "publishedTime",
                time).append(
                "disposable",
                disposable).append(
                "publisher",
                new Document().append(
                        "client",
                        message.publisher().client()).append(
                        "session",
                        message.publisher().session()));
        batchContext.getInsertBatch(
                "message").insertOne(
                messageDocument,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if (throwable != null) {
                            callback.onFailed(
                                    new FailReason(
                                            throwable,
                                            FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            callback.onSuccess(
                                    message);
                        }
                    }
                });
    }

    private PubMessage parseMessage(Document document) {
        Document publisherDoc = (Document) document.get(
                "publisher");
        Document recipient = (Document) document.get(
                "recipient");
        return new PubMessageImpl(
                document.getString(
                        "_id"),
                document.get("body"),
                document.getDate(
                        "publishedTime").toInstant(),
                publisherDoc.getLong(
                        "client"),
                publisherDoc.getLong(
                        "session"),
                new RecipientImpl(
                        (long) recipient.getOrDefault(
                                "conversation",
                                -1),
                        (long) recipient.getOrDefault(
                                "client",
                                -1),
                        (long) recipient.getOrDefault(
                                "session",
                                -1)));
    }

    private static Document tagToDocument(Tag tag) {
        return new Document().append(
                "name",
                tag.name()).append(
                "time",
                tag.time()).append(
                "client", tag.client());
    }

    private static List<Document> tagsToListOfDocuments(Tag[] tags) {
        List<Document> documents = new ArrayList<>();
        for (Tag tag : tags) {
            documents.add(
                    tagToDocument(tag));
        }
        return documents;
    }

    private static Bson pushTagUpdate(Tag[] tags) {
        PushOptions options = new PushOptions().position(
                0);
        return pushEach("tags",
                tagsToListOfDocuments(
                        tags), options);
    }

    private final static Bson pushTagUpdate(Tag tag) {
        return push("tags",
                tagToDocument(tag));
    }

    private final Bson messageEquality(Session session, Recipient recipient) {
        if (recipient.conversation() != -1) {
            return eq("conversation",
                    recipient.conversation());
        } else {
            return eq("conversation",
                    StaticFunctions.uniqueConversationId(
                            session,
                            recipient));
        }
    }

    public void setTagOnMessages(Session session, Recipient recipient, Tag tag, long from, long lastIndex, SingleResultCallback<UpdateResult> callback) {
        messageCollection.updateMany(
                and(messageEquality(
                        session,
                        recipient),
                        eq("disposable",
                                false),
                        gt("id", from),
                        lte("id",
                                lastIndex)),
                pushTagUpdate(tag),
                callback);
    }

    private Bson sessionToSessionFilter(ID offset, Session session) {
        return and(gt("_id", offset),
                eq("type", PubMessage.Type.SESSION_TO_SESSION.code()),
                or(and(eq("publisher.client", session.clientId()),
                        eq("publisher.session", session.id())),
                        and(eq("recipient.client", session.clientId()),
                                eq("recipient.session", session.id()))));
    }

    private Bson clientToClientFilter(ID offset, Session session) {
        return and(gt("_id", offset),
                eq("type", PubMessage.Type.CLIENT_TO_CLIENT.code()),
                or(eq("publisher.client", session.clientId()),
                        eq("recipient.client", session.clientId())));
    }

    private Bson clientToConversationFilter(ID offset, List<Long> conversations) {
        return and(gt("_id", offset),
                eq("type", PubMessage.Type.CLIENT_TO_CONVERSATION.code()),
                in("recipient.conversation", conversations));
    }

    private Bson fullMessageQuery(Session session , ID offset , List<Long> conversation){
        List<String> indexes = new ArrayList<>();
        indexes.add(String.valueOf(session.clientId()));
        indexes.add(session.clientId()+"_"+session.id());
        for(Long id:conversation){
            indexes.add("C"+id);
        }
        return and(in("recipientIndex" , indexes) , gt("_id" , offset));
    }


    private final Bson clientMessageReadFilter(Session session, ID offset, List<Long> subscribeList) {

        return and(gt("_id", offset),
                or(
                ));


        /*and(
                gt("_id", offset),
                or(and(eq(
                                "recipient.client",
                                session.clientId()),
                        or(eq("recipient.session",
                                session.id()),
                                eq("recipient.session",
                                        -1))),
                        and(eq("publisher.client",
                                session.clientId()),
                                eq("recipient.conversation",
                                        -1),
                                or(eq("publisher.session",
                                        session.id()),
                                        eq("recipient.session",
                                                -1))),
                        in("recipient.conversation",
                                subscribeList)));*/
    }

    private final static Bson criteriaFilter(List<ReadMessageCriteria<String>> criteria) {
        return new Document();
    }

    @Deprecated
    public void numberOfMessages(String conversation, SingleResultCallback<Long> callback) {
        database.getCollection(
                conversation).countDocuments(
                callback);
    }

}
