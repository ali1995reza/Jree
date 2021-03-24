package jree.mongo_base;

import com.mongodb.internal.async.SingleResultCallback;

import java.util.concurrent.CountDownLatch;

final class SyncSingleResultCallback<T> implements SingleResultCallback<T> {

    private T result;
    private Throwable error;
    private final CountDownLatch latch = new CountDownLatch(1);

    private final void waitForResult()
    {
        try {
            latch.await();
        } catch (InterruptedException e) {
            while (latch.getCount()>0);
        }
    }

    @Override
    public void onResult(T t, Throwable throwable) {
        result = t;
        error = throwable;
        latch.countDown();
    }

    public T getResult()
    {
        waitForResult();
        if(error!=null)
            throw new IllegalStateException(error);
        return result;
    }
}
