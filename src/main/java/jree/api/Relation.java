package jree.api;

import java.util.Collections;
import java.util.Map;

public interface Relation {

    Relation EMPTY = new Relation() {
        @Override
        public Map<String, String> publisherProperties() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> recipientProperties() {
            return Collections.emptyMap();
        }
    };


    Map<String , String> publisherProperties();

    Map<String , String> recipientProperties();
}
