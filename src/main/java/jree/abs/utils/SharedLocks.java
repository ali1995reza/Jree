package jree.abs.utils;

public class SharedLocks {

    private final Object[] locks;

    public SharedLocks(int size) {
        locks = new Object[size];
        initObjects();
    }

    private void initObjects() {
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
    }

    public Object getLock(long id) {
        int index = (int) (id % locks.length);
        return locks[index];
    }

    public Object getLock(int id) {
        int index = id % locks.length;
        return locks[index];
    }

    public Object getLock(byte id) {
        int index = id % locks.length;
        return locks[index];
    }

    public Object getLock(String s) {
        int hash = s.hashCode();
        int index = hash % locks.length;
        return locks[index];
    }
}
