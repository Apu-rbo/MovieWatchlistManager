package com.watchlist.gui;

import com.watchlist.gui.theme.AppTheme;
import com.watchlist.model.Genre;
import com.watchlist.model.Movie;
import com.watchlist.model.WatchStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.Year;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Transactional modal form for add/edit operations. "Transactional" here
 * means: nothing about the underlying Movie is touched unless the user
 * confirms with OK, and even then the caller (MainWindow) is the one that
 * actually pushes the result into the controller. Cancel, or closing the
 * dialog, discards everything typed with no side effects.
 */
public class MovieDialog extends JDialog {

    private final JTextField titleField = new JTextField(24);
    private final JList<Genre> genreList = new JList<>(Genre.values());
    private final JSpinner yearSpinner;
    private final JComboBox<WatchStatus> statusCombo = new JComboBox<>(WatchStatus.values());
    private final JTextArea notesArea = new JTextArea(4, 24);
    private final StarPicker starPicker = new StarPicker();

    private final String existingId;      // null when adding a brand-new movie
    private final Instant existingAddedOn; // null when adding a brand-new movie
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

        int currentYear = Year.now().getValue();
        yearSpinner = new JSpinner(new SpinnerNumberModel(currentYear, 1888, currentYear + 5, 1));
        // Prevent thousands-separator grouping like "2,026" in the spinner.
        ((JSpinner.NumberEditor) yearSpinner.getEditor()).getFormat().setGroupingUsed(false);

        genreList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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

        setLayout(new BorderLayout(10, 10));
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        getRootPane().setDefaultButton(null);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JScrollPane genreScroll = new JScrollPane(genreList);
        genreScroll.setPreferredSize(new Dimension(160, 110));

        int row = 0;
        addRow(panel, gbc, row++, "Title:", titleField);
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
            result = (existingId == null)
                    ? new Movie(titleField.getText(), genres, year, starPicker.getRating(), status, notesArea.getText())
                    : new Movie(existingId, existingAddedOn, titleField.getText(), genres, year,
                            starPicker.getRating(), status, notesArea.getText());

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
                JLabel star = new JLabel("☆");
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
                stars[i].setText(i < rating ? "★" : "☆");
            }
        }

        int getRating() {
            return rating;
        }
    }
}
