package com.watchlist.gui.components;

import com.watchlist.gui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;

/**
 * One row of the dashboard's "By Genre" breakdown: a label, a count, and a
 * horizontal track whose filled portion is proportional to {@code fraction}
 * (count / max-count-across-genres). Custom-painted rather than a themed
 * JProgressBar so the fill color can vary per row (one per genre).
 */
public class StatBar extends JPanel {

    private final double fraction;
    private final Color color;

    public StatBar(String label, long count, double fraction, Color color) {
        this.fraction = Math.max(0.0, Math.min(1.0, fraction));
        this.color = color;

        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setPreferredSize(new Dimension(100, 26));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(AppTheme.FONT_BODY);
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);
        nameLabel.setPreferredSize(new Dimension(110, 20));

        JLabel countLabel = new JLabel(String.valueOf(count));
        countLabel.setFont(AppTheme.FONT_SMALL);
        countLabel.setForeground(AppTheme.TEXT_SECONDARY);

        add(nameLabel, BorderLayout.WEST);
        add(countLabel, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int trackX = 118;
        int trackWidth = Math.max(0, getWidth() - 118 - 40);
        int trackY = getHeight() / 2 - 4;
        int trackHeight = 8;

        g2.setColor(AppTheme.BG_INPUT);
        g2.fillRoundRect(trackX, trackY, trackWidth, trackHeight, trackHeight, trackHeight);

        int filledWidth = (int) Math.round(trackWidth * fraction);
        if (filledWidth > 0) {
            g2.setColor(color);
            g2.fillRoundRect(trackX, trackY, filledWidth, trackHeight, trackHeight, trackHeight);
        }

        g2.dispose();
    }
}
