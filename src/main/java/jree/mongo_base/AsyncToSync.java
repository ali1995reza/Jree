package jree.mongo_base;

import jree.api.FailReason;
import jree.api.OperationResultListener;

import java.util.concurrent.CountDownLatch;

public class AsyncToSync<T> implements OperationResultListener<T> {


    private CountDownLatch latch;
    private T result;
    private FailReason failReason;

    public AsyncToSync() {
        latch = new CountDownLatch(1);
    }


    private void waitUninterrpbility()
    {
        try {
            latch.await();
        } catch (InterruptedException e) {
            while (latch.getCount()>0);
        }
    }

    @Override
    public void onSuccess(T result) {
        this.result = result;
        latch.countDown();
    }

    @Override
    public void onFailed(FailReason reason) {
        this.failReason = reason;
        latch.countDown();
    }


    public T getResult()
    {
        waitUninterrpbility();

        if(failReason!=null)
            throw failReason;

        return result;
    }


    public AsyncToSync<T> refresh()
    {
        failReason = null;
        result = null;
        latch = new CountDownLatch(1);

        return this;
    }
}
