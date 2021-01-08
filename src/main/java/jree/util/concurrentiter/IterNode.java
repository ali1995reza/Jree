package jree.util.concurrentiter;

public final class IterNode<T> {

    private final T value;
    private IterNode<T> previous;
    private IterNode<T> next;
    final ConcurrentIter<T> iterator;
    private boolean removed = false;

    IterNode(T value,IterNode<T> previous ,  IterNode<T> next, ConcurrentIter<T> iterator) {
        this.value = value;
        this.previous = previous;
        this.next = next;
        this.iterator = iterator;
    }

    public void remove()
    {
        iterator.remove(this);
    }

    public T value() {
        return value;
    }

    public IterNode<T> setRemoved(boolean removed) {
        this.removed = removed;
        return this;
    }

    public boolean isRemoved() {
        return removed;
    }

    public IterNode<T> next()
    {
        return next;
    }

    public IterNode<T> previous() {
        return previous;
    }

    final IterNode<T> setNext(IterNode<T> next)
    {
        this.next = next;
        return this.next;
    }

    final IterNode<T> setPrevious(IterNode<T> previous) {
        this.previous = previous;
        return this.previous;
    }
}
