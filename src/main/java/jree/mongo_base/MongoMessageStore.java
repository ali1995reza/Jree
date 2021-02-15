package jree.mongo_base;

import com.mongodb.Block;
import com.mongodb.client.model.*;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.api.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.*;

public class MongoMessageStore {


    private final static FindOneAndUpdateOptions FIND_ONE_OPTIONS_WITH_UPSERT  =
            new FindOneAndUpdateOptions().upsert(true);
    private final static FindOneAndUpdateOptions FIND_ONE_OPTIONS_WITHOUT_UPSERT =
            new FindOneAndUpdateOptions().upsert(false);

    private final static UpdateOptions UPDATE_OPTIONS_WITH_UPSERT =
            new UpdateOptions().upsert(true);


    private final static String CONVERSATION_COLLECTION_NAME = "MSG_COUNTER";
    private final static String MESSAGE_COLLECTION_NAME = "MSG";

    private final AsyncMongoDatabase database;
    private final AsyncMongoCollection<Document> messageCollection;
    private final AsyncMongoCollection<Document> conversationCollection;
    private MongoClientDetailsStore detailsStore;
    private final BatchContext batchContext;
    private final IDBuilder idBuilder = new IDBuilder(1, System::currentTimeMillis);

    public MongoMessageStore(AsyncMongoDatabase database, BatchContext batchContext) {
        this.database = database;
        this.batchContext = batchContext;
        messageCollection = this.database.getCollection(MESSAGE_COLLECTION_NAME);
        conversationCollection = this.database.getCollection(CONVERSATION_COLLECTION_NAME);
        DBStaticFunctions.createIndex(
                messageCollection  ,
                Indexes.ascending("conversation" , "id" , "disposable")
        );

        this.batchContext.createNewUpdateBatch("message" , messageCollection , 2000 , 150);
        this.batchContext.createNewInsertBatch("message" , messageCollection , 2000 , 100);

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

    public void createNewConversationIndex(final long conversation , OperationResultListener<Long> callback)
    {
        conversationCollection.updateOne(
                eq("_id",conversation),
                setOnInsert("dump" , true) ,
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


    public void setTag(Session session, Recipient recipient , InsertTag insertTag,
                       OperationResultListener<Tag> callback)
    {

        final FindOneAndUpdateOptions deliveryOption = new FindOneAndUpdateOptions()
                .upsert(true)
                .projection(include(insertTag.name()));

        String conversation = StaticFunctions.uniqueConversationId(session , recipient);


        Instant now = Instant.now();

        Document document = new Document()
                .append("name" , insertTag.name())
                .append("value" , insertTag.value())
                .append("client" , session.clientId())
                .append("time" , now);

        batchContext.getUpdateBatch("message").updateMany(
                and(eq("conversation", conversation),
                        gte("_id", insertTag.from()),
                        lte("_id", insertTag.to())),
                push("tags", document),

                new SingleResultCallback<Void>() {
                    @Override
                    public void onResult(Void aVoid, Throwable throwable) {
                        if(throwable!=null)
                        {
                            callback.onFailed(new FailReason(MongoFailReasonsCodes.RUNTIME_EXCEPTION));
                        }else {
                            callback.onSuccess(
                                    new TagImpl(insertTag.name() , insertTag.value() ,
                                            now ,session.clientId())
                            );
                        }
                    }
                }

        );
    }

    public void isConversationExists(long conversation , SingleResultCallback<Boolean> callback){

        conversationCollection
                .find(eq("_id" , conversation))
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


    public void storeMessage(Session publisher , Recipient recipient ,
                             Object message , BodySerializer serializer , OperationResultListener<PubMessage> callback){

        doStoreMessage(
                publisher,
                recipient,
                message,
                false,
                serializer,
                callback

        );
    }

    public <T> void updateMessage(Session editor , Recipient recipient ,
                              String messageId , T message , BodySerializer<T> serializer , OperationResultListener<PubMessage<T , String>> callback){

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
                                  String messageId , BodySerializer<T> serializer, OperationResultListener<PubMessage<T , String>> callback)
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


        doStoreMessage(
                publisher,
                recipient,
                message,
                true ,
                serializer,
                callback

        );
    }




    public void doStoreMessage(Session publisher , Recipient recipient ,
                               Object message , boolean disposable , BodySerializer serializer , OperationResultListener<PubMessage> callback)
    {
        Instant time = Instant.now();
        PubMessage.Type type = StaticFunctions.getType(recipient);

        String messageId = idBuilder.newId();

        Document messageDocument = new Document("_id" , messageId);
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

        batchContext.getInsertBatch("message").insertOne(
                messageDocument,
                new SingleResultCallback<Document>() {
                    @Override
                    public void onResult(Document document, Throwable throwable) {
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


    private final PubMessage parseMessage(BodySerializer serializer , Document document)
    {
        Document publisherDoc = (Document) document.get("publisher");
        Document recipient = (Document)document.get("recipient");


        return new PubMessageImpl(
                document.getString("_id") ,
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

    private final static Bson offsetFilter(ConversationOffset offset)
    {
        if(offset==null)
            return new Document();

        Bson bson = and(eq("conversation" , offset.conversationId()) ,
                gt("id" , offset.offset()) , eq("disposable" , false));

        return bson;
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

    public void readStoredMessage(ConversationOffset offset ,
                                  BodySerializer serializer , Block<PubMessage> forEach , SingleResultCallback<Void> done)
    {
        messageCollection.find(offsetFilter(offset))
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
