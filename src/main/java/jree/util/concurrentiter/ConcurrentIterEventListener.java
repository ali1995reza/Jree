package jree.util.concurrentiter;

public interface ConcurrentIterEventListener<T> {

    ConcurrentIterEventListener EMPTY = new ConcurrentIterEventListener() {
        @Override
        public void afterAdd(ConcurrentIter iterator, IterNode added) {

        }

        @Override
        public void afterRemove(ConcurrentIter iterator, IterNode removed) {

        }
    };


    void afterAdd(ConcurrentIter<T> iterator , IterNode<T> added);
    void afterRemove(ConcurrentIter<T> iterator , IterNode<T> removed);

}
