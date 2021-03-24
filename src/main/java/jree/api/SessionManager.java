package jree.api;

import java.util.List;

public interface SessionManager<T, ID> {

    void createClient(long id, OperationResultListener<Boolean> callback);

    boolean createClient(long id);

    void createClient(OperationResultListener<Long> callback);

    long createClient();

    void createClientIfNotExists(long id, OperationResultListener<Boolean> callback);

    boolean createClientIfNotExists(long id);

    void createSession(long clientId, OperationResultListener<Long> callback);

    long createSession(long clientId);

    void openSession(long clientId, long sessionId, RelationController controller, SessionEventListener<T, ID> eventListener, OperationResultListener<Session<T, ID>> callback);

    Session<T, ID> openSession(long clientId, long sessionId, RelationController controller, SessionEventListener<T, ID> eventListener);

    void checkPresence(List<Long> ids, OperationResultListener<List<Presence>> callback);

    void getSession(long clientId, long sessionId, OperationResultListener<Session<T, ID>> callback);

    Session<T, ID> getSession(long clientId, long sessionId);

}
