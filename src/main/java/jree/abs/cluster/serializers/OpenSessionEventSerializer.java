package jree.abs.cluster.serializers;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import jree.abs.cluster.OpenSessionEvent;

import javax.annotation.Nonnull;
import java.io.IOException;

public class OpenSessionEventSerializer implements StreamSerializer<OpenSessionEvent> {
    @Override
    public void write(@Nonnull ObjectDataOutput output, @Nonnull OpenSessionEvent openSessionEvent) throws IOException {
        output.writeLong(openSessionEvent.clientId());
        output.writeLong(openSessionEvent.sessionId());
    }

    @Nonnull
    @Override
    public OpenSessionEvent read(@Nonnull ObjectDataInput input) throws IOException {
        return new OpenSessionEvent(input.readLong(), input.readLong());
    }

    @Override
    public int getTypeId() {
        return 500;
    }
}
