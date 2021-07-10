package jree.abs;

import jree.api.*;
import jree.util.Assertion;

final class ExceptionAdaptedEventListener<BODY, ID> implements SessionEventListener<BODY, ID> {


    private final SessionEventListener<BODY, ID> wrapped;

    public ExceptionAdaptedEventListener(SessionEventListener<BODY, ID> wrapped) {
        Assertion.ifNull("listener is null" , wrapped);
        this.wrapped = wrapped;
    }


    @Override
    public void onMessagePublished(SessionContext context, PubMessage<BODY, ID> message) {
        try{
            wrapped.onMessagePublished(context, message);
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onSignalReceived(SessionContext context, Signal<BODY> signal) {
        try {
            wrapped.onSignalReceived(context, signal);
        }catch (Throwable e){
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
    public void onClosing(SessionContext context) {
        try {
            wrapped.onClosing(context);
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
