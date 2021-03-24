package jree.test;

import com.mongodb.internal.async.SingleResultCallback;
import jree.abs.utils.StringIDBuilder;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public class Tested {

    static int lastSec = -1;
    static int counter = 0;
    static int identify = 100;
    static ByteBuffer buffer = ByteBuffer.allocate(12);

    public final static class Synchronizer<T> implements SingleResultCallback<T>
    {
        private Throwable throwable;
        private T t;
        private CountDownLatch latch;

        public Synchronizer<T> refresh()
        {
            throwable = null;
            t = null;
            latch = new CountDownLatch(1);
            return this;
        }

        @Override
        public void onResult(T t, Throwable throwable) {
            this.throwable = throwable;
            this.t = t;
            latch.countDown();
        }

        public T get()
        {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            if(throwable!=null)
                throw new IllegalStateException(throwable);

            return t;

        }
    }

    private final static Synchronizer sync = new Synchronizer();

    private final static void runAndTick(Runnable runnable, String name)
    {
        long start = System.currentTimeMillis();
        runnable.run();
        long end = System.currentTimeMillis();
        System.out.println(name + " : "+(end-start));
    }


    public static void main(String[] args)
    {
        
        StringIDBuilder builder = new StringIDBuilder(24566, System::currentTimeMillis);

        runAndTick(new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<10;i++)
                {
                    System.out.println(builder.newId().getBytes().length);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } , "IDS");

    }
}
