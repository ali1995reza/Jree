package jree.api;

import java.util.Collections;
import java.util.Map;

public interface Relation {

    Relation EMPTY = new Relation() {
        @Override
        public Map<String, String> setByClient(long clientId) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> setByConversation(long conversationId) {
            return Collections.emptyMap();
        }
    };


    Map<String , String> setByClient(long clientId);

    Map<String , String> setByConversation(long conversationId);
}
