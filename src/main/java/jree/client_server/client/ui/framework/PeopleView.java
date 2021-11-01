package jree.client_server.client.ui.framework;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class PeopleView {

    private final Label name;
    private final Label lastMessage;
    private final Panel panel;

    public PeopleView() {
        name = new Label();
        lastMessage = new Label();
        panel = new Panel();
        panel.setLayout(new LayoutManager() {

            private final int height = 150;
            private final int nameHeight = 50;
            private final int messageHeight = 100;

            @Override
            public void addLayoutComponent(String name, Component comp) {

            }

            @Override
            public void removeLayoutComponent(Component comp) {

            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return new Dimension(parent.getWidth(),height);
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return new Dimension(parent.getWidth(), height);
            }

            @Override
            public void layoutContainer(Container parent) {
                Dimension size = parent.getPreferredSize();

                parent.getComponent(0)
                        .setBounds(
                                0,
                                0,
                                size.width,
                                nameHeight
                        );

                parent.getComponent(1)
                        .setBounds(
                                0,
                                nameHeight,
                                size.width,
                                messageHeight
                        );
            }
        });
        panel.add(name);
        panel.add(lastMessage);
        panel.setBackground(Color.CYAN);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                panel.setBackground(Color.GRAY);
                panel.validate();
            }
        });
    }

    public PeopleView(String name, String lastMessage){
        this();
        this.name.setText(name);
        this.lastMessage.setText(lastMessage);
    }



    public PeopleView setName(String name) {
        this.name.setText(name);
        this.panel.validate();
        return this;
    }

    public PeopleView setLastMessage(String message) {
        this.lastMessage.setText(message);
        this.panel.validate();
        return this;
    }


    public Component getComponent() {
        return panel;
    }

    public void addMouseListener(MouseListener listener) {
        name.addMouseListener(listener);
        lastMessage.addMouseListener(listener);
    }
}
