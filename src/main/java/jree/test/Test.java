package jree.test;

import com.mongodb.Block;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.SocketSettings;
import jree.api.*;
import jree.mongo_base.MongoPubSubSystem;
import jree.mongo_base.RecipientImpl;

import java.util.concurrent.CountDownLatch;

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

    private final static class MyEventListener implements SessionEventListener<String>
    {

        private final String TAG;

        private MyEventListener(String tag) {
            TAG = tag;
        }

        @Override
        public void onMessagePublished(SessionContext context, PubMessage<String> message) {
            System.out.println(TAG+ " ON MSG : "+message);
        }

        @Override
        public void preInitialize(SessionContext context) {

        }

        @Override
        public void onInitialized(SessionContext context) {

        }

        @Override
        public void onClosedByException(Throwable exception) {
            exception.printStackTrace();
        }
    }

    private final static class EMPTYLISTENER implements SessionEventListener{

        @Override
        public void onMessagePublished(SessionContext context, PubMessage message) {

        }

        @Override
        public void preInitialize(SessionContext context) {

        }

        @Override
        public void onInitialized(SessionContext context) {

        }

        @Override
        public void onClosedByException(Throwable exception) {

        }
    }


    private final static BodySerializer<String> strSer = new BodySerializer<String>() {
        @Override
        public byte[] serialize(String object) {
            return object.getBytes();
        }

        @Override
        public String deserialize(byte[] data) {
            return new String(data);
        }
    };


    private final static class CounterOPListener implements OperationResultListener{

        private final CountDownLatch latch;
        private int counter = 0;

        private CounterOPListener(int latch) {
            this.latch = new CountDownLatch(latch);
        }


        @Override
        public void onSuccess(Object result) {
            if(((++counter)%1000)==0)
            {
                System.out.println(counter);
            }
            latch.countDown();
        }

        @Override
        public void onFailed(FailReason reason) {

        }

        public final void await() throws InterruptedException {
            latch.await();
        }
    }

    public static void main(String[] args) throws Exception
    {

        MongoPubSubSystem<String> mongoPubSubSystem =
                new MongoPubSubSystem<>("38921739" , strSer);

        /*mongoPubSubSystem.sessionManager()
                .createClient(1);
        mongoPubSubSystem.sessionManager()
                .createClient(2);

        long id = mongoPubSubSystem.sessionManager()
                .createSession(1);


        id = mongoPubSubSystem.sessionManager()
                .createSession(2);

        System.out.println("HEERE : "+id);

        id = mongoPubSubSystem.sessionManager()
                .createSession(2);

        System.out.println(id);

        System.out.println("WTF?");*/

        /*Session<String> session1_1 = mongoPubSubSystem.sessionManager()
        .connectToService(1 , 1 , new MyEventListener("SESSION 1-1"));*/

        Session<String> session1_2 = mongoPubSubSystem.sessionManager()
                .connectToService(1 , 2 , new MyEventListener("SS"));


        Recipient recipient = RecipientImpl.conversationRecipient(3371223121818720070l);


        /*CounterOPListener c = new CounterOPListener(1000000);
        for(int i=1;i<=1000000;i++)
        {

            session1_2.publishMessage(
                    recipient,
                    "HELLO",
                    c
            );
        }
        c.await();
         */
        Thread.sleep(1000000);
    }
}
