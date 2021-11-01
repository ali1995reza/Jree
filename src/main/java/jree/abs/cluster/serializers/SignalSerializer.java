package jree.abs.cluster.serializers;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import jree.abs.objects.PublisherImpl;
import jree.abs.objects.RecipientImpl;
import jree.abs.objects.SignalImpl;
import jree.api.Publisher;
import jree.api.Recipient;
import jree.api.Signal;

import javax.annotation.Nonnull;
import java.io.IOException;

public class SignalSerializer implements StreamSerializer<Signal<String>> {

    @Override
    public void write(@Nonnull ObjectDataOutput objectDataOutput, @Nonnull Signal<String> signal) throws IOException {
        objectDataOutput.writeLong(signal.publisher().client());
        objectDataOutput.writeLong(signal.publisher().session());
        objectDataOutput.writeLong(signal.recipient().conversation());
        objectDataOutput.writeLong(signal.recipient().client());
        objectDataOutput.writeLong(signal.recipient().session());
        objectDataOutput.writeString(signal.body());
    }

    @Nonnull
    @Override
    public Signal<String> read(@Nonnull ObjectDataInput input) throws IOException {
        Publisher publisher = new PublisherImpl(
                input.readLong(),
                input.readLong()
        );
        Recipient recipient = new RecipientImpl(
                input.readLong(),
                input.readLong(),
                input.readLong()
        );
        String body = input.readString();
        return new SignalImpl<String>(body, publisher, recipient);
    }

    @Override
    public int getTypeId() {
        return 200;
    }

}
