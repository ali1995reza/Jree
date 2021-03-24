package jree.abs.parts;

import jree.abs.SessionDetails;
import jree.api.OperationResultListener;
import jree.api.Recipient;
import jree.api.Relation;
import jree.api.Session;

public interface DetailsStore<ID> {

    void addClient(long client, OperationResultListener<Boolean> result);

    void addSessionToClient(long client, OperationResultListener<Long> callback);

    void isSessionExists(long client, long session, OperationResultListener<Boolean> callback);

    void isClientExists(long client, OperationResultListener<Boolean> callback);

    void getSessionDetails(long client, long session, OperationResultListener<SessionDetails<ID>> callback);

    void getSessionOffset(long client, long session, OperationResultListener<ID> callback);

    void setSessionOffset(long client, long session, ID offset, OperationResultListener<Boolean> callback);

    void addRelation(Session session, Recipient recipient, String key, String value, OperationResultListener<Boolean> callback);

    void getRelation(Session session, Recipient recipient, OperationResultListener<Relation> callback);

    void addToSubscribeList(long client , long conversation , OperationResultListener<Boolean> callback);

    void removeFromSubscribeList(long client , long conversation, OperationResultListener<Boolean> callback);

    void close();

}
