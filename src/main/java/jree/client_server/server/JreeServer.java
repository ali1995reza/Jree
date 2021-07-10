package jree.client_server.server;

import com.mongodb.client.MongoClients;
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
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;
import spark.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class JreeServer {

    private final int port;


    private final PubSubSystem<String, String> pubSubSystem;
    private final LoginSignupHandler loginSignupHandler;

    public JreeServer(int port) {
        this.port = port;
        AsyncMongoClient client = AsyncMongoClients.create();
        AsyncMongoDatabase database = client.getDatabase("MSG_"+"MINE");
        BatchContext batchContext = new BatchContext(Executors.newScheduledThreadPool(1));
        loginSignupHandler = new LoginSignupHandler(MongoClients.create().getDatabase("USERS_DB"));
        pubSubSystem = PubSubSystemBuilder.newBuilder(
                String.class, String.class
        ).setDetailsStore(new MongoDetailsStore<>(database , batchContext))
                .setMessageStore(new MongoMessageStore<>(database , batchContext))
                .setIdBuilder(new StringIDBuilder(1, System::currentTimeMillis))
                .build();
    }


    public void start() {
        Spark.port(port);
        Spark.ipAddress("0.0.0.0");
        Spark.webSocket("/chat", new MessageHandler(loginSignupHandler, pubSubSystem));
        Spark.post("/login",this::login);
        Spark.post("/signup",this::signup);
        Spark.exception(Exception.class, new ExceptionHandler() {
            @Override
            public void handle(Exception e, Request request, Response response) {
                e.printStackTrace();
            }
        });
        Spark.init();
    }

    public Object login(Request request, Response response) {
        JSONObject jsonObject = new JSONObject(request.body());
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");
        String token = null;
        try {
            token = loginSignupHandler.login(username, password, this::createSession);
            System.out.println("CREATED TOKEN IS : "+token);
        } catch (LoginSignupException e) {
            response.status(e.getCode());
            return null;
        }
        return new JSONObject().put("token", token).toString();
    }

    private Long createSession(Long l) {
        return pubSubSystem.sessionManager().createSession(l);
    }

    private Long createClient() {
        return pubSubSystem.sessionManager().createClient();
    }


    public Object signup(Request request, Response response) {
        JSONObject jsonObject = new JSONObject(request.body());
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");

        try {
            loginSignupHandler.signup(username, password, this::createClient);
        } catch (LoginSignupException e) {
            response.status(e.getCode());
            return null;
        }
        return "OK";
    }


    @WebSocket
    public final static class MessageHandler {
        private final ConcurrentHashMap<org.eclipse.jetty.websocket.api.Session, SessionAndCommands> users =
                new ConcurrentHashMap<>();

        private final LoginSignupHandler loginSignupHandler;
        private final PubSubSystem<String, String> pubSubSystem;

        public MessageHandler(LoginSignupHandler loginSignupHandler, PubSubSystem<String, String> pubSubSystem){
            this.loginSignupHandler = loginSignupHandler;
            this.pubSubSystem = pubSubSystem;
        }

        @OnWebSocketConnect
        public void onConnect(org.eclipse.jetty.websocket.api.Session user) {

        }

        @OnWebSocketClose
        public void onClose(org.eclipse.jetty.websocket.api.Session user , int code , String message) {
            //handle this like a charm !
            SessionAndCommands session = users.remove(user);
            if (session!=null)
                session.getSession().close();
        }

        @OnWebSocketMessage
        public void onMessage(Session user , String msg) {
            JSONObject jsonObject = new JSONObject(msg);
            String command = jsonObject.getString("command");
            if(command.equalsIgnoreCase("connect")){
                String token = jsonObject.getString("token");
                Long[] login = loginSignupHandler.verifyToken(token);
                long start = System.currentTimeMillis();
                jree.api.Session ss = pubSubSystem.sessionManager()
                        .openSession(login[0], login[1], RelationController.ALWAYS_ACCEPT, new SessionEventListener<String, String>() {
                            @Override
                            public void onMessagePublished(SessionContext context, PubMessage<String, String> message) {
                                try {
                                    user.getRemote().sendString(message.toString());
                                    context.currentSession().attach(message.id());
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
                            public void onClosing(SessionContext context) {
                                context.currentSession()
                                        .setMessageOffset(context.currentSession().attachment());
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

                users.put(user , new SessionAndCommands(pubSubSystem, ss));
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
            } else if(command.equalsIgnoreCase("getMessages")) {
                SessionAndCommands sessionAndCommands = users.get(user);
                Iterable<PubMessage<String, String>> iter = sessionAndCommands.getMessagesOfAllConversations(20);
                for(PubMessage message:iter) {
                    try {
                        user.getRemote().sendString(message.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if(command.equalsIgnoreCase("publish2")){

                String username = jsonObject.getString("username");
                Long id = loginSignupHandler.getClientIdFromUsername(username);

                if(id==null)
                    return;

                String message = jsonObject.getString("message");

                users.get(user)
                        .publishMessage(RecipientImpl.clientRecipient(id) ,
                                message);
            }/*else if(command.equalsIgnoreCase("remove")) {
                boolean b = pubSubSystem.sessionManager()
                        .removeClient(users.get(user).clientId());
                System.out.println(b);
            } else if(command.equalsIgnoreCase("remove_session")){
                boolean b = pubSubSystem.sessionManager()
                        .removeSession(users.get(user).clientId(), users.get(user).id());
                System.out.println(b);
            }*/
        }
    }
}
