package com.watchlist.gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * A card container that reflows its column count as the surrounding
 * viewport is resized, instead of using a fixed column count or a plain
 * FlowLayout (which would leave ragged trailing gaps). Cards are expected
 * to be added via {@link #setCards(java.util.List)}, not the raw
 * {@code add} method, so the grid can rebuild its GridLayout on demand.
 */
public class ResponsiveCardGrid extends JPanel {

    private final int cardWidth;
    private final int gap;
    private java.util.List<JComponent> cards = java.util.List.of();
    private int currentColumns = -1;

    public ResponsiveCardGrid(int cardWidth, int gap) {
        this.cardWidth = cardWidth;
        this.gap = gap;
        setOpaque(false);
        setLayout(new GridLayout(0, 1, gap, gap));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayout();
            }
        });
    }

    public void setCards(java.util.List<? extends JComponent> newCards) {
        this.cards = new java.util.ArrayList<>(newCards);
        currentColumns = -1; // force a rebuild even if the column count happens to match
        relayout();
    }

    private void relayout() {
        int width = getWidth();
        int columns = Math.max(1, width / (cardWidth + gap));
        if (columns == currentColumns && getComponentCount() == cards.size()) {
            return;
        }
        currentColumns = columns;

        removeAll();
        setLayout(new GridLayout(0, columns, gap, gap));
        for (JComponent card : cards) {
            add(card);
        }
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        // Height must reflect the actual number of rows so the enclosing
        // JScrollPane's viewport sizes correctly rather than clipping.
        if (cards.isEmpty() || currentColumns <= 0) {
            return super.getPreferredSize();
        }
        int rows = (int) Math.ceil(cards.size() / (double) currentColumns);
        int cardHeight = cards.isEmpty() ? 0 : cards.get(0).getPreferredSize().height;
        int height = rows * cardHeight + Math.max(0, rows - 1) * gap;
        return new Dimension(getWidth(), height);
    }
}
