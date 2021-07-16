package jree.abs.parts.interceptor;

public interface Interceptor<BODY, ID> {

    default void initialize(InterceptorContext<BODY, ID> context) {

    }

    default MessageInterceptor<BODY, ID> messageInterceptor() {
        return MessageInterceptor.EMPTY;
    }

    default SessionInterceptor<BODY, ID> sessionInterceptor() {
        return SessionInterceptor.EMPTY;
    }

    default SubscriptionInterceptor<BODY, ID> subscriptionInterceptor() {
        return SubscriptionInterceptor.EMPTY;
    }

}
