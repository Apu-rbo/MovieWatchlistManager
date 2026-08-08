package com.watchlist.gui;

import com.watchlist.gui.theme.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Fixed-width dark sidebar: app logo, nav items (Dashboard / Library), and
 * a prominent "Add Movie" action. Holds no data of its own — it just reports
 * navigation clicks upward via the {@code onNavigate} callback and updates
 * its own active-item highlight through {@link #setActive(View)}.
 */
public class Sidebar extends JPanel {

    private static final int WIDTH = 210;

    private final Map<View, NavItem> navItems = new EnumMap<>(View.class);
    private View active = View.DASHBOARD;

    public Sidebar(Consumer<View> onNavigate, Runnable onAddMovie) {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_SIDEBAR);
        setPreferredSize(new Dimension(WIDTH, 10));
        setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(20, 18, 20, 18));

        JLabel logo = new JLabel("\uD83C\uDFAC  Watchlist");
        logo.setFont(AppTheme.FONT_LOGO);
        logo.setForeground(AppTheme.TEXT_PRIMARY);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        top.add(logo);
        top.add(Box.createVerticalStrut(28));

        NavItem dashboardItem = new NavItem("\u25A4", "Dashboard", () -> onNavigate.accept(View.DASHBOARD));
        NavItem libraryItem = new NavItem("\uD83D\uDDC2", "Library", () -> onNavigate.accept(View.LIBRARY));
        navItems.put(View.DASHBOARD, dashboardItem);
        navItems.put(View.LIBRARY, libraryItem);

        dashboardItem.setAlignmentX(LEFT_ALIGNMENT);
        libraryItem.setAlignmentX(LEFT_ALIGNMENT);
        top.add(dashboardItem);
        top.add(Box.createVerticalStrut(4));
        top.add(libraryItem);

        top.add(Box.createVerticalStrut(20));

        JButton addButton = new JButton("+  Add Movie");
        addButton.setAlignmentX(LEFT_ALIGNMENT);
        addButton.setFont(AppTheme.FONT_HEADER);
        addButton.setForeground(Color.WHITE);
        addButton.setBackground(AppTheme.ACCENT);
        addButton.setFocusPainted(false);
        addButton.setBorderPainted(false);
        addButton.setOpaque(true);
        addButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        addButton.addActionListener(e -> onAddMovie.run());
        top.add(addButton);

        add(top, BorderLayout.NORTH);

        JLabel footer = new JLabel("v2.0 \u00b7 dark mode");
        footer.setFont(AppTheme.FONT_SMALL);
        footer.setForeground(AppTheme.TEXT_MUTED);
        footer.setBorder(new EmptyBorder(0, 18, 16, 0));
        add(footer, BorderLayout.SOUTH);

        setActive(View.DASHBOARD);
    }

    public void setActive(View view) {
        this.active = view;
        for (Map.Entry<View, NavItem> entry : navItems.entrySet()) {
            entry.getValue().setActive(entry.getKey() == view);
        }
    }

    /** One clickable nav row with an animated accent bar that slides in on the active item. */
    private static class NavItem extends JPanel {
        private final JLabel label;
        private boolean isActive = false;
        private float accentWidth = 0f;
        private Timer animTimer;

        NavItem(String icon, String text, Runnable onClick) {
            setOpaque(false);
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(8, 10, 8, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            label = new JLabel(icon + "   " + text);
            label.setFont(AppTheme.FONT_NAV);
            label.setForeground(AppTheme.TEXT_SECONDARY);
            add(label, BorderLayout.CENTER);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    onClick.run();
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (!isActive) {
                        label.setForeground(AppTheme.TEXT_PRIMARY);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (!isActive) {
                        label.setForeground(AppTheme.TEXT_SECONDARY);
                    }
                }
            });
        }

        void setActive(boolean active) {
            this.isActive = active;
            label.setForeground(active ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
            label.setFont(active ? AppTheme.FONT_NAV.deriveFont(Font.BOLD) : AppTheme.FONT_NAV);
            animateAccent(active ? 4f : 0f);
        }

        private void animateAccent(float target) {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            animTimer = new Timer(12, e -> {
                float delta = target - accentWidth;
                if (Math.abs(delta) < 0.3f) {
                    accentWidth = target;
                    ((Timer) e.getSource()).stop();
                } else {
                    accentWidth += delta * 0.4f;
                }
                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (accentWidth > 0.1f) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.ACCENT);
                g2.fillRoundRect(0, 4, Math.round(accentWidth), getHeight() - 8, 4, 4);
                g2.dispose();
            }
            if (isActive) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(6, 0, getWidth() - 6, getHeight(), 8, 8);
                g2.dispose();
            }
        }
    }
}
