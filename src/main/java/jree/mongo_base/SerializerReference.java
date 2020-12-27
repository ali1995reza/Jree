package jree.mongo_base;

import jree.api.BodySerializer;
import jree.util.Assertion;

final class SerializerReference<T> implements BodySerializer<T> {


    private BodySerializer<T> ref;

    SerializerReference(BodySerializer<T> serializer) {
        setReference(serializer);
    }

    public void setReference(BodySerializer<T> serializer)
    {
        Assertion.ifNull("serializer is null" , serializer);
        this.ref = serializer;
    }

    @Override
    public byte[] serialize(T object) {
        return ref.serialize(object);
    }

    @Override
    public T deserialize(byte[] data) {
        return ref.deserialize(data);
    }
}
