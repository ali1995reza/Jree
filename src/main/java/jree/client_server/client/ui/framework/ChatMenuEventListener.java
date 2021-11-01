package jree.client_server.client.ui.framework;

import java.util.Collections;
import java.util.List;

public interface ChatMenuEventListener {

    ChatMenuEventListener EMPTY = new ChatMenuEventListener() {
        @Override
        public void onMessageReadyToSend(PeopleView peopleView, String message) {

        }

        @Override
        public List<String> getLastMessages(PeopleView peopleView) {
            return Collections.emptyList();
        }
    };

    void onMessageReadyToSend(PeopleView peopleView, String message);
    List<String> getLastMessages(PeopleView peopleView);

}
