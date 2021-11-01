package jree.abs.cluster.serializers;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import jree.abs.cluster.RemoveClientEvent;

import javax.annotation.Nonnull;
import java.io.IOException;

public class RemoveClientSerializer implements StreamSerializer<RemoveClientEvent> {
    @Override
    public void write(@Nonnull ObjectDataOutput out, @Nonnull RemoveClientEvent removeClientEvent) throws IOException {
        out.writeLong(removeClientEvent.clientId());
    }

    @Nonnull
    @Override
    public RemoveClientEvent read(@Nonnull ObjectDataInput input) throws IOException {
        return new RemoveClientEvent(input.readLong());
    }

    @Override
    public int getTypeId() {
        return 800;
    }
}
