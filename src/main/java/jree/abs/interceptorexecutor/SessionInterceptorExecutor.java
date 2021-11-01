package jree.abs.interceptorexecutor;

import jree.abs.parts.interceptor.Interceptor;
import jree.abs.parts.interceptor.SessionInterceptor;
import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.api.Session;

final class SessionInterceptorExecutor<BODY, ID> implements SessionInterceptor<BODY, ID> {

    private final Interceptor<BODY, ID>[] interceptors;

    public SessionInterceptorExecutor(Interceptor<BODY, ID>[] interceptors) {
        this.interceptors = interceptors;
    }

    private SessionInterceptor<BODY, ID> getInterceptor(int index) {
        SessionInterceptor<BODY, ID> interceptor = interceptors[index].sessionInterceptor();
        return interceptor!=null?interceptor:SessionInterceptor.EMPTY;
    }

    @Override
    public void beforeOpenSession(long clientId, long sessionId, OperationResultListener<Void> listener) {
        getInterceptor(0).beforeOpenSession(clientId, sessionId, new BeforeOpenSessionExecutor(clientId, sessionId, listener));
    }

    @Override
    public void onSessionOpen(Session<BODY, ID> session, OperationResultListener<Void> listener) {
        getInterceptor(0).onSessionOpen(session, new OnSessionOpenExecutor(session, listener));
    }

    @Override
    public void beforeCloseSession(Session<BODY, ID> session, OperationResultListener<Void> listener) {
        getInterceptor(0).beforeCloseSession(session, new BeforeCloseSessionExecutor(session, listener));
    }

    @Override
    public void onSessionClose(Session<BODY, ID> session, OperationResultListener<Void> listener) {
        getInterceptor(0).onSessionClose(session, new OnSessionCloseExecutor(session, listener));
    }

    @Override
    public void beforeRemoveSession(long clientId, long sessionId, OperationResultListener<Void> listener) {
        getInterceptor(0).beforeRemoveSession(clientId, sessionId, new BeforeRemoveSessionExecutor(clientId, sessionId, listener));
    }

    @Override
    public void onSessionRemove(long clientId, long sessionId, OperationResultListener<Void> listener) {
        getInterceptor(0).onSessionRemove(clientId, sessionId, new OnSessionRemoveExecutor(clientId, sessionId, listener));
    }

    @Override
    public void beforeRemoveClient(long clientId, OperationResultListener<Void> listener) {
        getInterceptor(0).beforeRemoveClient(clientId, new BeforeRemoveClientExecutor(clientId, listener));
    }

    @Override
    public void onClientRemove(long clientId, OperationResultListener<Void> listener) {
        getInterceptor(0).onClientRemove(clientId, new OnClientRemoveExecutor(clientId, listener));
    }

    private final class BeforeOpenSessionExecutor implements OperationResultListener<Void> {

        private final long clientId;
        private final long sessionId;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private BeforeOpenSessionExecutor(long clientId, long sessionId, OperationResultListener<Void> listener) {
            this.clientId = clientId;
            this.sessionId = sessionId;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).beforeOpenSession(clientId, sessionId, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class OnSessionOpenExecutor implements OperationResultListener<Void> {

        private final Session<BODY, ID> session;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private OnSessionOpenExecutor(Session<BODY, ID> session, OperationResultListener<Void> listener) {
            this.session = session;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).onSessionOpen(session, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class BeforeCloseSessionExecutor implements OperationResultListener<Void> {

        private final Session<BODY, ID> session;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private BeforeCloseSessionExecutor(Session<BODY, ID> session, OperationResultListener<Void> listener) {
            this.session = session;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).beforeCloseSession(session, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class OnSessionCloseExecutor implements OperationResultListener<Void> {

        private final Session<BODY, ID> session;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private OnSessionCloseExecutor(Session<BODY, ID> session, OperationResultListener<Void> listener) {
            this.session = session;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).onSessionClose(session, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class BeforeRemoveSessionExecutor implements OperationResultListener<Void> {

        private final long clientId;
        private final long sessionId;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private BeforeRemoveSessionExecutor(long clientId, long sessionId, OperationResultListener<Void> listener) {
            this.clientId = clientId;
            this.sessionId = sessionId;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).beforeRemoveSession(clientId, sessionId, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class OnSessionRemoveExecutor implements OperationResultListener<Void> {

        private final long clientId;
        private final long sessionId;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private OnSessionRemoveExecutor(long clientId, long sessionId, OperationResultListener<Void> listener) {
            this.clientId = clientId;
            this.sessionId = sessionId;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).onSessionRemove(clientId, sessionId, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class BeforeRemoveClientExecutor implements OperationResultListener<Void> {

        private final long clientId;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private BeforeRemoveClientExecutor(long clientId, OperationResultListener<Void> listener) {
            this.clientId = clientId;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).beforeRemoveClient(clientId, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

    private final class OnClientRemoveExecutor implements OperationResultListener<Void> {

        private final long clientId;
        private final OperationResultListener<Void> listener;
        private int index = 0;

        private OnClientRemoveExecutor(long clientId, OperationResultListener<Void> listener) {
            this.clientId = clientId;
            this.listener = listener;
        }

        @Override
        public void onSuccess(Void result) {
            if(++index<interceptors.length) {
                getInterceptor(index).onClientRemove(clientId, this);
            } else {
                listener.onSuccess(result);
            }
        }

        @Override
        public void onFailed(FailReason reason) {
            listener.onFailed(reason);
        }

    }

}
