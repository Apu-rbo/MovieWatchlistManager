package com.watchlist.gui;

import com.watchlist.gui.theme.AppTheme;
import com.watchlist.model.Genre;
import com.watchlist.model.Movie;
import com.watchlist.model.WatchStatus;
import com.watchlist.service.MovieDetails;
import com.watchlist.service.MovieSuggestion;
import com.watchlist.service.OmdbClient;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.Year;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Transactional modal form for add/edit operations. "Transactional" here
 * means: nothing about the underlying Movie is touched unless the user
 * confirms with OK, and even then the caller (MainWindow) is the one that
 * actually pushes the result into the controller. Cancel, or closing the
 * dialog, discards everything typed with no side effects.
 *
 * The title field doubles as a live search box against OMDb (requires the
 * OMDB_API_KEY environment variable - see OmdbClient). Typing debounces for
 * ~400ms, then queries in the background; picking a suggestion auto-fills
 * year, genre (best-effort mapped onto this app's Genre enum), and a real
 * poster URL. If no API key is configured, or a lookup fails, the dialog
 * just behaves like a plain manual-entry form - live search is a bonus,
 * never a requirement.
 */
public class MovieDialog extends JDialog {

    private final JTextField titleField = new JTextField(24);
    private final DefaultListModel<MovieSuggestion> suggestionModel = new DefaultListModel<>();
    private final JList<MovieSuggestion> suggestionJList = new JList<>(suggestionModel);
    private final JLabel searchStatusLabel = new JLabel(" ");
    private final Timer searchDebounce;
    private final OmdbClient omdbClient; // null when OMDB_API_KEY isn't set - live search silently disables

    private final JList<Genre> genreList = new JList<>(Genre.values());
    private final JSpinner yearSpinner;
    private final JComboBox<WatchStatus> statusCombo = new JComboBox<>(WatchStatus.values());
    private final JTextArea notesArea = new JTextArea(4, 24);
    private final StarPicker starPicker = new StarPicker();

    private final String existingId;       // null when adding a brand-new movie
    private final Instant existingAddedOn; // null when adding a brand-new movie
    private boolean suppressSearchEvents = false; // true while we set titleField.setText() programmatically
    private String selectedPosterUrl;      // poster to save with the movie, if any
    private boolean confirmed = false;
    private Movie result;

    /** Add-mode constructor. */
    public MovieDialog(Frame owner) {
        this(owner, null);
    }

    /** Edit-mode constructor; pass the movie to pre-fill the form with. */
    public MovieDialog(Frame owner, Movie existing) {
        super(owner, existing == null ? "Add Movie" : "Edit Movie", true);
        this.existingId = existing == null ? null : existing.getId();
        this.existingAddedOn = existing == null ? null : existing.getAddedOn();
        this.selectedPosterUrl = existing == null ? null : existing.getPosterUrl();

        OmdbClient client;
        try {
            client = OmdbClient.fromEnvironment();
        } catch (IllegalArgumentException e) {
            client = null;
        }
        this.omdbClient = client;
        searchStatusLabel.setFont(AppTheme.FONT_SMALL);
        searchStatusLabel.setForeground(AppTheme.TEXT_SECONDARY);
        if (omdbClient == null) {
            searchStatusLabel.setText("Set OMDB_API_KEY to enable live title search & posters.");
        }

        searchDebounce = new Timer(400, e -> runSearch());
        searchDebounce.setRepeats(false);

        suggestionJList.setVisibleRowCount(4);
        suggestionJList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            MovieSuggestion picked = suggestionJList.getSelectedValue();
            if (picked != null) {
                applySuggestion(picked);
            }
        });

        int currentYear = Year.now().getValue();
        yearSpinner = new JSpinner(new SpinnerNumberModel(currentYear, 1888, currentYear + 5, 1));
        // Prevent thousands-separator grouping like "2,026" in the spinner.
        ((JSpinner.NumberEditor) yearSpinner.getEditor()).getFormat().setGroupingUsed(false);

        genreList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        genreList.setVisibleRowCount(6);

        if (existing != null) {
            titleField.setText(existing.getTitle());
            Genre[] values = Genre.values();
            int[] selectedIndices = java.util.stream.IntStream.range(0, values.length)
                    .filter(i -> existing.getGenres().contains(values[i]))
                    .toArray();
            genreList.setSelectedIndices(selectedIndices);
            yearSpinner.setValue(existing.getReleaseYear());
            statusCombo.setSelectedItem(existing.getStatus());
            notesArea.setText(existing.getNotes());
            starPicker.setRating(existing.getRating());
        }

        // Attached after the initial prefill above, so pre-filling the title on
        // Edit doesn't itself trigger a live search.
        titleField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onTitleChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onTitleChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onTitleChanged(); }
        });

        setLayout(new BorderLayout(10, 10));
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        getRootPane().setDefaultButton(null);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void onTitleChanged() {
        if (suppressSearchEvents) {
            return;
        }
        // A manual edit means whatever poster/suggestion was previously picked
        // may no longer match, so don't silently keep saving it.
        selectedPosterUrl = null;
        searchDebounce.restart();
    }

    private void runSearch() {
        String query = titleField.getText().trim();
        suggestionModel.clear();
        if (omdbClient == null) {
            return;
        }
        if (query.length() < 2) {
            searchStatusLabel.setText(" ");
            return;
        }
        searchStatusLabel.setText("Searching\u2026");

        new SwingWorker<List<MovieSuggestion>, Void>() {
            @Override
            protected List<MovieSuggestion> doInBackground() {
                return omdbClient.search(query);
            }

            @Override
            protected void done() {
                // Guard against a slow response landing after the user kept typing.
                if (!query.equals(titleField.getText().trim())) {
                    return;
                }
                try {
                    List<MovieSuggestion> results = get();
                    suggestionModel.clear();
                    results.forEach(suggestionModel::addElement);
                    searchStatusLabel.setText(results.isEmpty()
                            ? "No matches on OMDb \u2014 you can still fill this in manually."
                            : results.size() + " match(es) found \u2014 pick one below to auto-fill.");
                } catch (Exception ex) {
                    searchStatusLabel.setText("Live search failed \u2014 you can still fill this in manually.");
                }
            }
        }.execute();
    }

    private void applySuggestion(MovieSuggestion picked) {
        suppressSearchEvents = true;
        titleField.setText(picked.title());
        suppressSearchEvents = false;

        try {
            int year = Integer.parseInt(picked.year().replaceAll("[^0-9].*$", ""));
            int currentYear = Year.now().getValue();
            year = Math.max(1888, Math.min(currentYear + 5, year));
            yearSpinner.setValue(year);
        } catch (NumberFormatException ignored) {
            // Leave whatever the spinner already had; the user can fix it manually.
        }

        selectedPosterUrl = picked.posterUrl();
        suggestionModel.clear();

        if (omdbClient == null) {
            return;
        }
        searchStatusLabel.setText("Fetching genre & poster\u2026");

        new SwingWorker<MovieDetails, Void>() {
            @Override
            protected MovieDetails doInBackground() {
                return omdbClient.fetchDetails(picked.imdbId());
            }

            @Override
            protected void done() {
                try {
                    MovieDetails details = get();
                    if (details == null) {
                        searchStatusLabel.setText("Couldn't fetch extra details \u2014 fill genre in manually.");
                        return;
                    }
                    if (details.posterUrl() != null) {
                        selectedPosterUrl = details.posterUrl();
                    }
                    Set<Genre> mapped = mapOmdbGenres(details.rawGenres());
                    Genre[] values = Genre.values();
                    int[] indices = java.util.stream.IntStream.range(0, values.length)
                            .filter(i -> mapped.contains(values[i]))
                            .toArray();
                    if (indices.length > 0) {
                        genreList.setSelectedIndices(indices);
                    }
                    searchStatusLabel.setText("Genre & poster filled in from OMDb.");
                } catch (Exception ex) {
                    searchStatusLabel.setText("Couldn't fetch extra details \u2014 fill genre in manually.");
                }
            }
        }.execute();
    }

    /**
     * Best-effort mapping from OMDb's free-text genre list (e.g. "Action,
     * Adventure, Sci-Fi") onto this app's fixed Genre enum. OMDb's vocabulary
     * is broader than ours (Crime, War, Biography, etc. have no equivalent
     * here), so unmatched tokens are simply dropped; if nothing at all
     * matches, falls back to Genre.OTHER rather than leaving the selection empty.
     */
    private static Set<Genre> mapOmdbGenres(String raw) {
        Set<Genre> mapped = EnumSet.noneOf(Genre.class);
        if (raw == null || raw.isBlank()) {
            mapped.add(Genre.OTHER);
            return mapped;
        }
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            for (Genre g : Genre.values()) {
                if (g.getDisplayName().equalsIgnoreCase(trimmed)) {
                    mapped.add(g);
                    break;
                }
            }
        }
        if (mapped.isEmpty()) {
            mapped.add(Genre.OTHER);
        }
        return mapped;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JScrollPane genreScroll = new JScrollPane(genreList);
        genreScroll.setPreferredSize(new Dimension(160, 110));

        JScrollPane suggestionScroll = new JScrollPane(suggestionJList);
        suggestionScroll.setPreferredSize(new Dimension(260, 80));

        int row = 0;
        addRow(panel, gbc, row++, "Title:", titleField);

        gbc.gridx = 1;
        gbc.gridy = row++;
        panel.add(searchStatusLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        panel.add(suggestionScroll, gbc);

        addRow(panel, gbc, row++, "Genres:", genreScroll);
        addRow(panel, gbc, row++, "Year:", yearSpinner);
        addRow(panel, gbc, row++, "Rating:", starPicker);
        addRow(panel, gbc, row++, "Status:", statusCombo);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Notes:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(notesArea), gbc);

        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }

    private JPanel buildButtonPanel() {
        JButton okButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        okButton.setBackground(AppTheme.ACCENT);
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);
        okButton.setOpaque(true);

        okButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(cancelButton);
        panel.add(okButton);
        return panel;
    }

    private void onSave() {
        try {
            Set<Genre> genres = new LinkedHashSet<>(genreList.getSelectedValuesList());
            WatchStatus status = (WatchStatus) statusCombo.getSelectedItem();
            int year = (Integer) yearSpinner.getValue();

            // Movie's constructor re-runs every validation rule (blank title, empty
            // genre set, year range, rating range) so the dialog can't drift out of
            // sync with the model's actual invariants.
            String id = existingId == null ? UUID.randomUUID().toString() : existingId;
            Instant addedOn = existingId == null ? Instant.now() : existingAddedOn;
            result = new Movie(id, addedOn, titleField.getText(), genres, year,
                    starPicker.getRating(), status, notesArea.getText(), selectedPosterUrl);

            confirmed = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Call after the dialog closes to check whether the user confirmed (vs cancelled). */
    public boolean isConfirmed() {
        return confirmed;
    }

    /** Only meaningful when isConfirmed() is true. */
    public Movie getResult() {
        return result;
    }

    /**
     * Small inline widget: five clickable star labels acting as a 0-5 rating
     * picker. Kept private to MovieDialog since nothing else needs it -
     * the *display* side of ratings (read-only, in the table) is handled
     * separately by StarRatingRenderer.
     */
    private static class StarPicker extends JPanel {
        private final JLabel[] stars = new JLabel[5];
        private int rating = 0;

        StarPicker() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
            for (int i = 0; i < stars.length; i++) {
                final int starValue = i + 1;
                JLabel star = new JLabel("\u2606");
                star.setFont(star.getFont().deriveFont(20f));
                star.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                star.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // Clicking the currently-set star again clears the rating,
                        // giving users a way back to "unrated" without a separate control.
                        setRating(rating == starValue ? 0 : starValue);
                    }
                });
                stars[i] = star;
                add(star);
            }
        }

        void setRating(int newRating) {
            this.rating = Math.max(0, Math.min(5, newRating));
            for (int i = 0; i < stars.length; i++) {
                stars[i].setText(i < rating ? "\u2605" : "\u2606");
            }
        }

        int getRating() {
            return rating;
        }
    }
}
