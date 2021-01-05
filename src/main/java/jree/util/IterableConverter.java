package jree.util;

import java.util.Iterator;

public class IterableConverter<F , T> implements Iterable<T> {


    private final static class ConvIter<F , T> implements Iterator<T>{

        private final Iterator<F> wrapped;
        private final Converter<F , T> converter;

        private ConvIter(Iterator<F> wrapped, Converter<F, T> converter) {
            this.wrapped = wrapped;
            this.converter = converter;
        }

        private T current;

        @Override
        public boolean hasNext() {
            boolean b = false;
            if(wrapped.hasNext())
            {

                return true;
            }
            return false;
        }

        @Override
        public T next() {
            return null;
        }
    }







    @Override
    public Iterator<T> iterator() {
        return null;
    }
}
