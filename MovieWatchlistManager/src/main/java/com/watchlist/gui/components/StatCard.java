package com.watchlist.gui.components;

import com.watchlist.gui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Dashboard tile showing one aggregate number (e.g. "Watching: 4") with an
 * accent-colored left edge and an icon glyph. The numeric value animates
 * counting up from 0 on first paint via a Swing Timer, purely as a visual
 * touch — {@link #setValue(int)} on a later refresh jumps straight to the
 * new number without re-animating, so repeated refreshes don't feel busy.
 */
public class StatCard extends JPanel {

    private final JLabel valueLabel = new JLabel("0");
    private final Color accent;
    private int displayedValue = 0;
    private Timer countTimer;

    public StatCard(String icon, String caption, Color accent) {
        this.accent = accent;

        setLayout(new BorderLayout(4, 2));
        setBackground(AppTheme.BG_CARD);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(AppTheme.FONT_HEADER.deriveFont(18f));
        iconLabel.setForeground(accent);

        valueLabel.setFont(AppTheme.FONT_STAT_VALUE);
        valueLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(AppTheme.FONT_SMALL);
        captionLabel.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(iconLabel, BorderLayout.WEST);

        JPanel textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        captionLabel.setAlignmentX(LEFT_ALIGNMENT);
        textStack.add(valueLabel);
        textStack.add(captionLabel);

        add(top, BorderLayout.NORTH);
        add(textStack, BorderLayout.SOUTH);
    }

    /** Sets the displayed number, animating a brief count-up from the current value. */
    public void setValue(int target) {
        if (countTimer != null && countTimer.isRunning()) {
            countTimer.stop();
        }
        int start = displayedValue;
        int steps = 12;
        int delayMs = 15;
        int[] frame = {0};

        countTimer = new Timer(delayMs, e -> {
            frame[0]++;
            double progress = Math.min(1.0, frame[0] / (double) steps);
            int current = (int) Math.round(start + (target - start) * progress);
            displayedValue = current;
            valueLabel.setText(String.valueOf(current));
            if (progress >= 1.0) {
                ((Timer) e.getSource()).stop();
            }
        });
        countTimer.start();
    }

    /** Sets the displayed text directly (used for the average-rating tile, which isn't a plain integer). */
    public void setValueText(String text) {
        if (countTimer != null && countTimer.isRunning()) {
            countTimer.stop();
        }
        valueLabel.setText(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(accent);
        g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public boolean isOpaque() {
        return false;
    }
}
