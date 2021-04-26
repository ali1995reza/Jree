package jree.abs.utils;

public class ObjectProvider {

    private final Object[] objects;

    public ObjectProvider(int size) {
        objects = new Object[size];
        initObjects();
    }

    private void initObjects() {
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new Object();
        }
    }

    public Object getObject(long id) {
        int index = (int) (id % objects.length);
        return objects[index];
    }

    public Object getObject(String s) {
        int hash = s.hashCode();
        int index = hash % objects.length;
        return objects[index];
    }
}
