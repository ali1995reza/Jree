package jree.util;

public interface Converter<F , T>{

    T convert(F f);
}