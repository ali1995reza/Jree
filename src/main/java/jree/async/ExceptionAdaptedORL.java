package jree.async;

import jree.abs.codes.FailReasonsCodes;
import jree.api.FailReason;
import jree.api.OperationResultListener;

import java.util.function.Function;

public class ExceptionAdaptedORL<T> implements OperationResultListener<T> {

    private final static Function<Throwable, Boolean> ALWAYS_FAIL = new Function<Throwable, Boolean>() {
        @Override
        public Boolean apply(Throwable throwable) {
            return false;
        }
    };

    private final OperationResultListener<T> wrapped;
    private final Function<Throwable, Boolean> unhandledExceptionHandler;

    public ExceptionAdaptedORL(OperationResultListener<T> wrapped, Function<Throwable, Boolean> unhandledExceptionHandler) {
        this.wrapped = wrapped;
        this.unhandledExceptionHandler = unhandledExceptionHandler;
    }

    public ExceptionAdaptedORL(OperationResultListener<T> wrapped) {
        this(wrapped, ALWAYS_FAIL);
    }

    @Override
    public void onSuccess(T result) {
        try{

        }catch (Throwable e){
            Boolean handled = unhandledExceptionHandler.apply(e);
            if(handled!=null && handled) {

            } else {

            }
            try {
                wrapped.onFailed(new FailReason(e, FailReasonsCodes.RUNTIME_EXCEPTION));
            }catch (Throwable ex){
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onFailed(FailReason reason) {
    }

}
