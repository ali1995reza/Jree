package jree.client_server.client.ui.framework;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatViewWithEditText {

    private final Panel panel;
    private final ChatView chatView;
    private final TextField textField;
    private final Button sendButton;

    private Object attachment;
    private BiConsumer<String,Object> sendCallback;

    public ChatViewWithEditText() {
        this.chatView = new ChatView();
        this.textField = new TextField();
        this.sendButton = new Button("Send");
        this.textField.setEditable(true);
        panel = new Panel();
        panel.setLayout(new LayoutManager() {
            @Override
            public void addLayoutComponent(String name, Component comp) {
            }

            @Override
            public void removeLayoutComponent(Component comp) {
            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return parent.getSize();
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return parent.getSize();
            }

            @Override
            public void layoutContainer(Container parent) {
                Rectangle bounds = parent.getBounds();
                double viewHeight = bounds.getHeight()/10*9;
                double textHeight = bounds.getHeight()/10;

                double textWidth = bounds.getWidth()/10*9;
                double buttonWidth = bounds.getWidth()/10;

                parent.getComponent(0)
                        .setBounds(
                                0,
                                0,
                                bounds.width,
                                (int) viewHeight
                        );

                parent.getComponent(1)
                        .setBounds(
                                0,
                                (int) viewHeight,
                                (int) textWidth,
                                (int) textHeight
                        );

                parent.getComponent(2)
                        .setBounds(
                                (int) textWidth,
                                (int) viewHeight,
                                (int) buttonWidth,
                                (int) textHeight
                        );

            }
        });
        panel.add(chatView.getComponent());
        panel.add(textField);
        panel.add(sendButton);
        textField.addActionListener(e->whenSendCalled());
        sendButton.addActionListener(e -> whenSendCalled());
    }

    private void whenSendCalled() {
        if(sendCallback!=null && textField.getText()!=null && !textField.getText().isEmpty()) {
            sendCallback.accept(textField.getText(), attachment);
            textField.setText(null);
        }
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public void setSendCallback(BiConsumer<String, Object> sendCallback) {
        this.sendCallback = sendCallback;
    }

    public ChatView getChatView() {
        return chatView;
    }

    public Component getComponent() {
        return panel;
    }

}
