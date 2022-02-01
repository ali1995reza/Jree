package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.PushOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static jree.mongo_base.CollectionNames.*;
import static jree.mongo_base.StaticFiltersOptionsSorts.*;

public class MongoMessageStore<BODY, ID extends Comparable<ID>> implements MessageStore<BODY, ID> {

    private final class MessageCriteriaReader implements ForEach<PubMessage<BODY,ID>> {

        private final ForEach<PubMessage<BODY,ID>> wrapped;
        private final List<ReadMessageCriteria<ID>> criteriaList;
        private int index = 0;

        private MessageCriteriaReader(ForEach<PubMessage<BODY, ID>> wrapped, List<ReadMessageCriteria<ID>> criteriaList) {
            this.wrapped = wrapped;
            this.criteriaList = criteriaList==null? Collections.emptyList():criteriaList;
            done(null);
        }

        @Override
        public void accept(PubMessage<BODY, ID> message) {
            wrapped.accept(message);
        }

        @Override
        public void done(Throwable e) {
            if(e==null) {
                if(criteriaList.size()==index) {
                    wrapped.done(null);
                } else {
                    ReadMessageCriteria<ID> criteria = criteriaList.get(index++);
                    doReadMessage(criteria, this);
                }
            } else {
                wrapped.done(e);
            }
        }

    }





    //private final AsyncMongoDatabase database;
    private final CollectionInfo messageCollection;
    private final CollectionInfo conversationCollection;
    //private final BatchContext batchContext;

    public MongoMessageStore(AsyncMongoDatabase database, BatchContext batchContext) {
        //this.database = database;
        //this.batchContext = batchContext;
        messageCollection = new CollectionInfo(MESSAGE_COLLECTION_NAME, database);
        conversationCollection = new CollectionInfo(CONVERSATIONS_COLLECTION_NAME, database);

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

        DBStaticFunctions.createIndex(messageCollection.collection() ,
                Indexes.ascending(
                        "recipientIndex" ,
                        "_id"
                ));

        DBStaticFunctions.createIndex(messageCollection.collection(),
                Indexes.ascending("conversation"));

        batchContext.createNewUpdateBatch(messageCollection, 2000, 150);
        batchContext.createNewInsertBatch(messageCollection, 2000, 100);
        batchContext.createNewFindBatch(conversationCollection, 1000, 100);
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


    private void doReadMessage(ReadMessageCriteria<ID> criteria, ForEach<PubMessage<BODY, ID>> forEach) {
        messageCollection.collection().find(criteriaFilter(criteria))
                .sort(criteria.backward()?ID_SORT_REVERSE:ID_SORT)
                .limit(criteria.length())
                .forEach(d->{
                    try {
                        forEach.accept(
                                parseMessage(d));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, (v,t)->{
                    forEach.done(t);
                });
    }

    @Override
    public void readStoredMessageByCriteria(List<ReadMessageCriteria<ID>> criteria, ForEach<PubMessage<BODY, ID>> forEach) {
        new MessageCriteriaReader(forEach, criteria);
    }



    @Override
    public void readStoredMessage(Session session, ID offset, List<Long> conversations, ForEach<PubMessage<BODY, ID>> forEach) {

        messageCollection.collection().find(
                fullMessageQuery(session , offset , conversations)
        ).sort(ID_SORT).forEach(
                d->forEach.accept(parseMessage(d)) ,
                (v,t)->forEach.done(t)
        );

        if(Boolean.TRUE)
            return;

        messageCollection.collection().find(
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
                        messageCollection.collection().find(
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
                                        messageCollection.collection().find(
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
        conversationCollection.collection().updateOne(
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
        conversationCollection.findByIdBatch()
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
        messageCollection.updateBatch().updateMany(
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
    public void getRecipientsList(long client, long session, OperationResultListener<List<Recipient>> callback) {
        String clientRecipientId = String.valueOf(client);
        String sessionRecipientId = String.valueOf(client).concat("_").concat(String.valueOf(session));

        final List<Recipient> recipients = new ArrayList<>();

        messageCollection.collection().distinct("recipientIndex",String.class)
                .filter(in("recipientIndex", clientRecipientId, sessionRecipientId))
                .forEach(new Block<String>() {
                    @Override
                    public void apply(String s) {
                        String[] split = s.split("_");
                        if(split.length==1) {
                            Long recipientClient = Long.parseLong(split[0]);
                            if(recipientClient==client){
                                return;
                            }
                            recipients.add(RecipientImpl.clientRecipient(recipientClient));
                        } else {
                            //2
                            Long recipientClient = Long.parseLong(split[0]);
                            Long recipientSession = Long.parseLong(split[1]);
                            if(recipientClient==client){
                                return;
                            }
                            recipients.add(RecipientImpl.sessionRecipient(recipientClient, recipientSession));
                        }
                    }
                }, new SingleResultCallback<Void>() {
                    @Override
                    public void onResult(Void unused, Throwable throwable) {
                        if(throwable!=null) {
                            callback.onFailed(new FailReason(throwable, FailReasonsCodes.RUNTIME_EXCEPTION));
                        } else {
                            callback.onSuccess(recipients);
                        }
                    }
                });

    }

    @Override
    public void close() {
    }

    public void isConversationExists(long conversation, SingleResultCallback<Boolean> callback) {
        conversationCollection.findByIdBatch().findOne(conversation, new SingleResultCallback<Document>() {
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
        messageCollection.collection().findOneAndUpdate(
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
        messageCollection.collection().findOneAndDelete(
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
        messageCollection.insertBatch().insertOne(
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
        messageCollection.collection().updateMany(
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

    private Bson criteriaFilter(ReadMessageCriteria<ID> criteria) {
        String id =
                StaticFunctions.uniqueConversationId(criteria.session(), criteria.recipient());
        Bson bson = eq("conversation", id);
        if(criteria.backward()) {
            bson = and(bson, lt("_id", criteria.from()), eq("disposable", criteria.containsDisposables()));
        } else {
            bson = and(bson, gt("_id", criteria.from()), eq("disposable", criteria.containsDisposables()));
        }

        return bson;
     }


}
