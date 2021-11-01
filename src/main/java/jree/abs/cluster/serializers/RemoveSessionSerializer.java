package jree.abs.cluster.serializers;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import jree.abs.cluster.RemoveSessionEvent;

import javax.annotation.Nonnull;
import java.io.IOException;

public class RemoveSessionSerializer implements StreamSerializer<RemoveSessionEvent> {
    @Override
    public void write(@Nonnull ObjectDataOutput out, @Nonnull RemoveSessionEvent removeSessionEvent) throws IOException {
        out.writeLong(removeSessionEvent.clientId());
        out.writeLong(removeSessionEvent.sessionId());
    }

    @Nonnull
    @Override
    public RemoveSessionEvent read(@Nonnull ObjectDataInput input) throws IOException {
        return new RemoveSessionEvent(input.readLong(), input.readLong());
    }

    @Override
    public int getTypeId() {
        return 700;
    }
}
