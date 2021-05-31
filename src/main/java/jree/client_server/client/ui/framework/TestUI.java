package jree.client_server.client.ui.framework;

import org.checkerframework.checker.units.qual.C;
import org.eclipse.jetty.websocket.common.io.FrameFlusher;

import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

public class TestUI {

    public static void main(String[] args) {
        Frame frame = new Frame();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                System.exit(12);
            }
        });
        frame.setSize(1000,500);
        MainChatView chatView = new MainChatView();
        //frame.add(new MainChatView().getPanel());
        chatView.setEventListener(new ChatMenuEventListener() {
            @Override
            public void onMessageReadyToSend(PeopleView peopleView, String message) {
                System.out.println(message);
            }

            @Override
            public List<String> getLastMessages(PeopleView peopleView) {
                return Collections.emptyList();
            }
        });

        frame.add(chatView.getPanel());

        frame.setVisible(true);

        chatView.getPeoples().getPeople(1).setName("name").setLastMessage("message");
        chatView.getPeoples().getPeople(2).setName("name").setLastMessage("message43432324");
    }
}
