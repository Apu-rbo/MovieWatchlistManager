package com.watchlist.gui;

import com.watchlist.controller.WatchlistController;
import com.watchlist.gui.components.MovieCard;
import com.watchlist.gui.components.ResponsiveCardGrid;
import com.watchlist.gui.theme.AppTheme;
import com.watchlist.model.Genre;
import com.watchlist.model.Movie;
import com.watchlist.model.WatchStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * "All Movies" screen: search box, genre/status filters, a sort dropdown,
 * and the poster card grid itself. Filtering/sorting is done here directly
 * over {@code controller.getAllMovies()} rather than through the old
 * string-matched table filters, since cards carry real Genre/WatchStatus
 * objects instead of rendered display strings.
 */
public class LibraryPanel extends JPanel {

    private static final String ALL_GENRES = "All Genres";
    private static final String ALL_STATUSES = "All Statuses";

    private final WatchlistController controller;
    private final Consumer<Movie> onEdit;
    private final Consumer<Movie> onDelete;

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> genreFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JComboBox<String> sortCombo = new JComboBox<>(new String[]{
            "Title (A\u2013Z)", "Year (Newest)", "Year (Oldest)", "Rating (High\u2013Low)"
    });

    private final ResponsiveCardGrid cardGrid = new ResponsiveCardGrid(190, 16);
    private final JPanel emptyState = buildEmptyState();
    private final JLabel countLabel = new JLabel();
    private final JPanel gridHost = new JPanel(new BorderLayout());

    public LibraryPanel(WatchlistController controller, Consumer<Movie> onEdit,
                         Consumer<Movie> onDelete, Runnable onAddMovie) {
        this.controller = controller;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        add(buildHeader(onAddMovie), BorderLayout.NORTH);

        gridHost.setOpaque(false);
        gridHost.add(cardGrid, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(gridHost);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel buildHeader(Runnable onAddMovie) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Your Library");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);

        countLabel.setFont(AppTheme.FONT_SMALL);
        countLabel.setForeground(AppTheme.TEXT_SECONDARY);
        countLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);
        controls.setAlignmentX(LEFT_ALIGNMENT);
        controls.setBorder(new EmptyBorder(12, 0, 0, 0));

        searchField.setColumns(18);
        searchField.putClientProperty("JTextField.placeholderText", "Search your watchlist\u2026");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        genreFilter.addItem(ALL_GENRES);
        for (Genre g : Genre.values()) genreFilter.addItem(g.getDisplayName());
        genreFilter.addActionListener(e -> refresh());

        statusFilter.addItem(ALL_STATUSES);
        for (WatchStatus s : WatchStatus.values()) statusFilter.addItem(s.getDisplayName());
        statusFilter.addActionListener(e -> refresh());

        sortCombo.addActionListener(e -> refresh());

        JButton addButton = new JButton("+  Add Movie");
        addButton.setBackground(AppTheme.ACCENT);
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.setBorderPainted(false);
        addButton.setOpaque(true);
        addButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addButton.addActionListener(e -> onAddMovie.run());

        controls.add(searchField);
        controls.add(genreFilter);
        controls.add(statusFilter);
        controls.add(sortCombo);
        controls.add(addButton);

        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(countLabel);
        header.add(controls);
        return header;
    }

    /** Re-reads the controller's data, applies the current filters/sort, and repaints the grid. */
    public void refresh() {
        List<Movie> movies = controller.getAllMovies();

        String query = searchField.getText();
        if (query != null && !query.isBlank()) {
            movies = controller.search(query);
        }

        String genreChoice = (String) genreFilter.getSelectedItem();
        if (genreChoice != null && !genreChoice.equals(ALL_GENRES)) {
            for (Genre g : Genre.values()) {
                if (g.getDisplayName().equals(genreChoice)) {
                    movies = movies.stream().filter(m -> m.getGenre() == g).toList();
                    break;
                }
            }
        }

        String statusChoice = (String) statusFilter.getSelectedItem();
        if (statusChoice != null && !statusChoice.equals(ALL_STATUSES)) {
            for (WatchStatus s : WatchStatus.values()) {
                if (s.getDisplayName().equals(statusChoice)) {
                    movies = movies.stream().filter(m -> m.getStatus() == s).toList();
                    break;
                }
            }
        }

        Comparator<Movie> comparator = switch ((String) sortCombo.getSelectedItem()) {
            case "Year (Newest)" -> Comparator.comparingInt(Movie::getReleaseYear).reversed();
            case "Year (Oldest)" -> Comparator.comparingInt(Movie::getReleaseYear);
            case "Rating (High\u2013Low)" -> Comparator.comparingInt(Movie::getRating).reversed();
            default -> Comparator.comparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER);
        };
        movies = movies.stream().sorted(comparator).toList();

        countLabel.setText(movies.size() + " of " + controller.getAllMovies().size() + " movies shown");

        gridHost.removeAll();
        if (movies.isEmpty()) {
            gridHost.add(emptyState, BorderLayout.NORTH);
        } else {
            cardGrid.setCards(movies.stream()
                    .map(m -> (JComponent) new MovieCard(m, onEdit, onDelete, false))
                    .toList());
            gridHost.add(cardGrid, BorderLayout.NORTH);
        }
        gridHost.revalidate();
        gridHost.repaint();
    }

    private static JPanel buildEmptyState() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(60, 0, 0, 0));

        JLabel icon = new JLabel("\uD83C\uDFA5");
        icon.setFont(icon.getFont().deriveFont(48f));
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel text = new JLabel("No movies match your filters");
        text.setFont(AppTheme.FONT_HEADER);
        text.setForeground(AppTheme.TEXT_SECONDARY);
        text.setAlignmentX(CENTER_ALIGNMENT);

        panel.add(icon);
        panel.add(Box.createVerticalStrut(10));
        panel.add(text);
        return panel;
    }
}
