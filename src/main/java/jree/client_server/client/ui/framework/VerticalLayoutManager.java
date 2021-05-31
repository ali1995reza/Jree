package jree.client_server.client.ui.framework;

import java.awt.*;

public class VerticalLayoutManager implements LayoutManager {

    private final int rowHeight;
    private final int gap;

    public VerticalLayoutManager(int rowHeight, int gap) {
        this.rowHeight = rowHeight;
        this.gap = gap;
    }

    public VerticalLayoutManager(int rowHeight) {
        this(rowHeight,0);
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(parent.getSize().width, parent.getComponentCount()*rowHeight+gap);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return parent.getMinimumSize();
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            int height = gap;
            for (Component component : parent.getComponents()) {
                component.setBounds(10, height, parent.getWidth()-20, rowHeight);
                height += rowHeight+gap;
            }
        }
    }

}
