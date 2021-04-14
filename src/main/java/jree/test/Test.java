package jree.test;

import com.mongodb.internal.async.client.AsyncMongoClient;
import com.mongodb.internal.async.client.AsyncMongoClients;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.abs.PubSubSystemBuilder;
import jree.abs.utils.StringIDBuilder;
import jree.api.*;
import jree.abs.objects.RecipientImpl;
import jree.mongo_base.batch.BatchContext;
import jree.mongo_base.MongoDetailsStore;
import jree.mongo_base.MongoMessageStore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class Test {

    private final static OperationResultListener LOGGER_LISTENER = new OperationResultListener() {
        @Override
        public void onSuccess(Object result) {
            System.out.println("OnSuccess : "+result);
        }

        @Override
        public void onFailed(FailReason reason) {
            reason.printStackTrace();
        }
    };

    private final static class MyEventListener implements SessionEventListener<String , String>
    {

        private final String TAG;

        private MyEventListener(String tag) {
            TAG = tag;
        }

        @Override
        public void onMessagePublished(SessionContext context, PubMessage<String , String> message) {
            System.out.println(TAG+ " ON MSG : "+message);
        }

        @Override
        public void onSignalReceived(SessionContext context, Signal<String> signal) {

        }

        @Override
        public void preInitialize(SessionContext context) {

        }

        @Override
        public void onInitialized(SessionContext context) {

        }

        @Override
        public void onClosedByException(SessionContext context, Throwable exception) {

        }

        @Override
        public void onCloseByCommand(SessionContext context) {

        }

    }

    private final static class EMPTYLISTENER implements SessionEventListener{

        @Override
        public void onMessagePublished(SessionContext context, PubMessage message) {

        }

        @Override
        public void onSignalReceived(SessionContext context, Signal signal) {

        }

        @Override
        public void preInitialize(SessionContext context) {

        }

        @Override
        public void onInitialized(SessionContext context) {

        }

        @Override
        public void onClosedByException(SessionContext context, Throwable exception) {

        }

        @Override
        public void onCloseByCommand(SessionContext context) {

        }

    }


    private final static class CounterOPListener implements OperationResultListener{

        private final CountDownLatch latch;
        private int counter = 0;

        private CounterOPListener(int latch) {
            this.latch = new CountDownLatch(latch);
        }


        @Override
        public synchronized void onSuccess(Object result) {
            if(((++counter)%100000)==0)
            {
                System.out.println(counter);
            }
            latch.countDown();
        }

        @Override
        public void onFailed(FailReason reason) {
            reason.printStackTrace();
        }

        public final void await() throws InterruptedException {
            latch.await();
        }
    }



    public static void main(String[] args) throws Exception
    {
        AsyncMongoClient client = AsyncMongoClients.create();
        AsyncMongoDatabase database = client.getDatabase("MSG_"+"MINE");
        BatchContext batchContext = new BatchContext(Executors.newScheduledThreadPool(1));


        PubSubSystem<String, String> pubSubSystem = PubSubSystemBuilder.newBuilder(
                String.class, String.class
        ).setDetailsStore(new MongoDetailsStore<>(database , batchContext))
                .setMessageStore(new MongoMessageStore<>(database , batchContext))
                .setIdBuilder(new StringIDBuilder(1, System::currentTimeMillis))
                .build();

        /*pubSubSystem.sessionManager()
                .createClient(1);
        pubSubSystem.sessionManager()
                .createClient(2);

        long id = pubSubSystem.sessionManager()
                .createSession(1);


        id = pubSubSystem.sessionManager()
                .createSession(2);

        System.out.println("HEERE : "+id);

        id = pubSubSystem.sessionManager()
                .createSession(2);

        System.out.println(id);

        System.out.println("WTF?");

        System.exit(1);*/

        long start = System.currentTimeMillis();
        Session<String , String> session2_x = pubSubSystem.sessionManager()
                .openSession(2, 2592858883883676207l, new RelationController() {
                    @Override
                    public boolean validatePublishMessage(Relation relation) {
                        if(relation.setByClient(2).get("Block")!=null)
                            return false;

                        return true;
                    }
                } , new MyEventListener("SSSSSS+_+_+_"));

        System.out.println(System.currentTimeMillis()-start);

        session2_x.subscribe(100);

        //session2_x.setRelationProperties(RecipientImpl.clientRecipient(1) , "Block" , "TRUE");


        int NUMBER = 0;
        CounterOPListener listener = new CounterOPListener(NUMBER);

        long s = System.currentTimeMillis();
        for(int i=0;i<NUMBER;i++)
        {
            session2_x.publishMessage(
                    RecipientImpl.conversationRecipient(100) ,
                    "A message maybe !" ,
                listener
            );
        }

        listener.await();

        System.out.println(System.currentTimeMillis()-s);

        /*long start = System.currentTimeMillis();
        Tag tag = session2_x.addTag(RecipientImpl.clientRecipient(1),
                new InsertTag().withName("recv").andValue("seen").from("0").to("0000017859af148a000000010000041b"));
        System.out.println(tag);
        System.out.println(System.currentTimeMillis()-start);*/

        Thread.sleep(1000000);
    }
}
