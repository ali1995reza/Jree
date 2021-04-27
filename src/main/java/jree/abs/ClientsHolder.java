package jree.abs;


import jree.abs.utils.ThreadLocalManager;
import jree.api.PubMessage;
import jree.api.Signal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

final class ClientsHolder {

    public final static class AddSessionResult {


        private boolean isAdded;
        private boolean firstSession;

        public AddSessionResult setAdded(boolean added) {
            isAdded = added;
            return this;
        }

        public AddSessionResult setFirstSession(boolean firstSession) {
            this.firstSession = firstSession;
            return this;
        }

        public boolean isFirstSession() {
            return firstSession;
        }

        public boolean isAdded() {
            return isAdded;
        }
    }

    public final static class RemoveSessionResult {

        private final boolean isRemoved;
        private final boolean isLastSession;

        private RemoveSessionResult(boolean isRemoved, boolean isLastSession) {
            this.isRemoved = isRemoved;
            this.isLastSession = isLastSession;
        }

        public boolean isRemoved() {
            return isRemoved;
        }

        public boolean isLastSession() {
            return isLastSession;
        }
    }

    private abstract static class SessionComputer<R> implements BiFunction<Long, SessionsHolder, SessionsHolder> {
        protected R result;
        protected SessionImpl session;

        public SessionComputer setSession(SessionImpl session) {
            this.session = session;
            return this;
        }

        public SessionImpl getSession() {
            return session;
        }

        public void setResult(R result) {
            this.result = result;
        }

        public R getResult() {
            return result;
        }

        public R getResult(R def) {
            if (result == null) {
                return def;
            }

            return result;
        }

        public void refresh() {
            result = null;
        }
    }

    private final class SessionRemover extends SessionComputer<RemoveSessionResult> {

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            if (sessionsHolder.removeSession(session)) {
                if (sessionsHolder.isEmpty()) {
                    result = new RemoveSessionResult(true, true);
                    return null;
                } else {
                    result = new RemoveSessionResult(true, false);
                }
            } else {
                result = new RemoveSessionResult(false, false);
            }

