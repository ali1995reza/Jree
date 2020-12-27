package jree.mongo_base;

import jree.api.PubMessage;
import jree.api.SessionContext;
import jree.util.Assertion;
import jree.api.SessionEventListener;

public class ExceptionAdaptedEventListener<T> implements SessionEventListener<T> {


    private final SessionEventListener<T> wrapped;

    public ExceptionAdaptedEventListener(SessionEventListener<T> wrapped) {
        Assertion.ifNull("listener is null" , wrapped);
        this.wrapped = wrapped;
    }


    @Override
    public void onMessagePublished(SessionContext context, PubMessage<T> message) {
        try{
            wrapped.onMessagePublished(context, message);
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void preInitialize(SessionContext context) {
        try{
            wrapped.preInitialize(context);
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onInitialized(SessionContext context) {
        try {
            wrapped.onInitialized(context);
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClosedByException(Throwable exception) {
        try{
            wrapped.onClosedByException(exception);
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }
}
