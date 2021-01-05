package jree.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

public class ConverterList<F , T> implements List<T> {




    private final static class ConverterIter<F , T> implements Iterator<T>{

        private final Iterator<F> wrapped;
        private final Converter<F , T> converter;

        private ConverterIter(Iterator<F> wrapped, Converter<F, T> converter) {
            this.wrapped = wrapped;
            this.converter = converter;
        }

        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }

        @Override
        public T next() {
            return converter.convert(wrapped.next());
        }

        @Override
        public void remove() {
            wrapped.remove();
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            while (hasNext())
            {
                action.accept(next());
            }
        }
    }

    private final static class ConverterListIter<F , T> implements ListIterator<T>{

        private final ListIterator<F> wrapped;
        private final Converter<F , T> converter;

        private ConverterListIter(ListIterator<F> wrapped, Converter<F, T> converter) {
            this.wrapped = wrapped;
            this.converter = converter;
        }

        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }

        @Override
        public T next() {
            return converter.convert(wrapped.next());
        }

        @Override
        public boolean hasPrevious() {
            return wrapped.hasPrevious();
        }

        @Override
        public T previous() {
            return converter.convert(wrapped.previous());
        }

        @Override
        public int nextIndex() {
            return wrapped.nextIndex();
        }

        @Override
        public int previousIndex() {
            return wrapped.previousIndex();
        }

        @Override
        public void remove() {
            wrapped.remove();
        }

        @Override
        public void set(T t) {
            throw new IllegalStateException("ops not supported");
        }

        @Override
        public void add(T t) {
            throw new IllegalStateException("ops not supported");
        }
    }

    public ConverterList(List<F> wrapped, Converter<F, T> converter) {
        this.wrapped = wrapped;
        this.converter = converter;
    }





    private final List<F> wrapped;
    private final Converter<F , T> converter;


    @Override
    public int size() {
        return wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return wrapped.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new ConverterIter<>(wrapped.iterator() , converter);
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return null;
    }

    @Override
    public boolean add(T t) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public T get(int index) {
        return null;
    }

    @Override
    public T set(int index, T element) {
        return null;
    }

    @Override
    public void add(int index, T element) {

    }

    @Override
    public T remove(int index) {
        return null;
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }

    @Override
    public ListIterator<T> listIterator() {
        return null;
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return null;
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return null;
    }
}
