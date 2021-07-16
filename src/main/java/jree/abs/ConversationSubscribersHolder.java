package jree.abs;

import jree.abs.funcs.AsyncToSync;
import jree.abs.utils.H2ConnectionPool;
import jree.api.*;
import jree.util.Assertion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

final class ConversationSubscribersHolder<BODY, ID extends Comparable<ID>> {


    private final H2ConnectionPool subscribersDatabase;
    private final String tableName;
    private final String queryBaseString;
    private final ClientsHolder clients;

    public ConversationSubscribersHolder(ClientsHolder clients, String path) {
        this.clients = clients;

        this.tableName = "subscribers";
        this.subscribersDatabase = new H2ConnectionPool(5, path);
        initDB();
        this.queryBaseString = "SELECT CL,S FROM " + tableName + " WHERE C = ";
    }

    private void initDB() {
        try {
            Statement statement =
                    subscribersDatabase.getDataSource().getConnection().createStatement();
            try {
                statement.execute("DROP TABLE " + tableName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            statement.execute(
                    "CREATE TABLE " + tableName + "(C BIGINT , CL BIGINT)");
            statement.execute("CREATE INDEX C_INDEX ON " + tableName + " (C)");
            statement.execute(
                    "CREATE INDEX CL_INDEX ON " + tableName + " (CL)");
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public ConversationSubscribersHolder<BODY, ID> addSubscriber(
            List<Long> conversations,
            SessionImpl<BODY, ID> session,
            OperationResultListener<Boolean> callback
    ) {
        if(conversations==null || conversations.isEmpty()){
            callback.onSuccess(true);
        }
        String sql = "INSERT INTO " + tableName + " VALUES " + commaSplitString(
                conversations, session);

        doAdd(sql, callback);

        return this;
    }

    public ConversationSubscribersHolder<BODY, ID> addSubscriber(
            List<Long> conversations,
            SessionImpl<BODY, ID> session
    ) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        addSubscriber(conversations, session, asyncToSync);
        asyncToSync.getResult();
        return this;
    }


    public ConversationSubscribersHolder<BODY, ID> addSubscriber(
            long conversation,
            SessionImpl<BODY, ID> session,
            OperationResultListener<Boolean> callback
    ) {
        return doAdd(conversation, session.clientId(), callback);
    }

    public ConversationSubscribersHolder<BODY, ID> addSubscriber(
            long conversation,
            Recipient subscriber,
            OperationResultListener<Boolean> callback
    ) {
        return doAdd(conversation, subscriber.client(), callback);
    }

    public ConversationSubscribersHolder<BODY, ID> addSubscriber(
            long conversation,
            SessionImpl<BODY, ID> session
    ) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        addSubscriber(conversation, session, asyncToSync);
        asyncToSync.getResult();
        return this;
    }

    public ConversationSubscribersHolder<BODY, ID> addSubscriber(
            long conversation,
            Recipient subscriber
    ) {
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        addSubscriber(conversation, subscriber, asyncToSync);
        asyncToSync.getResult();
        return this;
    }

    public ConversationSubscribersHolder<BODY, ID> removeSubscriber(
            long conversation,
            SessionImpl<BODY, ID> session,
            OperationResultListener<Boolean> callback
    ) {
        String sql = "DELETE FROM " + tableName + " WHERE CL=" + session.clientId() + " AND S=" + session.id() + " AND C=" + conversation;
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

    public ConversationSubscribersHolder<BODY, ID> removeSubscriber(
            long conversation,
            Recipient subscriber,
            OperationResultListener<Boolean> callback
    ) {
        String sql = "DELETE FROM " + tableName + " WHERE CL=" + subscriber.client() + " AND S=" + subscriber.session() + " AND C=" + conversation;
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

    public ConversationSubscribersHolder<BODY,ID> removeSubscriber(long conversation,
                                                                   SessionImpl<BODY, ID> session){
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        removeSubscriber(conversation, session, asyncToSync);
        asyncToSync.getResult();
        return this;
    }

    public ConversationSubscribersHolder<BODY, ID> removeSubscriber(
            long conversation,
            Recipient subscriber
    ){
        AsyncToSync<Boolean> asyncToSync = SharedAsyncToSync.shared().get().refresh();
        removeSubscriber(conversation, subscriber, asyncToSync);
        asyncToSync.getResult();
        return this;
    }


    private final ConversationSubscribersHolder<BODY, ID> doAdd(long conversation,
                                                                long clientId,
                                                                OperationResultListener<Boolean> callback) {
        String insert = "INSERT INTO " + tableName + "(C , CL) VALUES (" + conversation + " , " + clientId + ")";

        doAdd(insert, callback);

        return this;
    }

    private final ConversationSubscribersHolder<BODY, ID> doAdd(String sql,
                                                                OperationResultListener<Boolean> callback) {
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


    private static String commaSplitString(Iterable<Long> iterable, Session session) {
        StringBuilder builder = new StringBuilder();
        Iterator<Long> iter = iterable.iterator();
        if (iter.hasNext()) {
            builder.append("(")
                    .append(iter.next())
                    .append(",")
                    .append(session.clientId())
                    .append(")");
        }

        while (iter.hasNext()) {
            builder.append(",").append("(")
                    .append(iter.next())
                    .append(",")
                    .append(session.clientId())
                    .append(")");
        }

        return builder.toString();
    }


    public ConversationSubscribersHolder<BODY, ID> publishMessage(
            PubMessage<BODY, ID> pubMessage
    ) {
        Assertion.ifTrue("recipient not a conversation",
                pubMessage.recipient().conversation() < 0);

        String sql = queryBaseString + pubMessage.recipient().conversation();

        subscribersDatabase.execute(sql,
                new OperationResultListener<Statement>() {
                    @Override
                    public void onSuccess(Statement result) {
                        try {
                            ResultSet set = result.getResultSet();
                            while (set.next()) {
                                long clientId = set.getLong(1);
                                clients.publishMessage(clientId, pubMessage);
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

    public ConversationSubscribersHolder<BODY, ID> sendSignal(
            Signal<BODY> signal
    ) {
        Assertion.ifTrue("recipient not a conversation",
                signal.recipient().conversation() < 0);

        String sql = queryBaseString + signal.recipient().conversation();

        subscribersDatabase.execute(sql,
                new OperationResultListener<Statement>() {
                    @Override
                    public void onSuccess(Statement result) {
                        try {
                            ResultSet set = result.getResultSet();
                            while (set.next()) {
                                long clientId = set.getLong(1);
                                clients.sendSignal(clientId, signal);
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


    public ConversationSubscribersHolder<BODY, ID> remove(SessionImpl session,
                                                          OperationResultListener<Boolean> callback) {
        String sql = "DELETE FROM " + tableName + " WHERE CL=" + session.clientId();
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


}
