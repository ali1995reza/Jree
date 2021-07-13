package jree.abs.interceptorexecutor;

import jree.abs.parts.*;
import jree.util.Assertion;

public class InterceptorsExecutor<BODY, ID> implements Interceptor<BODY, ID> {

    private Interceptor<BODY, ID>[] interceptors;
    private MessageInterceptor<BODY, ID> messageInterceptor;
    private SessionInterceptor<BODY, ID> sessionInterceptor;
    private SubscribeInterceptor<BODY, ID> subscribeInterceptor;

    public InterceptorsExecutor() {
        interceptors = new Interceptor[0];
        initializeInterceptors();
    }

    private void initializeInterceptors() {
        if(interceptors.length==0) {
            messageInterceptor = MessageInterceptor.EMPTY;
            sessionInterceptor = SessionInterceptor.EMPTY;
            subscribeInterceptor = SubscribeInterceptor.EMPTY;
        } else {
            messageInterceptor = new MessageInterceptorExecutor<>(interceptors);
            sessionInterceptor = SessionInterceptor.EMPTY;
            subscribeInterceptor = SubscribeInterceptor.EMPTY;
        }
    }

    private int getIndexOfInterceptor(Interceptor<BODY, ID> interceptor) {
        for (int i = 0; i < interceptors.length; i++) {
            Interceptor<BODY, ID> inter = interceptors[i];
            if(inter == interceptor || inter.equals(interceptor))
                return i;
        }
        return -1;
    }

    public synchronized void addInterceptor(Interceptor<BODY, ID> interceptor) {
        Assertion.ifNull("provided interceptor is null", interceptor);
        Assertion.ifTrue("interceptor already exists", getIndexOfInterceptor(interceptor)!=-1);
        Interceptor<BODY, ID>[] copy = new  Interceptor[interceptors.length+1];
        System.arraycopy(interceptors, 0 , copy, 0 , interceptors.length);
        copy[copy.length-1] = interceptor;
        interceptors = copy;
        initializeInterceptors();
    }

    public synchronized void removeInterceptor(Interceptor<BODY,ID> interceptor) {
        int index = getIndexOfInterceptor(interceptor);
        Assertion.ifTrue("interceptor not exists", index==-1);
        Interceptor<BODY, ID>[] copy = new  Interceptor[interceptors.length+1];
        int offset = 0;
        for(int i=0;i<interceptors.length;i++) {
            if(i==index) {
                offset = -1;
                continue;
            }
            copy[i+offset] = interceptors[i];
        }
        interceptors = copy;
        initializeInterceptors();
    }


    @Override
    public void initialize(InterceptorContext<BODY, ID> context) {
        for(Interceptor<BODY, ID> interceptor:interceptors) {
            interceptor.initialize(context);
        }
    }

    @Override
    public MessageInterceptor<BODY, ID> messageInterceptor() {
        return messageInterceptor;
    }

    @Override
    public SessionInterceptor<BODY, ID> sessionInterceptor() {
        return sessionInterceptor;
    }

    @Override
    public SubscribeInterceptor<BODY, ID> subscribeInterceptor() {
        return subscribeInterceptor;
    }
}
