package jree.test;

import jree.api.*;
import jree.mongo_base.MongoPubSubSystem;
import jree.mongo_base.RecipientImpl;

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

        Session<String> session1_1 = mongoPubSubSystem.sessionManager()
        .connectToService(1 , 1 , new MyEventListener("SESSION 1-1"));
        //session1.publishMessage(RecipientImpl.sessionRecipient(2 , 1) , "HELLO KIDO :)");
        //todo build it !
        Session<String> session2_2 = mongoPubSubSystem.sessionManager()
                .connectToService(2, 2  , new MyEventListener("SESSION 2-2"));

        Session<String> session2_1 = mongoPubSubSystem.sessionManager()
                .connectToService(2, 1  , new MyEventListener("SESSION 2-1"));

        PubMessage message = session1_1.publishMessage(RecipientImpl.clientRecipient(2) , "Hello 23232");

        session2_2.setMessageOffset(RecipientImpl.clientRecipient(1) , message.id());
        session2_1.setMessageOffset(RecipientImpl.clientRecipient(1) , message.id());

        Thread.sleep(1999);
    }
}
