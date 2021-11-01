package jree.abs.cluster.serializers;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import jree.abs.cluster.Subscribe;

import javax.annotation.Nonnull;
import java.io.IOException;

public class SubscribeSerializer implements StreamSerializer<Subscribe> {

    @Override
    public void write(@Nonnull ObjectDataOutput output, @Nonnull Subscribe subscribe) throws IOException {
        output.writeLong(subscribe.subscriber());
        output.writeLong(subscribe.conversation());
    }

    @Nonnull
    @Override
    public Subscribe read(@Nonnull ObjectDataInput input) throws IOException {
        return new Subscribe(input.readLong(), input.readLong());
    }

    @Override
    public int getTypeId() {
        return 300;
    }

}
