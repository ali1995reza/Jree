package jree.abs;

import java.util.List;

public class SessionDetails<ID> {

    private boolean isSessionExists;
    private ID offset;
    private List<Long> subscribeList;
    private final long clientId;
    private final long sessionId;

    public SessionDetails(long clientId, long sessionId) {
        this.clientId = clientId;
        this.sessionId = sessionId;
    }

    public long clientId() {
        return clientId;
    }

    public long sessionId() {
        return sessionId;
    }

    public SessionDetails<ID> setOffset(ID offset) {
        this.offset = offset;
        return this;
    }

    public SessionDetails<ID> setSessionExists(boolean sessionExists) {
        isSessionExists = sessionExists;
        return this;
    }

    public SessionDetails<ID> setSubscribeList(List<Long> subscribeList) {
        this.subscribeList = subscribeList;
        return this;
    }

    public ID offset() {
        return offset;
    }

    public List<Long> subscribeList() {
        return subscribeList;
    }

    public boolean isSessionExists() {
        return isSessionExists;
    }

}
