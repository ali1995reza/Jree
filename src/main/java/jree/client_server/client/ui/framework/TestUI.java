package jree.client_server.client.ui.framework;

import org.checkerframework.checker.units.qual.C;
import org.eclipse.jetty.websocket.common.io.FrameFlusher;

import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;
import java.util.function.BiConsumer;

public class TestUI {

    public static void main(String[] args) {
        Frame frame = new Frame();
        frame.setSize(1000,1000);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                System.exit(1);
            }
        });
        Random random = new Random();

        ChatViewWithEditText chatViewWithEditText = new ChatViewWithEditText();
        chatViewWithEditText.setSendCallback(new BiConsumer<String, Object>() {
            @Override
            public void accept(String s, Object o) {
                chatViewWithEditText.getChatView()
                        .addMessage(s, random.nextBoolean());
            }
        });
        frame.add(chatViewWithEditText.getComponent());
        frame.setVisible(true);
    }
}
