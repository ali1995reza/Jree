package jree.test;

import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.async.client.AsyncMongoClient;
import com.mongodb.internal.async.client.AsyncMongoClients;
import com.mongodb.internal.async.client.AsyncMongoCollection;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.api.OperationResultListener;
import jree.mongo_base.H2ConnectionPool;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.checkerframework.checker.units.qual.C;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Filter;

public class JDBCTest {

    private final static class Synchronizer<T> implements SingleResultCallback<T>
    {
        private Throwable throwable;
        private T t;
        private CountDownLatch latch;

        private Synchronizer<T> refresh()
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

    public final static void main(String[] args) throws Exception
    {
        AsyncMongoClient client = AsyncMongoClients.create();
        AsyncMongoDatabase database = client.getDatabase("Test");
        AsyncMongoCollection<Document> collection = database.getCollection("TEST");

        ObjectId id = new ObjectId(new byte[]{1 ,1});

        System.out.println(id);

        System.exit(1);

        collection.createIndex(Indexes.ascending("$**"),sync.refresh());
        sync.get();

        long s = System.currentTimeMillis();
        collection.find(
                Filters.and(
                        Filters.eq("F_1" , 125),
                        Filters.eq("F_2" , 523)
                )
        ).forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                System.out.println(System.currentTimeMillis()-s);
                System.out.println(document);
            }
        } , sync.refresh());

        sync.get();


    }
}
