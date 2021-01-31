package jree.util;

import com.google.common.util.concurrent.FutureCallback;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class BatchExecutor<B>  {


    private int batchSize;
    private int capturedBatchSize;
    private ScheduledFuture timeChecker;
    private ScheduledExecutorService executorService;
    private List<B> batch;
    private final Object _sync = new Object();


    public BatchExecutor(ScheduledExecutorService service ,
                         int batchSize ,
                         long timeOut ){

        executorService = service;
        setBatchSize(batchSize);
        intiBatch();
        setTimeOut(timeOut , TimeUnit.MILLISECONDS);

    }


    public final BatchExecutor<B> setBatchSize(int batchSize)
    {
        Assertion.ifTrue("batch-size can not be less than 1" , batchSize<1);
        this.batchSize = batchSize;
        return this;
    }

    public final BatchExecutor<B> setTimeOut(long timeOut , TimeUnit unit)
    {
        synchronized (_sync)
        {
            if(timeChecker!=null)
            {
                Assertion.ifFalse("can not cancel current timout task" ,
                        timeChecker.cancel(false));
            }

            timeChecker = executorService.scheduleAtFixedRate(
                    this::timeCheck,
                    timeOut ,
                    timeOut ,
                    unit
            );
            return this;
        }
    }


    public BatchExecutor<B> putInBatch(B b){


        List<B> capturedBatch = null;
        synchronized (_sync)
        {
            if(!batch.add(b))
                throw new IllegalStateException("can not add to batch right now");

            if(batch.size()>=capturedBatchSize)
            {
                capturedBatch = batch;
                intiBatch();
            }else {
                return this;
            }
        }

        executeBatch(capturedBatch);
        return this;
    }

    private final void intiBatch()
    {
        capturedBatchSize = batchSize;
        batch = new ArrayList<>(capturedBatchSize+10);
    }

    private final void timeCheck()
    {
        List<B> capturedBatch = null;
        synchronized (_sync)
        {
            if(batch.isEmpty())
                return;


            capturedBatch = batch;
            intiBatch();
        }

        executeBatch(capturedBatch);

    }


    public abstract void executeBatch(List<B> batch);



}
