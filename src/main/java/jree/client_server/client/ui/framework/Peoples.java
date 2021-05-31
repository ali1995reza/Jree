package jree.client_server.client.ui.framework;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Peoples {

    private final Map<Object, PeopleView> peopleViewMap = new HashMap<>();
    private final Panel panel = new Panel();
    private final ScrollPane scrollPane = new ScrollPane();
    private Consumer<PeopleView> peopleChangeEventListener = (p)->{};

    public Peoples() {
        panel.setLayout(new VerticalLayoutManager(150, 5));
        scrollPane.add(panel);
    }

    @Deprecated
    public void addNewPeople(Object key, PeopleView view) {
        peopleViewMap.put(key, view);
    }

    public PeopleView getPeople(Object key) {
        return peopleViewMap.computeIfAbsent(key, (k)->
        {
            PeopleView view = createNewPeopleView(k);
            panel.add(view.getComponent());
            scrollPane.validate();
            return view;
        });
    }

    public Peoples updateLastMessage(Object key, String message) {
        PeopleView v = peopleViewMap.computeIfAbsent(key, this::createNewPeopleView);
        panel.remove(v.getComponent());
        v.setLastMessage(message);
        panel.add(v.getComponent(), 0);
        scrollPane.validate();
        return this;
    }

    public Component getComponent() {
        return scrollPane;
    }


    public void setPeopleChangeEventListener(Consumer<PeopleView> peopleChangeEventListener) {
        this.peopleChangeEventListener = peopleChangeEventListener;
    }

    private PeopleView createNewPeopleView(Object key) {
        PeopleView view = new PeopleView();
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                peopleChangeEventListener.accept(view);
            }
        });
        return view;
    }
}
