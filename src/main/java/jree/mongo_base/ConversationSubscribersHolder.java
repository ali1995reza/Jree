package jree.mongo_base;

import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.api.PubMessage;
import jree.api.Session;
import jree.util.Assertion;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class ConversationSubscribersHolder<T> {



    private final H2ConnectionPool subscribersDatabase;
    private final String tableName;
    private final String queryBaseString;
    private final ClientsHolder clients;

    public ConversationSubscribersHolder(ClientsHolder clients , String path)
    {
        this.clients = clients;

        this.tableName = "subscribers";
        subscribersDatabase = new H2ConnectionPool(5 , path);
        initDB(path);
        this.queryBaseString = "SELECT CL,S FROM "+tableName+" WHERE C = ";
    }

    private void initDB(String path)
    {
        try {
            Statement statement =
                    subscribersDatabase.getDataSource().getConnection().createStatement();
            try {
                statement.execute("DROP TABLE " + tableName);
            }catch (Exception e)
            {
                e.printStackTrace();
            }
            statement.execute("CREATE TABLE "+tableName+"(C BIGINT , CL BIGINT , S BIGINT)");
            statement.execute("CREATE INDEX C_INDEX ON "+tableName+" (C)");
            statement.execute("CREATE INDEX CL_INDEX ON "+tableName+" (CL)");
            statement.execute("CREATE INDEX S_INDEX ON "+tableName+" (S)");
        }catch (Throwable e)
        {
            throw new IllegalStateException(e);
        }
    }

    public ConversationSubscribersHolder<T> addSubscriber(
            Iterable<Long> conversations ,
            MongoSession<T> session ,
            OperationResultListener<Boolean> callback
    )
    {
        String sql = "INSERT INTO "+tableName+"(C , CL , S) VALUES"+commaSplitString(conversations , session);

        doAdd(sql , callback);

        return this;
    }


    public ConversationSubscribersHolder<T> addSubscriber(
            long conversation ,
            MongoSession<T> session ,
            OperationResultListener<Boolean> callback
    )
    {
        return doAdd(conversation , session , callback);
    }


    private final ConversationSubscribersHolder<T> doAdd(long conversation ,
                                                         MongoSession<T> session ,
                                                         OperationResultListener<Boolean> callback)
    {
        String insert = "INSERT INTO "+tableName+"(C , CL , S) VALUES ("+conversation+" , "+session.clientId()+" , "+session.id()+")";

        doAdd(insert , callback);

        return this;
    }

    private final ConversationSubscribersHolder<T> doAdd(String sql ,
                                                         OperationResultListener<Boolean> callback)
    {
        subscribersDatabase.execute(sql,
                new OperationResultListener<Statement>() {
                    @Override
                    public void onSuccess(Statement result) {
                        callback.onSuccess(true);
                    }

                    @Override
                    public void onFailed(FailReason reason) {
                        callback.onFailed(reason);
                    }
                });

        return this;
    }


    private static String commaSplitString(Iterable<Long> iterable , MongoSession session)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        Iterator<Long> iter = iterable.iterator();
        int counter = 0;
        if(iter.hasNext())
        {
            builder.append(iter.next());
            ++counter;
        }

        while (iter.hasNext())
        {
            builder.append(",").append(iter.next());
            ++counter;
        }

        return builder.toString()+" , "+createSessionMultiple(counter , session);
    }

    private static String commaSplitStringFromOffsets(
            Iterable<ConversationOffset> iterable ,
            MongoSession session
    )
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        Iterator<ConversationOffset> iter = iterable.iterator();
        int count = 0;
        while (iter.hasNext())
        {
            ConversationOffset offset = iter.next();
            if(offset.conversationId().contains("_"))
                continue;
            builder.append(offset.conversationId());
            ++count;
            break;
        }

        while (iter.hasNext())
        {
            ConversationOffset offset = iter.next();
            if(offset.conversationId().contains("_"))
                continue;

            builder.append(",").append(offset.conversationId());
            ++count;
        }

        return builder.toString()+" , "+createSessionMultiple(count , session);
    }

    private final static String createSessionMultiple(int size , Session session)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for(int i=0;i<size-1;i++)
        {
            builder.append(session.clientId()).append(",");
        }
        builder.append(session.clientId()).append(") , ");

        builder.append("(");
        for(int i=0;i<size-1;i++)
        {
            builder.append(session.id()).append(",");
        }
        builder.append(session.id()).append(") , ");

        return builder.toString();
    }


    public ConversationSubscribersHolder<T> addSubscriberByOffsets(
            Iterable<ConversationOffset> conversations ,
            MongoSession<T> session ,
            OperationResultListener<Boolean> callback
    )
    {
        String sql = "INSERT INTO "+tableName+"(C , CL , S) VALUES"+commaSplitStringFromOffsets(conversations , session);

        doAdd(sql , callback);
        return this;
    }


    public ConversationSubscribersHolder<T> publishMessage(
            PubMessage<T> pubMessage
    ){
        Assertion.ifTrue("recipient not a conversation" , pubMessage.recipient().conversation()<0);

        String sql = queryBaseString+pubMessage.recipient().conversation();

        subscribersDatabase.execute(sql, new OperationResultListener<Statement>() {
            @Override
            public void onSuccess(Statement result) {
                try {
                    ResultSet set = result.getResultSet();
                    while (set.next())
                    {
                        long client = set.getLong(1);
                        long session = set.getLong(2);
                        SessionsHolder sessionsHolder =
                                clients.getSessionsForClient(client);
                        if(sessionsHolder==null)
                            continue;
                        MongoSession<T> mongoSession = sessionsHolder.findSessionById(session);

                        if(mongoSession==null)
                            continue;

                        publishMessageToSubscribers(
                                mongoSession ,
                                pubMessage
                        );
                    }
                } catch (SQLException e) {
                    //todo handle it
                }
            }

            @Override
            public void onFailed(FailReason reason) {
                onFailed(reason);
            }
        });
        return this;
    }

    public ConversationSubscribersHolder<T> remove(MongoSession session ,
                                                   OperationResultListener<Boolean> callback) {
        String sql = "DELETE FROM "+tableName+" WHERE CL="+session.clientId()+" , S="+session.id();
        subscribersDatabase.execute(sql, new OperationResultListener<Statement>() {
            @Override
            public void onSuccess(Statement result) {
                callback.onSuccess(true);
            }

            @Override
            public void onFailed(FailReason reason) {
                callback.onFailed(reason);
            }
        });

        return this;
    }


    private final void publishMessageToSubscribers(MongoSession<T> session , PubMessage<T> message)
    {
        session.onMessagePublished(message);
    }




}
