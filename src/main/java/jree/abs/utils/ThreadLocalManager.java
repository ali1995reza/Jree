package jree.abs.utils;

import jree.util.Assertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadLocalManager {

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {

        private final static Function EMPTY_PREPARER = x->x;

        private final Map<Class, Function> map;

        private Builder() {
            this.map = new HashMap<>();
        }

        public <T> Builder addClass(Class<T> clazz, Function<T , T> preparer) {
            synchronized (map){
                Assertion.ifTrue("class "+clazz+" already exists" , map.containsKey(clazz));
                Assertion.ifNull("preparer can not be null" , preparer);
                map.put(clazz , preparer);
            }
            return this;
        }

        public Builder refresh(){
            synchronized (map){
                map.clear();
            }
            return this;
        }

        public ThreadLocalManager build(){
            synchronized (map){
                if(map.isEmpty()){
                    return new ThreadLocalManager(Collections.emptyMap());
                }else {
                    Map<Class, ThreadLocalDetails> detailsMap = new HashMap<>();
                    for(Class clazz:map.keySet()){
                        detailsMap.put(clazz , new ThreadLocalDetails(map.get(clazz)));
                    }
                    return new ThreadLocalManager(Collections.unmodifiableMap(detailsMap));
                }
            }
        }
    }


    private final static class ThreadLocalDetails<T> {

        private final Function<T , T> preparer;
        private final ThreadLocal<T> threadLocal;

        private ThreadLocalDetails(Function<T , T> preparer) {
            this.preparer = preparer;
            this.threadLocal = new ThreadLocal<>();
        }
    }


    private final Map<Class, ThreadLocalDetails> detailsMap;

    private ThreadLocalManager(Map<Class, ThreadLocalDetails> detailsMap) {
        this.detailsMap = detailsMap;
    }

    public <T> T get(Class<T> clazz){
        ThreadLocalDetails<T> details = detailsMap.get(clazz);
        T t  = details.threadLocal.get();
        T newT = details.preparer.apply(t);
        if(newT!=t){
            details.threadLocal.set(newT);
        }
        return newT;
    }
}
