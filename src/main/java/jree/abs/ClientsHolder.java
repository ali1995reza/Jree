package jree.abs;


import jree.abs.utils.ThreadLocalManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

final class ClientsHolder {

    private abstract static class SessionComputer implements BiFunction<Long, SessionsHolder, SessionsHolder> {
        protected boolean result = false;
        protected SessionImpl session;

        public SessionComputer setSession(SessionImpl session) {
            this.session = session;
            return this;
        }

        public SessionImpl getSession() {
            return session;
        }

        public void setResult(boolean result) {
            this.result = result;
        }

        public boolean getResult() {
            return result;
        }

        public void  refresh(){
            result = false;
        }
    }

    private final class SessionRemover extends SessionComputer {

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            result = sessionsHolder.removeSession(session);
            if (sessionsHolder.isEmpty()) {
                return null;
            }
            return sessionsHolder;
        }
    }

    private final class SessionAdder extends SessionComputer {

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            if (sessionsHolder == null) {
                sessionsHolder = new SessionsHolder(aLong);
            }
            result = sessionsHolder.addNewSession(session);
            return sessionsHolder;
        }
    }

    private final class SessionActiveChecker extends SessionComputer{

        private long sessionId;

        public void setSessionId(long sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public SessionsHolder apply(Long aLong, SessionsHolder sessionsHolder) {
            result = sessionsHolder!=null && sessionsHolder.findSessionById(sessionId)!=null;
            return sessionsHolder;
        }
    }

    private final ConcurrentHashMap<Long, SessionsHolder> holders;

    private final ThreadLocalManager threadLocalManager;

    public ClientsHolder() {
        holders = new ConcurrentHashMap<>();
        threadLocalManager = ThreadLocalManager.builder()
                .addClass(SessionRemover.class , x->{
                    if(x==null){
                        x = new SessionRemover();
                    }
                    x.refresh();
                    return x;
                }).addClass(SessionAdder.class , x->{
                    if(x==null){
                        x = new SessionAdder();
                    }
                    x.refresh();
                    return x;
                }).addClass(SessionActiveChecker.class , x->{
                    if(x==null){
                        x = new SessionActiveChecker();
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
        SessionRemover sessionRemover = threadLocalManager.get(SessionRemover.class);
        sessionRemover.setSession(session);
        return sessionRemover;
    }

    private SessionActiveChecker getChecker(long id){
        SessionActiveChecker checker = threadLocalManager.get(SessionActiveChecker.class);
        checker.setSessionId(id);
        return checker;
    }

    public boolean addNewSession(SessionImpl session) {
        SessionAdder sessionAdder = getAdder(session);
        holders.compute(session.clientId(), sessionAdder);
        return sessionAdder.getResult();
    }

    public boolean removeSession(SessionImpl session) {
        SessionRemover sessionRemover = getRemover(session);
        holders.computeIfPresent(session.clientId(), sessionRemover);
        return sessionRemover.getResult();
    }

    public SessionsHolder getSessionsForClient(long client) {
        return holders.get(client);
    }

    public boolean isSessionActive(long client, long session) {
        SessionActiveChecker sessionActiveChecker = getChecker(session);
        holders.computeIfPresent(client , sessionActiveChecker);
        return sessionActiveChecker.getResult();
    }
}
