package jree.abs;

import jree.api.PubMessage;
import jree.api.SessionContext;
import jree.util.Assertion;
import jree.api.SessionEventListener;

final class ExceptionAdaptedEventListener<T , ID> implements SessionEventListener<T , ID> {


    private final SessionEventListener<T , ID> wrapped;

    public ExceptionAdaptedEventListener(SessionEventListener<T , ID> wrapped) {
        Assertion.ifNull("listener is null" , wrapped);
        this.wrapped = wrapped;
    }


    @Override
    public void onMessagePublished(SessionContext context, PubMessage<T , ID> message) {
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
    public void onClosedByException(SessionContext context ,Throwable exception) {
        try{
            wrapped.onClosedByException(context , exception);
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onCloseByCommand(SessionContext context) {
        try{
            wrapped.onCloseByCommand(context);
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }
}
