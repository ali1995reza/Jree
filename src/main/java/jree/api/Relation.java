package jree.api;

import java.util.Collections;
import java.util.Map;

public interface Relation {

    Relation EMPTY = new Relation() {
        @Override
        public String getAttribute(String attar) {
            return null;
        }
    };

    String getAttribute(String attar);
}
