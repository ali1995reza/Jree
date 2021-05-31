package jree.client_server.client.ui.framework;

import java.awt.*;

public class MainChatView {

    private final ChatViewWithEditText chatViewWithEditText;
    private final Peoples peoples;
    private final Panel panel;
    private ChatMenuEventListener eventListener = ChatMenuEventListener.EMPTY;
    private PeopleView selectedPeople;

    public MainChatView() {
        chatViewWithEditText = new ChatViewWithEditText();
        peoples = new Peoples();
        panel = new Panel();
        panel.setLayout(new LayoutManager() {

            private final int height = 0;

            @Override
            public void addLayoutComponent(String name, Component comp) {

            }

            @Override
            public void removeLayoutComponent(Component comp) {

            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return new Dimension(parent.getWidth(), height);
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return new Dimension(parent.getWidth(), height);
            }

            @Override
            public void layoutContainer(Container parent) {
                int parentHeight = parent.getHeight();
                int parentWidth = parent.getWidth();

                int peopleWidth = parentWidth/4;

                parent.getComponent(0)
                        .setBounds(
                                0 ,
                                0,
                                peopleWidth,
                                parentHeight
                        );

                parent.getComponent(1)
                        .setBounds(
                                peopleWidth ,
                                0,
                                parentWidth - peopleWidth,
                                parentHeight
                        );

            }
        });

        panel.add(peoples.getComponent());
        panel.add(chatViewWithEditText.getComponent());

        chatViewWithEditText.setSendCallback((s,o)->{
            if(selectedPeople!=null) {
                eventListener.onMessageReadyToSend(selectedPeople,s);
            }
        });

        peoples.setPeopleChangeEventListener(p->this.selectedPeople = p);
    }

    public void setEventListener(ChatMenuEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public Panel getPanel() {
        return panel;
    }

    public Peoples getPeoples() {
        return peoples;
    }
}
