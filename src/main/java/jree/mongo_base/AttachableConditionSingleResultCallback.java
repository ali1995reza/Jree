package jree.mongo_base;

import com.mongodb.internal.async.SingleResultCallback;
import jree.api.Attachable;
import jree.api.SimpleAttachable;

import java.util.function.BiConsumer;

public class AttachableConditionSingleResultCallback<T , A> implements SingleResultCallback<T> {


    public static <T , A> AttachableConditionSingleResultCallback<T , A> condition() {
        return new AttachableConditionSingleResultCallback<>();
    }

    private final static BiConsumer EMPTY_LISTENER = new BiConsumer() {
        @Override
        public void accept(Object o, Object o2) {

        }
    };


    private BiConsumer<T , A> onSuccess = EMPTY_LISTENER;
    private BiConsumer<Throwable , A> onFail = EMPTY_LISTENER;
    private A attachment;


    public AttachableConditionSingleResultCallback<T , A> ifSuccess(BiConsumer<T , A> onSuccess)
    {
        this.onSuccess = onSuccess==null?EMPTY_LISTENER:onSuccess;

        return this;
    }

    public AttachableConditionSingleResultCallback<T , A> ifFail(BiConsumer<Throwable , A> onFail)
    {
        this.onFail = onFail==null?EMPTY_LISTENER:onFail;
        return this;
    }

    public AttachableConditionSingleResultCallback<T , A> attach(A attachment)
    {
        this.attachment = attachment;
        return this;
    }

    @Override
    public void onResult(T t, Throwable throwable) {
        if(throwable==null)
        {
            onSuccess.accept(t ,attachment);
        }else {
            onFail.accept(throwable ,attachment);
        }
    }
}
