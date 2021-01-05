package jree.mongo_base;

import com.mongodb.internal.async.SingleResultCallback;

import java.util.function.Consumer;

public class ConditionSingleResultCallback<T> implements SingleResultCallback<T> {


    public static <T> ConditionSingleResultCallback<T> condition()
    {
        return new ConditionSingleResultCallback<>();
    }



    private final static Consumer EMPTY_LISTENER = new Consumer() {
        @Override
        public void accept(Object o) {

        }
    };


    private Object attachment;
    private Consumer<T> onSuccess = EMPTY_LISTENER;
    private Consumer<Throwable> onFail = EMPTY_LISTENER;


    public ConditionSingleResultCallback<T> ifSuccess(Consumer<T> onSuccess)
    {
        this.onSuccess = onSuccess==null?EMPTY_LISTENER:onSuccess;
        return this;
    }

    public ConditionSingleResultCallback<T> ifFail(Consumer<Throwable> onFail)
    {
        this.onFail = onFail==null?EMPTY_LISTENER:onFail;
        return this;
    }


    @Override
    public void onResult(T t, Throwable throwable) {

        if(throwable==null)
        {
            onSuccess.accept(t);
        }else {
            onFail.accept(throwable);
        }
    }
}
