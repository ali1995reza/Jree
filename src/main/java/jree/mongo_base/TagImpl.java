package jree.mongo_base;

import jree.api.Tag;

import java.time.Instant;

public class TagImpl implements Tag {

    private final String name;
    private final Instant time;
    private final long client;

    public TagImpl(String name, Instant time, long client) {
        this.name = name;
        this.time = time;
        this.client = client;
    }


    @Override
    public String name() {
        return name;
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
