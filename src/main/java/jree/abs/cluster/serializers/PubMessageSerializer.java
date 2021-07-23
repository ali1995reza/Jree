package jree.abs.cluster.serializers;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import jree.abs.objects.PubMessageImpl;
import jree.abs.objects.PublisherImpl;
import jree.abs.objects.RecipientImpl;
import jree.api.PubMessage;
import jree.api.Publisher;
import jree.api.Recipient;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class PubMessageSerializer implements StreamSerializer<PubMessage<String, String>> {
    @Override
    public void write(@Nonnull ObjectDataOutput output, @Nonnull PubMessage<String, String> message) throws IOException {
        output.writeLong(message.publisher().client());
        output.writeLong(message.publisher().session());
        output.writeLong(message.recipient().conversation());
        output.writeLong(message.recipient().client());
        output.writeLong(message.recipient().session());
        output.writeString(message.id());
        output.writeString(message.body());
        output.writeLong(message.time().toEpochMilli());
        //todo serialize tag too !
        //maybe there is no tage ever !
    }

    @Nonnull
    @Override
    public PubMessage<String, String> read(@Nonnull ObjectDataInput input) throws IOException {
        Publisher publisher = new PublisherImpl(
                input.readLong(),
                input.readLong()
        );
        Recipient recipient = new RecipientImpl(
                input.readLong(),
                input.readLong(),
                input.readLong()
        );
        String id = input.readString();
        String body = input.readString();
        Instant time = Instant.ofEpochMilli(input.readLong());
        PubMessage<String, String> message = new PubMessageImpl<>(id, body, time, publisher, recipient);
        return message;
    }

    @Override
    public int getTypeId() {
        return 100;
    }
}
