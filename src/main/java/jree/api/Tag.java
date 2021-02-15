package jree.api;

import java.time.Instant;

public interface Tag {

    String name();

    String value();

    Instant time();

    long client();


}
