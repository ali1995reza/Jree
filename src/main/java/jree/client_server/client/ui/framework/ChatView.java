package jree.client_server.client.ui.framework;

import org.checkerframework.checker.units.qual.C;

import java.awt.*;

public class ChatView {

    private final Panel panel;
    private final ScrollPane scrollPane;

    public ChatView() {
        panel = new Panel();
        panel.setLayout(new ChatViewLayoutManger());
        scrollPane = new ScrollPane();
        scrollPane.add(panel);
    }

    public void addMessage(String msg, boolean self) {
        ChatMessageView label = new ChatMessageView(msg,self);

        if(self)
            label.setBackground(Color.LIGHT_GRAY);
        else
            label.setBackground(Color.CYAN);
        label.setAlignment(Label.RIGHT);
        panel.add(label);
        scrollPane.validate();
    }

    public void clear() {
        panel.removeAll();
        scrollPane.validate();
    }

    public Component getComponent() {
        return scrollPane;
    }


    private final static class ChatMessageView extends Label {
        private final boolean self;

        private ChatMessageView(String label, boolean self) {
            super(label);
            this.self = self;
        }

    }

    private final static class ChatViewLayoutManger implements LayoutManager {

        int height = 0;

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(0,height);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0,0);
        }

        @Override
        public void layoutContainer(Container parent) {
            int parentWidth = parent.getSize().width;
            int withOffset = parentWidth/5;
            int width = parentWidth-withOffset;
            int heightOffset = 10;

            for(Component component:parent.getComponents()) {
                ChatMessageView view = (ChatMessageView) component;
                int currentHeight = view.getPreferredSize().height;
                if(view.self) {

                    view.setBounds(
                            withOffset,
                            heightOffset,
                            width,
                            currentHeight
                    );


                } else {

                    view.setBounds(
                            0,
                            heightOffset,
                            width,
                            currentHeight
                    );
                }

                heightOffset += currentHeight+10;
            }

            height = heightOffset;
        }

    }

}