            return sessionsHolder;
        }
    }

    private final class SessionAdder extends SessionComputer<AddSessionResult> {

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            AddSessionResult result = new AddSessionResult();
            if (sessionsHolder == null) {
                sessionsHolder = new SessionsHolder(aLong);
                result.setFirstSession(true);
            }
            if (sessionsHolder.addNewSession(session)) {
                result.setAdded(true);
            } else {
                result.setAdded(false);
                if (result.isFirstSession()) {
                    result.setFirstSession(false);
                    sessionsHolder = null;
                }
            }
            this.result = result;
            return sessionsHolder;
        }
    }

    private final class SessionActiveChecker extends SessionComputer<Boolean> {

        private long sessionId;

        public void setSessionId(long sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            result = sessionsHolder != null && sessionsHolder.findSessionById(
                    sessionId) != null;
            return sessionsHolder;
        }
    }

    private final class MessagePublisher extends SessionComputer<Boolean> {

        private PubMessage message;

        public void setMessage(PubMessage message) {
            this.message = message;
        }

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            if (sessionsHolder == null)
                return null;
            result = sessionsHolder.publishMessage(message);
            return sessionsHolder;
        }

        @Override
        public void refresh() {
            super.refresh();
            message = null;
        }
    }

    private final class SignalSender extends SessionComputer<Boolean> {

        private Signal signal;

        public void setSignal(Signal signal) {
            this.signal = signal;
        }

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            if (sessionsHolder == null)
                return null;
            result = sessionsHolder.sendSignal(signal);
            return sessionsHolder;
        }

        @Override
        public void refresh() {
            super.refresh();
            signal = null;
        }
    }

    private final class SessionFinder extends SessionComputer<SessionImpl> {

        private long sessionId;

        public void setSessionId(long sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            if (sessionsHolder == null)
                return null;

            result = sessionsHolder.findSessionById(sessionId);

            return sessionsHolder;
        }
    }

    private final class ClientCloser implements BiFunction<Long, SessionsHolder, SessionsHolder> {

        private Boolean result;

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            if (sessionsHolder != null) {
                for (SessionImpl session : sessionsHolder.sessions()) {
                    session.close();
                }
            }
            result = true;
            return null;
        }

        public void refresh() {
            result = null;
        }

        public Boolean getResult() {
            return result;
        }

        public Boolean getResult(Boolean def) {
            if (result == null) {
                return def;
            }
            return result;
        }
    }

    private final ConcurrentHashMap<Long, SessionsHolder> holders;

    private final ThreadLocalManager threadLocalManager;

    public ClientsHolder() {
        holders = new ConcurrentHashMap<>();
        threadLocalManager = ThreadLocalManager.builder()
                .addClass(SessionRemover.class, x -> {
                    if (x == null) {
                        x = new SessionRemover();
                    }
                    x.refresh();
                    return x;
                }).addClass(SessionAdder.class, x -> {
                    if (x == null) {
                        x = new SessionAdder();
                    }
                    x.refresh();
                    return x;
                }).addClass(SessionActiveChecker.class, x -> {
                    if (x == null) {
                        x = new SessionActiveChecker();
                    }
                    x.refresh();
                    return x;
                }).addClass(MessagePublisher.class, x -> {
                    if (x == null) {
                        x = new MessagePublisher();
                    }
                    x.refresh();
                    return x;
                }).addClass(SignalSender.class, x -> {
                    if (x == null) {
                        x = new SignalSender();
                    }
                    x.refresh();
                    return x;
                }).addClass(SessionFinder.class, x -> {
                    if (x == null) {
                        x = new SessionFinder();
                    }
                    x.refresh();
                    return x;
                }).addClass(ClientCloser.class, x -> {
                    if (x == null) {
                        x = new ClientCloser();
                    }
                    x.refresh();
                    return x;
                }).build();
    }

    private SessionAdder getAdder(SessionImpl session) {
        SessionAdder sessionAdder = threadLocalManager.get(SessionAdder.class);
        sessionAdder.setSession(session);
        return sessionAdder;
    }

    private SessionRemover getRemover(SessionImpl session) {
        SessionRemover sessionRemover = threadLocalManager.get(
                SessionRemover.class);
        sessionRemover.setSession(session);
        return sessionRemover;
    }

    private SessionActiveChecker getChecker(long id) {
        SessionActiveChecker checker = threadLocalManager.get(
                SessionActiveChecker.class);
        checker.setSessionId(id);
        return checker;
    }

    private MessagePublisher getMessagePublisher(PubMessage message) {
        MessagePublisher messagePublisher = threadLocalManager.get(
                MessagePublisher.class);
        messagePublisher.setMessage(message);
        return messagePublisher;
    }

    private SignalSender getSignalSender(Signal signal) {
        SignalSender signalSender = threadLocalManager.get(SignalSender.class);
        signalSender.setSignal(signal);
        return signalSender;
    }

    private SessionFinder getSessionFinder(long sessionId) {
        SessionFinder sessionFinder = threadLocalManager.get(
                SessionFinder.class);
        sessionFinder.setSessionId(sessionId);
        return sessionFinder;
    }

    private ClientCloser getClientCloser() {
        ClientCloser clientCloser = threadLocalManager.get(ClientCloser.class);
        return clientCloser;
    }

    public AddSessionResult addNewSession(SessionImpl session) {
        SessionAdder sessionAdder = getAdder(session);
        holders.compute(session.clientId(), sessionAdder);
        return sessionAdder.getResult();
    }

    public RemoveSessionResult removeSession(SessionImpl session) {
        SessionRemover sessionRemover = getRemover(session);
        holders.computeIfPresent(session.clientId(), sessionRemover);
        return sessionRemover.getResult();
    }

    public boolean removeClientAndCloseAllSessions(long clientId) {
        ClientCloser clientCloser = getClientCloser();
        holders.computeIfPresent(clientId, clientCloser);
        return clientCloser.getResult(false);
    }

    public boolean isSessionActive(long client, long session) {
        SessionActiveChecker sessionActiveChecker = getChecker(session);
        holders.computeIfPresent(client, sessionActiveChecker);
        return sessionActiveChecker.getResult(false);
    }

    public boolean publishMessage(long client, PubMessage message) {
        MessagePublisher messagePublisher = getMessagePublisher(message);
        //so handle it please !
        //for now use map lock !
        holders.computeIfPresent(client, messagePublisher);
        return messagePublisher.getResult(false);
    }

    public boolean sendSignal(long client, Signal signal) {
        SignalSender signalSender = getSignalSender(signal);
        //so handle it please !
        //for now use map lock !
        holders.computeIfPresent(client, signalSender);
        return signalSender.getResult(false);
    }

    public SessionImpl findSessionById(long client, long sessionId) {
        SessionFinder sessionFinder = getSessionFinder(sessionId);
        holders.computeIfPresent(client, sessionFinder);
        return sessionFinder.getSession();
    }


}
