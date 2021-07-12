package jree.abs.parts;

public interface Interceptor<BODY, ID> {

    MessageInterceptor<BODY, ID> messageInterceptor();

    SessionInterceptor<BODY, ID> sessionInterceptor();

    SubscribeInterceptor<BODY, ID> subscribeInterceptor();

}
