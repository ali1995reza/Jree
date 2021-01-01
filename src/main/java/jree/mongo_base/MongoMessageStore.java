package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.client.model.*;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncFindIterable;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.api.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class MongoMessageStore {


    private final static FindOneAndUpdateOptions FIND_ONE_OPTIONS_WITH_UPSERT  =
            new FindOneAndUpdateOptions().upsert(true);
    private final static FindOneAndUpdateOptions FIND_ONE_OPTIONS_WITHOUT_UPSERT =
            new FindOneAndUpdateOptions().upsert(false);

    private final static UpdateOptions UPDATE_OPTIONS_WITH_UPSERT =
            new UpdateOptions().upsert(true);


    private final static String MESSAGE_COUNTER_COLLECTION_NAME = "MSG_COUNTER";
    private final static String MESSAGE_COLLECTION_NAME = "MSG";

    private final AsyncMongoDatabase database;
    private final AsyncMongoCollection<Document> messageCounterCollection;
    private final AsyncMongoCollection<Document> messageCollection;
    private MongoClientDetailsStore detailsStore;

    public MongoMessageStore(AsyncMongoDatabase database) {
        this.database = database;
        messageCounterCollection = this.database.getCollection(MESSAGE_COUNTER_COLLECTION_NAME);
        messageCollection = this.database.getCollection(MESSAGE_COLLECTION_NAME);
        DBStaticFunctions.createIndex(
                messageCollection  ,
                Indexes.ascending("conversation" , "id" , "disposable")
        );
    }

    void setDetailsStore(MongoClientDetailsStore detailsStore) {
        this.detailsStore = detailsStore;
    }

    public Document convertRecipient(Recipient recipient)
    {
        Document document = new Document();
        document.append(
                "conversation" , recipient.conversation()
        ).append(
                "client" , recipient.client()
        ).append(
                "session" , recipient.session()
        );

        return document;
    }

    private void getMessageId(String conversation , boolean upsert , SingleResultCallback<Long> idCallback)
    {
        Bson filter = eq("_id" , conversation);
        Bson update = inc("counter"  , 1l);

        messageCounterCollection.findOneAndUpdate(filter, update,
                upsert?FIND_ONE_OPTIONS_WITH_UPSERT:FIND_ONE_OPTIONS_WITHOUT_UPSERT
                , new SingleResultCallback<Document>() {
            @Override
            public void onResult(Document document, Throwable throwable) {
                if (throwable != null) {
                    idCallback.onResult(null, throwable);
                } else {
                    if (document == null) {
                        idCallback.onResult(upsert?1l:-1l, null);
                    } else {
                        long counter = (long) document.getOrDefault("counter", 0l);
                        ++counter;
                        idCallback.onResult(counter, null);
                    }
                }
            }
        });
    }


    public void getMessageIds(List<Subscribe> conversation , SingleResultCallback<List<MessageIndex>> callback)
    {

        AsyncFindIterable<Document> iterable =
                messageCounterCollection.find(in("_id" ,
                        new ConverterList<Subscribe , String>(conversation, new ConverterList.Converter<Subscribe, String>() {
                            @Override
                            public String convert(Subscribe subscribe) {
                                return String.valueOf(subscribe.conversation());
                            }
                        })));

        final List<MessageIndex> indexes = new ArrayList<>();

        iterable.forEach(new Block<Document>() {


            @Override
            public void apply(Document document) {

                indexes.add(
                        new MessageIndex(
                                document.getString("_id") ,
                                (Long) document.getOrDefault("counter" , 0l)
                        )
                );
            }
        }, new SingleResultCallback<Void>() {
            @Override
            public void onResult(Void aVoid, Throwable throwable) {
                if(throwable!=null)
                {

                    callback.onResult(null , throwable);

                }else {

                    if(indexes.size()==conversation.size())
                    {

                        callback.onResult(indexes , null);
                    }else {
                        callback.onResult(null , new IllegalStateException("not all conversation found"));
                    }
                }
            }
        });
    }

    public void createNewConversationIndex(final long conversation , OperationResultListener<Long> callback)
    {
        messageCounterCollection.updateOne(
                eq("_id", String.valueOf(conversation)),
                max("counter", 0l),
                UPDATE_OPTIONS_WITH_UPSERT,
                new SingleResultCallback<UpdateResult>() {
                    @Override
                    public void onResult(UpdateResult updateResult, Throwable throwable) {
                        if(throwable!=null)
                        {
                            callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                        }else
                        {
                            if(updateResult.getMatchedCount()>0)
                            {
                                callback.onFailed(new FailReason(MongoFailReasonsCodes.CONVERSATION_ALREADY_EXISTS));
                            }else{
                                if(updateResult.getUpsertedId()==null)
                                {
                                    callback.onFailed(new FailReason(MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                                }else {
                                    callback.onSuccess(conversation);
                                }
                            }
                        }
                    }
                }
        );
    }


    public void storeMessage(Session publisher , Recipient recipient ,
                             Object message , BodySerializer serializer , OperationResultListener<PubMessage> callback){

        boolean upsert = recipient.conversation()<0;

        getMessageId(
                StaticFunctions.uniqueConversationId(publisher , recipient) , upsert , new SingleResultCallback<Long>() {
                    @Override
                    public void onResult(Long messageId, Throwable throwable) {
                        if(throwable!=null)
                        {
                            callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                        }else
                        {
                            if(messageId==-1)
                            {
                                callback.onFailed(new FailReason(MongoFailReasonsCodes.CONVERSATION_NOT_EXISTS));
                            }else {
                                doStoreMessage(
                                        publisher,
                                        recipient,
                                        messageId,
                                        message,
                                        false ,
                                        serializer,
                                        callback

                                );
                            }
                        }
                    }
                }
        );
    }

    public <T> void updateMessage(Session editor , Recipient recipient ,
                              long messageId , T message , BodySerializer<T> serializer , OperationResultListener<PubMessage<T>> callback){

        Bson update = set("body" , serializer.serialize(message));
        String conversationId = StaticFunctions.uniqueConversationId(editor , recipient);
        PubMessage.Type type = StaticFunctions.getType(recipient);

        Bson filter = null;

        if(type.is(PubMessage.Type.SESSION_TO_SESSION)){

            filter = and(eq("conversation" , conversationId) ,
                    eq("id" , messageId) ,
                    eq("publisher.client" , editor.clientId()) ,
                    eq("publisher.session" , editor.id()));

        }else {


            filter = and(eq("conversation" , conversationId) ,
                    eq("id" , messageId) ,
                    eq("publisher.client" , editor.clientId()));

        }

        messageCollection.findOneAndUpdate(
                filter, update, new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if(throwable!=null){

                            callback.onFailed(new FailReason(throwable, MongoFailReasonsCodes.RUNTIME_EXCEPTION));

                        }else {

                            if(document==null)
                            {
                                callback.onFailed(new FailReason(MongoFailReasonsCodes.MESSAGE_NOT_FOUND));
                            }else {

                                callback.onSuccess(parseMessage(serializer , document));

                            }
                        }
                    }
                }
        );


    }

    public <T> void removeMessage(Session remover , Recipient recipient ,
                                  long messageId , BodySerializer<T> serializer, OperationResultListener<PubMessage<T>> callback)
    {
        String conversationId = StaticFunctions.uniqueConversationId(remover , recipient);
        PubMessage.Type type = StaticFunctions.getType(recipient);

        Bson filter = null;

        if(type.is(PubMessage.Type.SESSION_TO_SESSION)){

            filter = and(eq("conversation" , conversationId) ,
                    eq("id" , messageId) ,
                    eq("publisher.client" , remover.clientId()) ,
                    eq("publisher.session" , remover.id()));

        }else {


            filter = and(eq("conversation" , conversationId) ,
                    eq("id" , messageId) ,
                    eq("publisher.client" , remover.clientId()));

        }

        messageCollection.findOneAndDelete(
                filter, new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
                        if(throwable!=null){

                            callback.onFailed(new FailReason(throwable, MongoFailReasonsCodes.RUNTIME_EXCEPTION));

                        }else {

                            if(document==null)
                            {
                                callback.onFailed(new FailReason(MongoFailReasonsCodes.MESSAGE_NOT_FOUND));
                            }else {

                                callback.onSuccess(parseMessage(serializer , document));

                            }
                        }
                    }
                }
        );
    }

    public void storeDisposableMessage(Session publisher , Recipient recipient ,
                             Object message , BodySerializer serializer , OperationResultListener<PubMessage> callback){

        boolean upsert = recipient.conversation()<0;

        getMessageId(
                StaticFunctions.uniqueConversationId(publisher , recipient) , upsert , new SingleResultCallback<Long>() {
                    @Override
                    public void onResult(Long messageId, Throwable throwable) {
                        if(throwable!=null)
                        {
                            callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                        }else
                        {
                            if(messageId==-1)
                            {
                                callback.onFailed(new FailReason(MongoFailReasonsCodes.CONVERSATION_NOT_EXISTS));
                            }else {
                                doStoreMessage(
                                        publisher,
                                        recipient,
                                        messageId,
                                        message,
                                        true ,
                                        serializer,
                                        callback

                                );
                            }
                        }
                    }
                }
        );
    }




    public void doStoreMessage(Session publisher , Recipient recipient  , long messageId ,
                               Object message , boolean disposable , BodySerializer serializer , OperationResultListener<PubMessage> callback)
    {
        Instant time = Instant.now();
        PubMessage.Type type = StaticFunctions.getType(recipient);



        Document messageDocument = new Document("id" , messageId);
        messageDocument.append(
                "body" ,
                serializer.serialize(message)
        ).append(
                "type" ,
                type.code()
        ).append(
                "recipient" ,
                convertRecipient(recipient)
        ).append(
                "publishedTime" ,
                time
        ).append(
                "disposable" ,
                disposable
        ).append(
                "publisher" ,
                new Document()
                        .append("client" , publisher.clientId())
                        .append("session" , publisher.id())
        ).append("conversation" , StaticFunctions.uniqueConversationId(publisher ,recipient));

        if(messageId == 1 && type.isNot(PubMessage.Type.CLIENT_TO_CONVERSATION))
        {
            detailsStore.storeMessageZeroOffset(
                    publisher, recipient, type,
                    new SingleResultCallback<InsertManyResult>() {
                        @Override
                        public void onResult(InsertManyResult insertManyResult, Throwable throwable) {
                            if(throwable!=null)
                            {
                                callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                            }else {

                                messageCollection.insertOne(
                                        messageDocument,
                                        new SingleResultCallback<InsertOneResult>() {
                                            @Override
                                            public void onResult(InsertOneResult insertOneResult, Throwable throwable) {
                                                if(throwable!=null)
                                                {
                                                    callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                                                }else
                                                {
                                                    callback.onSuccess(
                                                            new PubMessageImpl(
                                                                    messageId ,
                                                                    message ,
                                                                    time ,
                                                                    type,
                                                                    publisher,
                                                                    recipient)
                                                    );
                                                }
                                            }
                                        }
                                );
                            }
                        }
                    }
            );
        }else {

            messageCollection.insertOne(
                    messageDocument,
                    new SingleResultCallback<InsertOneResult>() {
                        @Override
                        public void onResult(InsertOneResult insertOneResult, Throwable throwable) {
                            if(throwable!=null)
                            {
                                callback.onFailed(new FailReason(throwable , MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                            }else
                            {
                                callback.onSuccess(
                                        new PubMessageImpl(
                                                messageId ,
                                                message ,
                                                time ,
                                                type,
                                                publisher,
                                                recipient)
                                );
                            }
                        }
                    }
            );
        }

    }


    private final PubMessage parseMessage(BodySerializer serializer , Document document)
    {
        Document publisherDoc = (Document) document.get("publisher");
        Document recipient = (Document)document.get("recipient");


        return new PubMessageImpl(
                document.getLong("id") ,
                serializer.deserialize(((Binary) document.get("body")).getData()),
                document.getDate("publishedTime").toInstant() ,
                PubMessage.Type.findByCode(document.getInteger("type")) ,
                publisherDoc.getLong("client") ,
                publisherDoc.getLong("session"),
                new RecipientImpl(
                        (long)recipient.getOrDefault("conversation" , -1) ,
                        (long)recipient.getOrDefault("client" , -1) ,
                        (long)recipient.getOrDefault("session" , -1)
                ));
    }

    private final static Document tagToDocument(Tag tag)
    {
        return new Document()
                .append("name" , tag.name())
                .append("time" , tag.time())
                .append("client" , tag.client());
    }

    private final static List<Document> tagsToListOfDocuments(Tag[] tags)
    {
        List<Document> documents= new ArrayList<>();
        for(Tag tag:tags)
        {
            documents.add(tagToDocument(tag));
        }
        return documents;
    }

    private final static Bson pushTagUpdate(Tag[] tags)
    {
        PushOptions options = new PushOptions().position(0);
        return pushEach("tags" , tagsToListOfDocuments(tags) , options);
    }

    private final static Bson pushTagUpdate(Tag tag)
    {
        return push("tags" , tagToDocument(tag));
    }


    private final Bson messageEquality(Session session , Recipient recipient)
    {
        if(recipient.conversation()!=-1)
        {
            return eq("conversation" , recipient.conversation());
        }else {

            return eq("conversation" , StaticFunctions.uniqueConversationId(session , recipient));
        }
    }

    public void setTagOnMessages(Session session , Recipient recipient, Tag tag ,
                                 long from ,
                                 long lastIndex ,
                                 SingleResultCallback<UpdateResult> callback){




        messageCollection.updateMany(
                and(messageEquality(session, recipient) ,
                        eq("disposable" , false) ,
                        gt("id", from),
                        lte("id", lastIndex)),
                pushTagUpdate(tag),
                callback

        );
    }

    private final static Bson offsetFilter(List<ConversationOffset> offsets)
    {
        if(offsets==null || offsets.size()<1)
            return new Document();

        Bson[] ors = new Bson[offsets.size()];

        for(int i=0;i<offsets.size();i++)
        {
            ConversationOffset offset = offsets.get(i);

            ors[i] = and(eq("conversation" , offset.conversationId()) ,
                    gt("id" , offset.offset()) , eq("disposable" , false));
        }

        return or(ors);
    }

    private final static Bson criteriaFilter(List<ReadMessageCriteria> criteria)
    {
        if(criteria==null || criteria.size()<1)
            return new Document();

        Bson[] ors = new Bson[criteria.size()];

        for(int i=0;i<criteria.size();i++)
        {
            ReadMessageCriteria cri = criteria.get(i);


            if(cri.length()!=-1) {
                if (cri.backward()) {
                    ors[i] = and(eq("conversation", StaticFunctions.uniqueConversationId(cri.session(), cri.recipient())),
                            lt("id", cri.offset()) , gte("id" , cri.offset()-cri.length()));
                }else {
                    ors[i] = and(eq("conversation", StaticFunctions.uniqueConversationId(cri.session(), cri.recipient())),
                            gt("id", cri.offset()) , lte("id" , cri.offset()+cri.length()));
                }
            }else
            {
                if (cri.backward()) {
                    ors[i] = and(eq("conversation", StaticFunctions.uniqueConversationId(cri.session(), cri.recipient())),
                            lt("id", cri.offset()));
                }else {
                    ors[i] = and(eq("conversation", StaticFunctions.uniqueConversationId(cri.session(), cri.recipient())),
                            gt("id", cri.offset()));
                }
            }

        }

        return or(ors);
    }

    public void readStoredMessage(List<ConversationOffset> offsets ,
                                  BodySerializer serializer , Block<PubMessage> forEach , SingleResultCallback<Void> done)
    {
        if(offsets==null || offsets.size()<1) {
            done.onResult(null, null);
            return;
        }
        messageCollection.find(offsetFilter(offsets))
        .forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                try {
                    forEach.apply(parseMessage(serializer , document));
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        } , done);
    }

    public void readStoredMessageByCriteria(List<ReadMessageCriteria> criteria ,
                                            BodySerializer serializer , Block<PubMessage> forEach , SingleResultCallback<Void> done)
    {

        messageCollection.find(criteriaFilter(criteria))
                .forEach(new Block<Document>() {
                    @Override
                    public void apply(Document document) {
                        try {
                            forEach.apply(parseMessage(serializer , document));
                        }catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                } , done);
    }

    @Deprecated
    public void numberOfMessages(String conversation , SingleResultCallback<Long> callback)
    {
        database.getCollection(conversation).countDocuments(
                callback
        );
    }
}
