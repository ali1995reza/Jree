package jree.mongo_base;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import jree.util.Assertion;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InsertBatch {


    private int size;
    private long waitTime;

    private final AsyncMongoCollection<Document> collection;

    private final ScheduledExecutorService executorService;


    private List<InsertOneModel<Document>> inserts;
    private List<SingleResultCallback<Document>> callbacks;

    private Object _sync = new Object();


    public InsertBatch(AsyncMongoCollection<Document> collection , int size , long waitTime , TimeUnit timeUnit)
    {
        Assertion.ifNull("collection is null" , collection);
        this.collection = collection;
        setSize(size).setWaitTime(waitTime, timeUnit);
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(this::timerCheck ,
                waitTime , waitTime , TimeUnit.MILLISECONDS);
        inserts = new ArrayList<>(size+10);
        callbacks = new ArrayList<>(size+10);

    }

    public InsertBatch(AsyncMongoCollection<Document> collection , int size , long waitTime)
    {
        this( collection , size , waitTime , TimeUnit.MILLISECONDS);
    }

    public InsertBatch(AsyncMongoCollection<Document> collection)
    {
        this(collection , 1000 , 5);
    }


    public InsertBatch setSize(int size) {
        Assertion.ifTrue("size must be positive" , size<1);
        this.size = size;
        return this;
    }

    public InsertBatch setWaitTime(long waitTime) {
        return setWaitTime(waitTime , TimeUnit.MILLISECONDS);
    }

    public InsertBatch setWaitTime(long time , TimeUnit timeUnit)
    {
        Assertion.ifTrue("wait-time must be positive" , time<1);
        this.waitTime = timeUnit.toMillis(time);
        return this;
    }

    public InsertBatch putInsert(final InsertOneModel<Document> insert ,
                                 final SingleResultCallback<Document> callback)
    {
        List<InsertOneModel<Document>> capturedInserts;
        List<SingleResultCallback<Document>> capturedCallbacks;
        synchronized (_sync)
        {
            inserts.add(insert);
            callbacks.add(callback);

            if(inserts.size()<size)
                return this;


            capturedInserts = inserts;
            capturedCallbacks = callbacks;

            inserts = new ArrayList<>(size+10);
            callbacks = new ArrayList<>(size+10);
        }


        System.out.println("Batch Size1 : "+capturedInserts.size());

        collection.bulkWrite(
                capturedInserts , new SingleResultCallback<BulkWriteResult>() {
                    @Override
                    public void onResult(BulkWriteResult bulkWriteResult, Throwable throwable) {
                        int totalInserted = bulkWriteResult.getInsertedCount();

                        for(int i=0;i<totalInserted;i++)
                        {
                            capturedCallbacks.get(i)
                                    .onResult(capturedInserts.get(i).getDocument() , null);
                        }

                        if(totalInserted==capturedInserts.size()) return;

                        throwable = throwable==null?
                                new IllegalStateException("document insertion fail"):throwable;


                        for(int i=totalInserted;i<capturedInserts.size();i++)
                        {
                            capturedCallbacks.get(i)
                                    .onResult(null , throwable);
                        }

                    }
                }
        );


        return this;
    }


    public void timerCheck()
    {


        List<InsertOneModel<Document>> capturedInserts;
        List<SingleResultCallback<Document>> capturedCallbacks;
        synchronized (_sync)
        {
            if(inserts.size()==0)
                return;

            capturedInserts = inserts;
            capturedCallbacks = callbacks;

            inserts = new ArrayList<>(size+10);
            callbacks = new ArrayList<>(size+10);
        }

        System.out.println("Batch Size2 : "+capturedInserts.size());
        collection.bulkWrite(
                capturedInserts , new SingleResultCallback<BulkWriteResult>() {
                    @Override
                    public void onResult(BulkWriteResult bulkWriteResult, Throwable throwable) {

                        int totalInserted = bulkWriteResult.getInsertedCount();

                        for(int i=0;i<totalInserted;i++)
                        {
                            capturedCallbacks.get(i)
                                    .onResult(capturedInserts.get(i).getDocument() , null);
                        }

                        if(totalInserted==capturedInserts.size()) return;

                        throwable = throwable==null?
                                new IllegalStateException("document insertion fail"):throwable;

                        for(int i=totalInserted;i<capturedInserts.size();i++)
                        {
                            capturedCallbacks.get(i)
                                    .onResult(null , throwable);
                        }

                    }
                }
        );
    }
}
