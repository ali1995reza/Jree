package jree.test;

import com.mongodb.Block;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.SocketSettings;
import jree.api.*;
import jree.mongo_base.MongoPubSubSystem;
import jree.mongo_base.RecipientImpl;
import sun.security.x509.FreshestCRLExtension;

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
        public synchronized void onSuccess(Object result) {
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


        Session<String , String> session1_2 = mongoPubSubSystem.sessionManager()
                .connectToService(2, 1, new RelationController() {
                    @Override
                    public boolean validatePublishMessage(Relation relation) {
                        if(relation.setByClient(2).get("Block")!=null)
                            return false;

                        return true;
                    }
                } , new MyEventListener("1"));

        session1_2.setRelationProperties(RecipientImpl.clientRecipient(1) , "Block" , "TRUE");

        PubMessage message = session1_2.publishMessage(
                RecipientImpl.clientRecipient(1) ,
                "EHllo world"
        );


        System.out.println(message);


        Thread.sleep(1000000);
    }
}
