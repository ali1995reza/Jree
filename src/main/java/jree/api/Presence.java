package jree.api;

public interface Presence {

    enum State{
        ACTIVE , NO_EXISTS , NOT_ACTIVE
    }


    long clientId();

    State state();

    long lastActiveTime();
}
