package jree.abs.parts;

public interface Interceptor<BODY, ID> {

    void initialize(InterceptorContext<BODY, ID> context);

    MessageInterceptor<BODY, ID> messageInterceptor();

    SessionInterceptor<BODY, ID> sessionInterceptor();

    SubscribeInterceptor<BODY, ID> subscribeInterceptor();

}
