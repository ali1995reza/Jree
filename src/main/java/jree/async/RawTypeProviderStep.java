package jree.async;

import jree.api.OperationResultListener;

import java.util.function.BiConsumer;

public abstract class RawTypeProviderStep<F, T> extends Step<F , T , T> {

    private final static class ConsumerConverter<F,T> extends RawTypeProviderStep<F,T> {

        private final BiConsumer<F, OperationResultListener<T>> consumer;

        private ConsumerConverter(BiConsumer<F, OperationResultListener<T>> consumer) {
            this.consumer = consumer;
        }


        @Override
        public void doExecute(F providedValue, OperationResultListener<T> target) {
            consumer.accept(providedValue, target);
        }
    }



    public static <F,T> Step<F, T, T> execute(BiConsumer<F, OperationResultListener<T>> execution) {
        return new ConsumerConverter<>(execution);
    }


    @Override
    protected final T finished(T result) {
        return result;
    }
}
