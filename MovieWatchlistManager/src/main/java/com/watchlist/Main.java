package com.watchlist;

import com.watchlist.controller.WatchlistController;
import com.watchlist.gui.MainWindow;
import com.watchlist.gui.theme.AppTheme;
import com.watchlist.repository.JsonStorage;

import javax.swing.*;

/**
 * Application entry point. Responsible only for bootstrapping: install the
 * look and feel, wire the storage -> controller -> GUI chain together, and
 * hand control to Swing's event dispatch thread. No business logic lives here.
 */
public class Main {

    public static void main(String[] args) {
        // The dark theme must be installed before any Swing component is created.
        AppTheme.install();

        SwingUtilities.invokeLater(() -> {
            JsonStorage storage = new JsonStorage();
            WatchlistController controller = new WatchlistController(storage);
            controller.loadFromDisk();

            MainWindow window = new MainWindow(controller);
            window.setVisible(true);
        });
    }
}
