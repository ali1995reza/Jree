package jree.abs.objects;

import jree.api.Tag;

import java.time.Instant;

public class TagImpl implements Tag {

    private final String name;
    private final String value;
    private final Instant time;
    private final long client;

    public TagImpl(String name, String value, Instant time, long client) {
        this.name = name;
        this.value = value;
        this.time = time;
        this.client = client;
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Instant time() {
        return time;
    }

    @Override
    public long client() {
        return client;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", time=" + time +
                ", client=" + client +
                '}';
    }
}
