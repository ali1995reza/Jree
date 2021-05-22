package jree.util;

import java.util.*;
import java.util.function.Consumer;

public class ConverterList<F, T> implements List<T> {

    private static final IllegalStateException UNMODIFIABLE_EXCEPTION = new IllegalStateException("unmodifiable list");

    private final static class ConverterIter<F, T> implements Iterator<T> {

        private final Iterator<F> wrapped;
        private final Converter<F, T> converter;

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
            while (hasNext()) {
                action.accept(next());
            }
        }

    }

    private final static class ConverterListIter<F, T> implements ListIterator<T> {

        private final ListIterator<F> wrapped;
        private final Converter<F, T> converter;

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
            throw UNMODIFIABLE_EXCEPTION;
        }

        @Override
        public void add(T t) {
            throw UNMODIFIABLE_EXCEPTION;
        }

    }

    public ConverterList(List<F> wrapped, Converter<F, T> converter) {
        this.wrapped = wrapped;
        this.converter = converter;
    }

    private final List<F> wrapped;
    private final Converter<F, T> converter;

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
        for (T t : this) {
            if (t == o || (t != null && t.equals(o))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new ConverterIter<>(wrapped.iterator(), converter);
    }

    @Override
    public Object[] toArray() {
        Object[] objects = new Object[size()];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = get(i);
        }
        return objects;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return null;
    }

    @Override
    public boolean add(T t) {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public boolean remove(Object o) {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public void clear() {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public T get(int index) {
        return converter.convert(wrapped.get(index));
    }

    @Override
    public T set(int index, T element) {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public void add(int index, T element) {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public T remove(int index) {
        throw UNMODIFIABLE_EXCEPTION;
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < size(); i++) {
            T data = get(i);
            if (o == data || (o != null && o.equals(data))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int index = -1;
        for (int i = 0; i < size(); i++) {
            T data = get(i);
            if (o == data || (o != null && o.equals(data))) {
                index = i;
            }
        }
        return index;
    }

    @Override
    public ListIterator<T> listIterator() {
        return new ConverterListIter<>(wrapped.listIterator(), converter);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new ConverterListIter<>(wrapped.listIterator(index), converter);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        List<T> list = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            list.add(get(i));
        }
        return list;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConvertedList [ ");
        for (int i = 0; i < size(); i++) {
            builder.append(get(i).toString());
            if (i == size() - 1) {
                builder.append(" ");
            } else {
                builder.append(" , ");
            }
        }
        builder.append("]");
        return builder.toString();
    }

}
