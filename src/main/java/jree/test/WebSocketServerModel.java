package jree.test;

import com.mongodb.internal.async.client.AsyncMongoClient;
import com.mongodb.internal.async.client.AsyncMongoClients;
import com.mongodb.internal.async.client.AsyncMongoDatabase;
import jree.abs.PubSubSystemBuilder;
import jree.abs.objects.RecipientImpl;
import jree.abs.utils.StringIDBuilder;
import jree.api.*;
import jree.mongo_base.MongoDetailsStore;
import jree.mongo_base.MongoMessageStore;
import jree.mongo_base.batch.BatchContext;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;
import spark.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class WebSocketServerModel {

    private final static void waitForEver() throws InterruptedException {
        Object w = new Object();
        w.wait();
    }

    public static void main(String[] args) throws Exception {

        Spark.port(5566);
        Spark.ipAddress("0.0.0.0");
        Spark.webSocket("/chat", MessageHandler.class);
        Spark.get("/hello", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                if(Boolean.TRUE)
                    throw new IllegalStateException("wrd");
                return "Hello idiot !";
            }
        });

        Spark.exception(Exception.class, new ExceptionHandler() {
            @Override
            public void handle(Exception e, Request request, Response response) {
                e.printStackTrace();
            }
        });
        Spark.init();
    }

    @WebSocket
    public final static class MessageHandler {


        AsyncMongoClient client = AsyncMongoClients.create();
        AsyncMongoDatabase database = client.getDatabase("MSG_"+"MINE");
        BatchContext batchContext = new BatchContext(Executors.newScheduledThreadPool(1));


        final PubSubSystem<String, String> pubSubSystem = PubSubSystemBuilder.newBuilder(
                String.class, String.class
        ).setDetailsStore(new MongoDetailsStore<>(database , batchContext))
                .setMessageStore(new MongoMessageStore<>(database , batchContext))
                .setIdBuilder(new StringIDBuilder(1, System::currentTimeMillis))
                .build();


        private final ConcurrentHashMap<Session , jree.api.Session> users =
                new ConcurrentHashMap<>();

        public MessageHandler(){
            System.out.println("CONSTRUCT");
        }

        @OnWebSocketConnect
        public void onConnect(Session user) {

        }

        @OnWebSocketClose
        public void onClose(Session user , int code , String message) {
            //handle this like a charm !
            jree.api.Session session = users.remove(user);
            if (session!=null)
                session.close();
        }

        @OnWebSocketMessage
        public void onMessage(Session user , String msg) {
            JSONObject jsonObject = new JSONObject(msg);
            String command = jsonObject.getString("command");
            if(command.equalsIgnoreCase("connect")){
                long client = jsonObject.getLong("client");
                long session = jsonObject.getLong("session");
                long start = System.currentTimeMillis();
                jree.api.Session ss = pubSubSystem.sessionManager()
                        .openSession(client, session, RelationController.ALWAYS_ACCEPT, new SessionEventListener<String, String>() {
                            @Override
                            public void onMessagePublished(SessionContext context, PubMessage<String, String> message) {
                                try {
                                    user.getRemote().sendString(message.toString());
                                    context.currentSession().setMessageOffset(message.id());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onSignalReceived(SessionContext context, Signal<String> signal) {
                                try{
                                    user.getRemote().sendString(signal.toString());
                                }catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onInitialized(SessionContext context) {
                            }

                            @Override
                            public void onClosedByException(SessionContext context, Throwable exception) {
                                user.close();
                            }

                            @Override
                            public void onCloseByCommand(SessionContext context) {
                                user.close();
                            }
                        });

                System.out.println(System.currentTimeMillis()-start);

                users.put(user , ss);
            } else if(command.equalsIgnoreCase("publish")){
                String message = jsonObject.getString("message");
                long recipientClient = jsonObject.getLong("client");
                long recipientSession = -1;
                if(jsonObject.has("session")){
                    recipientSession = jsonObject.getLong("session");
                }

                users.get(user)
                        .publishMessage(RecipientImpl.sessionRecipient(recipientClient, recipientSession) ,
                                message);
            }else if(command.equalsIgnoreCase("signal")){
                String message = jsonObject.getString("message");
                long recipientClient = jsonObject.getLong("client");

                users.get(user)
                        .sendSignal(RecipientImpl.clientRecipient(recipientClient) ,
                                message);
            } else if(command.equalsIgnoreCase("remove")) {
                boolean b = pubSubSystem.sessionManager()
                        .removeClient(users.get(user).clientId());
                System.out.println(b);
            } else if(command.equalsIgnoreCase("remove_session")){
                boolean b = pubSubSystem.sessionManager()
                        .removeSession(users.get(user).clientId(), users.get(user).id());
                System.out.println(b);
            }
        }

    }

}
