package jree.test;

import jree.api.Subscribe;
import jree.util.concurrentiter.ConcurrentIter;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TestMongoDB  {



    private final static void runAndTick(Runnable runnable, String name)
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

    public static void main(String[] args) throws Exception
    {







        JdbcDataSource dataSource = new JdbcDataSource();

        dataSource.setURL("jdbc:h2:E:\\h2db\\db");
        Connection connection = dataSource.getConnection();

        Statement statement = connection.createStatement();


        runAndTick(new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<10000000;i++)
                {
                    try {
                        statement.execute("SELECT value FROM SUBS WHERE value = 2");

                        ResultSet set = statement.getResultSet();

                        int c = 0;
                        while (set.next()) {
                            ++c;
                            //System.out.println(set.getInt("value"));
                        }
                        System.out.println(c);
                        //System.out.println(c);

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    if(i%5==0)
                    {
                        System.out.println(i);
                    }
                }
            }
        } , "WHOLE QUERY TIME");





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
