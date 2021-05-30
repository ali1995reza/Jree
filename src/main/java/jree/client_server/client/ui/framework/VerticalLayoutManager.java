package jree.client_server.client.ui.framework;

import java.awt.*;
import java.util.ArrayList;

public class VerticalLayoutManager implements LayoutManager {

    private int height = 0;
    private ArrayList<String> data = new ArrayList<>();

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(parent.getSize().width, height);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return parent.getMinimumSize();
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            int height = 0;
            for (Component component : parent.getComponents()) {
                component.setBounds(0, height, parent.getSize().width, 150);
                height += 150;
            }
            this.height = height;
        }
    }

}
