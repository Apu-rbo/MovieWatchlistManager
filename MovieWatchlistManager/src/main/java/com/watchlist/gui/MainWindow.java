package com.watchlist.gui;

import com.watchlist.controller.WatchlistController;
import com.watchlist.gui.theme.AppTheme;
import com.watchlist.model.Movie;

import javax.swing.*;
import java.awt.*;

/**
 * Top-level frame. Owns navigation (sidebar + CardLayout content area) and
 * all add/edit/delete wiring; DashboardPanel and LibraryPanel are pure views
 * that receive callbacks rather than touching WatchlistController's mutating
 * methods directly, so there's exactly one place that opens MovieDialog or
 * confirms a delete.
 */
public class MainWindow extends JFrame {

    private final WatchlistController controller;

    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);

    private final Sidebar sidebar;
    private final DashboardPanel dashboardPanel;
    private final LibraryPanel libraryPanel;

    public MainWindow(WatchlistController controller) {
        super("Movie Watchlist Manager");
        this.controller = controller;

        getContentPane().setBackground(AppTheme.BG_APP);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        dashboardPanel = new DashboardPanel(controller, this::openEditDialog, this::confirmDelete,
                this::openAddDialog, this::goToLibrary);
        libraryPanel = new LibraryPanel(controller, this::openEditDialog, this::confirmDelete,
                this::openAddDialog);

        sidebar = new Sidebar(this::navigateTo, this::openAddDialog);

        contentPanel.setOpaque(false);
        contentPanel.add(dashboardPanel, View.DASHBOARD.name());
        contentPanel.add(libraryPanel, View.LIBRARY.name());

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        controller.addChangeListener(this::refreshAll);
        controller.addErrorListener(this::showError);

        setSize(1200, 760);
        setMinimumSize(new Dimension(920, 600));
        setLocationRelativeTo(null);

        refreshAll();
    }

    private void navigateTo(View view) {
        contentLayout.show(contentPanel, view.name());
        sidebar.setActive(view);
    }

    private void goToLibrary() {
        navigateTo(View.LIBRARY);
    }

    private void refreshAll() {
        dashboardPanel.refresh();
        libraryPanel.refresh();
    }

    // ---------- Add / Edit / Delete actions (shared by both screens) ----------

    private void openAddDialog() {
        MovieDialog dialog = new MovieDialog(this);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Movie m = dialog.getResult();
            if (controller.isDuplicate(m.getTitle(), m.getReleaseYear(), null) && !confirmAddDuplicate(m)) {
                return;
            }
            controller.addMovie(m.getTitle(), m.getGenres(), m.getReleaseYear(),
                    m.getRating(), m.getStatus(), m.getNotes());
        }
    }

    private void openEditDialog(Movie existing) {
        MovieDialog dialog = new MovieDialog(this, existing);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Movie m = dialog.getResult();
            controller.updateMovie(m.getId(), m.getTitle(), m.getGenres(), m.getReleaseYear(),
                    m.getRating(), m.getStatus(), m.getNotes());
        }
    }

    /** Warns (rather than blocks) when a same-title/year movie already exists. Returns true to proceed anyway. */
    private boolean confirmAddDuplicate(Movie m) {
        int choice = JOptionPane.showConfirmDialog(this,
                "\"" + m.getTitle() + "\" (" + m.getReleaseYear() + ") is already in your watchlist.\nAdd it anyway?",
                "Possible duplicate", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void confirmDelete(Movie existing) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + existing.getTitle() + "\" from your watchlist?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            controller.deleteMovie(existing.getId());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
