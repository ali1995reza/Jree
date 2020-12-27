package jree.api;

public interface BodySerializer<T> {

    byte[] serialize(T object);
    T deserialize(byte[] data);

}
