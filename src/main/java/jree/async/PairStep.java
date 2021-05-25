package jree.async;

public abstract class PairStep<T> extends Step<T, T> {

    @Override
    public final T finished(T t) {
        return t;
    }

}
