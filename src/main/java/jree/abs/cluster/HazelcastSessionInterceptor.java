package jree.abs.cluster;

import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import jree.abs.codes.FailReasonsCodes;
import jree.abs.parts.interceptor.SessionInterceptor;
import jree.abs.utils.H2ConnectionPool;
import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.api.Session;

import java.sql.SQLException;
import java.sql.Statement;

public class HazelcastSessionInterceptor implements SessionInterceptor<String, String> {

    private final H2ConnectionPool connectionPool;
    private final String tableName = "sessions";
    private final ITopic sessionEventTopic;

    public HazelcastSessionInterceptor(ITopic sessionEventTopic) {
        this.sessionEventTopic = sessionEventTopic;
        this.connectionPool = new H2ConnectionPool(
                1, "jdbc:h2:mem:db2"
        );
        initDB();
        sessionEventTopic.addMessageListener(this::handleMessage);
    }

    private void handleMessage(Message message) {
        if(message.getPublishingMember().localMember())
            return;
        Object event = message.getMessageObject();

        if(event instanceof OpenSessionEvent) {
            OpenSessionEvent openSessionEvent = (OpenSessionEvent) event;
            connectionPool.execute("INSERT INTO " + tableName + " VALUES (" + openSessionEvent.clientId() + "," + openSessionEvent.sessionId()  + ")",
                    OperationResultListener.EMPTY_LISTENER);
        } else if(event instanceof CloseSessionEvent) {
            CloseSessionEvent closeSessionEvent = (CloseSessionEvent) event;
            connectionPool.execute("DELETE FROM "+tableName+" WHERE S="+closeSessionEvent.sessionId()+" AND C="+closeSessionEvent.clientId(), OperationResultListener.EMPTY_LISTENER);
        } else if(event instanceof RemoveSessionEvent) {
            RemoveSessionEvent removeSessionEvent = (RemoveSessionEvent) event;
            connectionPool.execute("DELETE FROM "+tableName+" WHERE S="+removeSessionEvent.sessionId()+" AND C="+removeSessionEvent.clientId(), OperationResultListener.EMPTY_LISTENER);
        } else if(event instanceof RemoveClientEvent) {
            RemoveClientEvent removeClientEvent = (RemoveClientEvent) event;
            connectionPool.execute("DELETE FROM "+tableName+" WHERE C="+removeClientEvent.clientId(), OperationResultListener.EMPTY_LISTENER);
        }
    }

    private void initDB() {
        try {
            Statement statement =
                    connectionPool.getDataSource().getConnection().createStatement();
            try {
                statement.execute("DROP TABLE " + tableName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            statement.execute(
                    "CREATE TABLE " + tableName + "(C BIGINT , S BIGINT)");
            statement.execute("CREATE UNIQUE INDEX C_S_INDEX ON " + tableName + " (C,S)");
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void beforeOpenSession(long clientId, long sessionId, OperationResultListener<Void> listener) {
        connectionPool.execute("SELECT S FROM " + tableName + " WHERE S=" + sessionId, new OperationResultListener<Statement>() {
            @Override
            public void onSuccess(Statement result) {
                try {
                    if(result.getResultSet().first()) {
                        listener.onFailed(new FailReason(123));
                    } else {
                        listener.onSuccess(null);
                    }
                } catch (SQLException ex) {
                    listener.onFailed(new FailReason(ex,120));
                }
            }

            @Override
            public void onFailed(FailReason reason) {
                listener.onFailed(reason);
            }
        });
    }

    @Override
    public void onSessionOpen(Session<String, String> session, OperationResultListener<Void> listener) {
        //todo fix model !
        connectionPool.execute("INSERT INTO " + tableName + " VALUES (" + session.clientId() + "," + session.id() + ")",
                new OperationResultListener<Statement>() {
                    @Override
                    public void onSuccess(Statement result) {
                        try {
                            if(result.getUpdateCount()!=1) {
                                listener.onFailed(new FailReason(FailReasonsCodes.RUNTIME_EXCEPTION));
                            } else {
                                sessionEventTopic.publishAsync(
                                        new OpenSessionEvent(session.clientId(), session.id())
                                );
                                listener.onSuccess(null);
                            }
                        } catch (SQLException ex) {
                            listener.onFailed(new FailReason(ex,120));
                        }
                    }

                    @Override
                    public void onFailed(FailReason reason) {
                        listener.onFailed(reason);
                    }
                });
    }

    @Override
    public void onSessionClose(Session<String, String> session, OperationResultListener<Void> listener) {
        connectionPool.execute("DELETE FROM "+tableName+" WHERE S="+session.id()+" AND C="+session.clientId(), OperationResultListener.EMPTY_LISTENER);
        sessionEventTopic.publishAsync(
                new CloseSessionEvent(session.clientId(), session.id())
        );
        listener.onSuccess(null);
    }

    @Override
    public void onClientRemove(long clientId, OperationResultListener<Void> listener) {
        connectionPool.execute("DELETE FROM "+tableName+" WHERE C="+clientId, OperationResultListener.EMPTY_LISTENER);
        sessionEventTopic.publishAsync(
                new RemoveClientEvent(clientId)
        );
        listener.onSuccess(null);
    }

    @Override
    public void onSessionRemove(long clientId, long sessionId, OperationResultListener<Void> listener) {
        connectionPool.execute("DELETE FROM "+tableName+" WHERE S="+sessionId+" AND C="+clientId, OperationResultListener.EMPTY_LISTENER);
        sessionEventTopic.publishAsync(
                new RemoveSessionEvent(clientId, sessionId)
        );
        SessionInterceptor.super.onSessionRemove(clientId, sessionId, listener);
    }
}
