package jree.abs;

import java.util.List;

public class SessionDetails<ID> {

    private boolean isSessionExists;
    private ID offset;
    private List<Long> subscribeList;

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
