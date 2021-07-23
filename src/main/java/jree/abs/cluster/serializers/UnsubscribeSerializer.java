package jree.abs.cluster.serializers;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import jree.abs.cluster.Subscribe;
import jree.abs.cluster.Unsubscribe;

import javax.annotation.Nonnull;
import java.io.IOException;

public class UnsubscribeSerializer implements StreamSerializer<Unsubscribe> {

    @Override
    public void write(@Nonnull ObjectDataOutput output, @Nonnull Unsubscribe unsubscribe) throws IOException {
        output.writeLong(unsubscribe.subscriber());
        output.writeLong(unsubscribe.conversation());
    }

    @Nonnull
    @Override
    public Unsubscribe read(@Nonnull ObjectDataInput input) throws IOException {
        return new Unsubscribe(input.readLong(), input.readLong());
    }

    @Override
    public int getTypeId() {
        return 400;
    }

}
