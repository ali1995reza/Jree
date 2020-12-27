package jree.api;

import java.util.List;

public interface SessionManager<T> {

    void createClient(long id, OperationResultListener<Boolean> callback);
    boolean createClient(long id);
    void createClient(OperationResultListener<Long> callback);
    long createClient();
    void createClientIfNotExists(long id, OperationResultListener<Boolean> callback);
    boolean createClientIfNotExists(long id);
    void createSession(long clientId, OperationResultListener<Long> callback);
    long createSession(long clientId);
    void connectToService(long clientId, long sessionId, SessionEventListener<T> eventListener, OperationResultListener<Session<T>> callback);
    Session<T> connectToService(long clientId, long sessionId, SessionEventListener<T> eventListener);
    void disconnectFromService(Session<T> session, OperationResultListener<Boolean> callback);
    boolean disconnectFromService(Session<T> session);
    void checkPresence(List<Long> ids, OperationResultListener<List<Presence>> callback);
    void getSession(long clientId, long sessionId, OperationResultListener<Session<T>> callback);
    Session<T> getSession(long clientId, long sessionId);

}
