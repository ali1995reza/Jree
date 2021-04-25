package jree.api;

import java.time.Instant;

public interface Presence {

    enum State{
        ACTIVE , NO_EXISTS , NOT_ACTIVE
    }


    long clientId();

    State state();

    Instant lastActiveTime();
}
