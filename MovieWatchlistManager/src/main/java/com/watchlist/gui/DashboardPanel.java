package com.watchlist.gui;

import com.watchlist.controller.WatchlistController;
import com.watchlist.controller.WatchlistStats;
import com.watchlist.gui.components.MovieCard;
import com.watchlist.gui.components.StatBar;
import com.watchlist.gui.components.StatCard;
import com.watchlist.gui.theme.AppTheme;
import com.watchlist.model.Genre;
import com.watchlist.model.Movie;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Landing screen: aggregate stat tiles, a per-genre breakdown, and a
 * horizontally-scrolling strip of the most recently added movies. Everything
 * here is read-only summary — actual add/edit/delete happen through the
 * same callbacks LibraryPanel uses, so both screens stay in sync through
 * WatchlistController's change listener rather than duplicating logic.
 */
public class DashboardPanel extends JPanel {

    private final WatchlistController controller;
    private final Consumer<Movie> onEdit;
    private final Consumer<Movie> onDelete;

    private final StatCard totalCard = new StatCard("\uD83D\uDCFD", "Total Movies", AppTheme.ACCENT);
    private final StatCard watchingCard = new StatCard("\u25B6", "Watching", AppTheme.ACCENT_GOLD);
    private final StatCard watchedCard = new StatCard("\u2713", "Watched", AppTheme.ACCENT_TEAL);
    private final StatCard planCard = new StatCard("\uD83D\uDCCB", "Plan to Watch", new Color(0x5C6BC0));
    private final StatCard droppedCard = new StatCard("\u2715", "Dropped", new Color(0xEF5350));
    private final StatCard ratingCard = new StatCard("\u2605", "Avg. Rating", AppTheme.ACCENT_GOLD);

    private final JPanel genreBarsHost = new JPanel();
    private final JPanel recentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
    private final JLabel recentEmptyLabel = new JLabel("Add your first movie to see it here.");

    public DashboardPanel(WatchlistController controller, Consumer<Movie> onEdit,
                           Consumer<Movie> onDelete, Runnable onAddMovie, Runnable onGoToLibrary) {
        this.controller = controller;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(20));
        content.add(buildStatRow());
        content.add(Box.createVerticalStrut(28));
        content.add(buildGenreSection());
        content.add(Box.createVerticalStrut(28));
        content.add(buildRecentSection(onGoToLibrary));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Your Watchlist Dashboard");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("A quick look at everything you're tracking.");
        subtitle.setFont(AppTheme.FONT_BODY);
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        title.setAlignmentX(LEFT_ALIGNMENT);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        stack.add(title);
        stack.add(subtitle);

        header.add(stack, BorderLayout.WEST);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
        return header;
    }

    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 6, 14, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        row.add(totalCard);
        row.add(watchingCard);
        row.add(watchedCard);
        row.add(planCard);
        row.add(droppedCard);
        row.add(ratingCard);
        return row;
    }

    private JPanel buildGenreSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setAlignmentX(LEFT_ALIGNMENT);

        JLabel heading = new JLabel("By Genre");
        heading.setFont(AppTheme.FONT_HEADER);
        heading.setForeground(AppTheme.TEXT_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, 10, 0));

        genreBarsHost.setOpaque(false);
        genreBarsHost.setLayout(new BoxLayout(genreBarsHost, BoxLayout.Y_AXIS));
        genreBarsHost.setBorder(new EmptyBorder(16, 18, 16, 18));
        genreBarsHost.setBackground(AppTheme.BG_CARD);

        JPanel card = roundedCardWrapper(genreBarsHost);

        section.add(heading, BorderLayout.NORTH);
        section.add(card, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildRecentSection(Runnable onGoToLibrary) {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setAlignmentX(LEFT_ALIGNMENT);

        JPanel headingRow = new JPanel(new BorderLayout());
        headingRow.setOpaque(false);
        JLabel heading = new JLabel("Recently Added");
        heading.setFont(AppTheme.FONT_HEADER);
        heading.setForeground(AppTheme.TEXT_PRIMARY);

        JButton viewAll = new JButton("View Library \u2192");
        viewAll.setContentAreaFilled(false);
        viewAll.setBorderPainted(false);
        viewAll.setFocusPainted(false);
        viewAll.setForeground(AppTheme.ACCENT);
        viewAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewAll.addActionListener(e -> onGoToLibrary.run());

        headingRow.add(heading, BorderLayout.WEST);
        headingRow.add(viewAll, BorderLayout.EAST);
        headingRow.setBorder(new EmptyBorder(0, 0, 10, 0));

        recentRow.setOpaque(false);
        recentEmptyLabel.setFont(AppTheme.FONT_BODY);
        recentEmptyLabel.setForeground(AppTheme.TEXT_SECONDARY);

        JScrollPane recentScroll = new JScrollPane(recentRow,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        recentScroll.setBorder(BorderFactory.createEmptyBorder());
        recentScroll.getViewport().setOpaque(false);
        recentScroll.setOpaque(false);
        recentScroll.setPreferredSize(new Dimension(10, 250));

        section.add(headingRow, BorderLayout.NORTH);
        section.add(recentScroll, BorderLayout.CENTER);
        return section;
    }

    private JPanel roundedCardWrapper(JComponent inner) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(inner, BorderLayout.CENTER);
        return wrapper;
    }

    /** Re-pulls stats/recent movies from the controller and repaints everything. */
    public void refresh() {
        WatchlistStats stats = controller.getStats();

        totalCard.setValue(stats.total());
        watchingCard.setValue(stats.watching());
        watchedCard.setValue(stats.watched());
        planCard.setValue(stats.planToWatch());
        droppedCard.setValue(stats.dropped());
        ratingCard.setValueText(stats.averageRating() > 0
                ? String.format("%.1f", stats.averageRating())
                : "\u2013");

        refreshGenreBars(stats.countsByGenre());
        refreshRecent();
    }

    private void refreshGenreBars(Map<Genre, Long> countsByGenre) {
        genreBarsHost.removeAll();

        long max = countsByGenre.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (max == 0) {
            JLabel empty = new JLabel("No movies yet \u2014 add one to see the breakdown.");
            empty.setFont(AppTheme.FONT_BODY);
            empty.setForeground(AppTheme.TEXT_SECONDARY);
            genreBarsHost.add(empty);
        } else {
            List<Map.Entry<Genre, Long>> sorted = countsByGenre.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(Map.Entry.<Genre, Long>comparingByValue().reversed())
                    .toList();
            for (Map.Entry<Genre, Long> entry : sorted) {
                double fraction = entry.getValue() / (double) max;
                StatBar bar = new StatBar(entry.getKey().getDisplayName(), entry.getValue(),
                        fraction, AppTheme.genreColor(entry.getKey()));
                bar.setAlignmentX(LEFT_ALIGNMENT);
                genreBarsHost.add(bar);
                genreBarsHost.add(Box.createVerticalStrut(6));
            }
        }

        genreBarsHost.revalidate();
        genreBarsHost.repaint();
    }

    private void refreshRecent() {
        recentRow.removeAll();
        List<Movie> recent = controller.getRecentlyAdded(8);
        if (recent.isEmpty()) {
            recentRow.add(recentEmptyLabel);
        } else {
            for (Movie movie : recent) {
                recentRow.add(new MovieCard(movie, onEdit, onDelete, true));
            }
        }
        recentRow.revalidate();
        recentRow.repaint();
    }
}
