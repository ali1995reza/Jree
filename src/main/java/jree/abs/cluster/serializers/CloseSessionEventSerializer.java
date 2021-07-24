package jree.abs.cluster.serializers;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import jree.abs.cluster.CloseSessionEvent;
import jree.abs.cluster.OpenSessionEvent;

import javax.annotation.Nonnull;
import java.io.IOException;

public class CloseSessionEventSerializer implements StreamSerializer<CloseSessionEvent> {
    @Override
    public void write(@Nonnull ObjectDataOutput output, @Nonnull CloseSessionEvent event) throws IOException {
        output.writeLong(event.clientId());
        output.writeLong(event.sessionId());
    }

    @Nonnull
    @Override
    public CloseSessionEvent read(@Nonnull ObjectDataInput input) throws IOException {
        return new CloseSessionEvent(input.readLong(), input.readLong());
    }

    @Override
    public int getTypeId() {
        return 600;
    }
}
