package jree.abs.objects;

import jree.api.Presence;
import jutils.assertion.Assertion;

import java.time.Instant;

public class PresenceImpl implements Presence {

    public static Presence active(long clientId) {
        Assertion.ifTrue("client id is negative", clientId < 0);
        return new PresenceImpl(clientId, State.ACTIVE, Instant.now());
    }

    public static Presence notActive(long clientId, Instant lastActiveTime) {
        Assertion.ifTrue("client id is negative", clientId < 0);
        return new PresenceImpl(clientId, State.NOT_ACTIVE, lastActiveTime);
    }

    public static Presence notExists(long clientId) {
        Assertion.ifTrue("client id is negative", clientId < 0);
        return new PresenceImpl(clientId, State.NO_EXISTS, null);
    }

    private final long clientId;
    private final State state;
    private final Instant lastActiveTime;

    private PresenceImpl(long clientId, State state, Instant lastActiveTime) {
        this.clientId = clientId;
        this.state = state;
        this.lastActiveTime = lastActiveTime;
    }

    @Override
    public long clientId() {
        return clientId;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public Instant lastActiveTime() {
        return lastActiveTime;
    }
}
