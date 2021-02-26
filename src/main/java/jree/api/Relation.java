package jree.api;

import java.util.Map;

public interface Relation {

    Map<String , String> publisherProperties();

    Map<String , String> recipientProperties();
}
