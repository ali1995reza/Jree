package jree.api;

import java.util.List;

public interface SessionManager<BODY, ID> {

    void createClient(long id, OperationResultListener<Boolean> callback);

    boolean createClient(long id);

    void createClient(OperationResultListener<Long> callback);

    long createClient();

    void createClientIfNotExists(long id, OperationResultListener<Boolean> callback);

    boolean createClientIfNotExists(long id);

    void removeClient(long id, OperationResultListener<Boolean> callback);

    boolean removeClient(long id);

    void createSession(long clientId, OperationResultListener<Long> callback);

    long createSession(long clientId);

    void openSession(long clientId, long sessionId, RelationController controller, SessionEventListener<BODY, ID> eventListener, OperationResultListener<Session<BODY, ID>> callback);

    Session<BODY, ID> openSession(long clientId, long sessionId, RelationController controller, SessionEventListener<BODY, ID> eventListener);

    void removeSession(long clientId, long sessionId, OperationResultListener<Boolean> callback);

    boolean removeSession(long clientId, long sessionId);

    void getPresence(List<Long> ids, OperationResultListener<List<Presence>> callback);

    List<Presence> getPresence(List<Long> ids);

    void getSession(long clientId, long sessionId, OperationResultListener<Session<BODY, ID>> callback);

    Session<BODY, ID> getSession(long clientId, long sessionId);

}
