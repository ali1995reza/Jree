package jree.mongo_base;


final class SharedAsyncToSync {

    private final static SharedAsyncToSync shared = new SharedAsyncToSync();

    public final static SharedAsyncToSync shared() {
        return shared;
    }


    private final ThreadLocal<AsyncToSync> threadLocal = new ThreadLocal<>(
    );


    public AsyncToSync get()
    {
        AsyncToSync asyncToSync = threadLocal.get();
        if(asyncToSync==null) {
            asyncToSync = new AsyncToSync();
            threadLocal.set(asyncToSync);
        }
        return asyncToSync;
    }
}
