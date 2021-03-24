package jree.test;

import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.abs.utils.H2ConnectionPool;
import jree.util.concurrentiter.ConcurrentIter;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TestMongoDB  {



    public final static void runAndTick(Runnable runnable, String name)
    {
        long start = System.currentTimeMillis();
        runnable.run();
        long end = System.currentTimeMillis();
        System.out.println(name + " : "+(end-start));
    }


    private final static void await(long l)
    {
        try {
            Thread.sleep(l);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }


    private final static class StatementResult implements OperationResultListener<Statement>{

        private final CountDownLatch latch;
        private AtomicInteger integer = new AtomicInteger(0);

        private StatementResult(int count) {
            this.latch = new CountDownLatch(count);
        }

        @Override
        public void onSuccess(Statement result) {
            try {
                ResultSet resultSet = result.getResultSet();
                int count = 0;
                while (resultSet.next())
                {
                    resultSet.getInt(1);
                    ++count;
                }
                latch.countDown();
                int now = integer.incrementAndGet();
                if(now%1000000==0)
                {
                    System.out.println(now);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            reason.printStackTrace();
            latch.countDown();
        }
    }


    public static void main(String[] args) throws Exception
    {


        StringBuilder builder = new StringBuilder();
        builder.append("12");
        builder.codePointAt(0);
        builder.append("3");
        System.out.println(builder.toString());

        System.exit(1);

        H2ConnectionPool pool = new H2ConnectionPool(10 , "jdbc:h2:E:\\h2db\\db");

        StatementResult result = new StatementResult(1000000);


        runAndTick(new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<1000000;i++)
                {
                    pool.execute("SELECT value FROM SUBS WHERE value = 1" , result);
                }

                try {
                    result.latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } , "EXEC_ASYNC");





        await(1999999999999l);


        final int SIZE = 20000000;

        if(true) {

            final ConcurrentHashMap map = new ConcurrentHashMap();


            runAndTick(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < SIZE; i++) {
                        map.put(i, i);
                    }
                }
            }, "MAP-ADD");

            runAndTick(new Runnable() {
                @Override
                public void run() {
                    int i =0;
                    for (Object val : map.values()) {
                        ++i;
                    }
                    System.out.println(i);
                }
            }, "MAP-ITERATE");

            await(100000000);

        }else {

            ConcurrentIter concurrentIter = new ConcurrentIter();
            List list = new ArrayList();
            runAndTick(new Runnable() {
                @Override
                public void run() {
                    for(int i=0;i<SIZE;i++)
                    {
                        list.add(concurrentIter.add(i));
                    }
                }
            } , "ITER-ADD");

            runAndTick(new Runnable() {
                @Override
                public void run() {
                    concurrentIter.forEach(new Consumer() {
                        int i  = 0;
                        @Override
                        public void accept(Object o) {
                            if(++i==SIZE)
                                System.out.println(i);
                        }
                    });
                }
            } , "Iter-ITERATE");

            await(1000000);
        }
    }
}
