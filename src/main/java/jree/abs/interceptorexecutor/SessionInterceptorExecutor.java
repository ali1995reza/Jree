package jree.abs.interceptorexecutor;

import jree.abs.parts.SessionInterceptor;
import jree.api.OperationResultListener;
import jree.api.Session;

public class SessionInterceptorExecutor<BODY, ID> implements SessionInterceptor<BODY, ID> {


    @Override
    public void beforeSessionOpen(long clientId, long sessionId, OperationResultListener<Void> listener) {

    }

    @Override
    public void onSessionOpen(Session<BODY, ID> session, OperationResultListener<Void> listener) {

    }

    @Override
    public void afterSessionOpened(Session<BODY, ID> session, OperationResultListener<Void> listener) {

    }

    @Override
    public void beforeSessionClose(Session<BODY, ID> session, OperationResultListener<Void> listener) {

    }

    @Override
    public void onSessionClose(Session<BODY, ID> session, OperationResultListener<Void> listener) {

    }

    @Override
    public void afterSessionClosed(Session<BODY, ID> session, OperationResultListener<Void> listener) {

    }


}
