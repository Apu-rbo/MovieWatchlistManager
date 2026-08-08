package com.watchlist.controller;

import com.watchlist.model.Genre;
import com.watchlist.model.Movie;
import com.watchlist.model.Watchlist;
import com.watchlist.model.WatchStatus;
import com.watchlist.repository.StorageInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Mediates between UI gestures (button clicks, dialog confirmations) and
 * the Watchlist model. The GUI never touches Watchlist or StorageInterface
 * directly - everything funnels through here, which keeps persistence
 * concerns and validation entirely out of the Swing code.
 *
 * Uses a lightweight observer pattern: GUI components register a listener
 * and get notified whenever the underlying data changes, so MainWindow's
 * table stays in sync without the controller needing to know Swing exists.
 */
public class WatchlistController {

    private final Watchlist watchlist = new Watchlist();
    private final StorageInterface storage;
    private final List<Runnable> changeListeners = new ArrayList<>();
    private final List<Consumer<String>> errorListeners = new ArrayList<>();

    public WatchlistController(StorageInterface storage) {
        this.storage = storage;
    }

    // ---------- Listener registration ----------

    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    public void addErrorListener(Consumer<String> listener) {
        errorListeners.add(listener);
    }

    private void notifyChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    private void notifyError(String message) {
        for (Consumer<String> listener : errorListeners) {
            listener.accept(message);
        }
    }

    // ---------- CRUD operations ----------

    public Movie addMovie(String title, Genre genre, int releaseYear, int rating,
                           WatchStatus status, String notes) {
        Movie movie = new Movie(title, genre, releaseYear, rating, status, notes);
        watchlist.add(movie);
        persist();
        notifyChanged();
        return movie;
    }

    public boolean updateMovie(String id, String title, Genre genre, int releaseYear,
                                int rating, WatchStatus status, String notes) {
        Movie updated = new Movie(id, title, genre, releaseYear, rating, status, notes);
        boolean success = watchlist.update(updated);
        if (success) {
            persist();
            notifyChanged();
        }
        return success;
    }

    public boolean deleteMovie(String id) {
        boolean removed = watchlist.remove(id);
        if (removed) {
            persist();
            notifyChanged();
        }
        return removed;
    }

    public List<Movie> getAllMovies() {
        return watchlist.getAll();
    }

    // ---------- Filtering / searching ----------

    public List<Movie> search(String query) {
        if (query == null || query.isBlank()) {
            return getAllMovies();
        }
        String lower = query.toLowerCase();
        return watchlist.getAll().stream()
                .filter(m -> m.getTitle().toLowerCase().contains(lower))
                .toList();
    }

    public List<Movie> filterByGenre(Genre genre) {
        if (genre == null) {
            return getAllMovies();
        }
        return watchlist.getAll().stream()
                .filter(m -> m.getGenre() == genre)
                .toList();
    }

    public List<Movie> filterByStatus(WatchStatus status) {
        if (status == null) {
            return getAllMovies();
        }
        return watchlist.getAll().stream()
                .filter(m -> m.getStatus() == status)
                .toList();
    }

    // ---------- Dashboard aggregation ----------

    /** Computes aggregate counts/ratings for the dashboard. Cheap enough to call on every refresh. */
    public WatchlistStats getStats() {
        List<Movie> all = watchlist.getAll();

        int watching = 0;
        int watched = 0;
        int planToWatch = 0;
        int dropped = 0;
        for (Movie m : all) {
            switch (m.getStatus()) {
                case WATCHING -> watching++;
                case WATCHED -> watched++;
                case PLAN_TO_WATCH -> planToWatch++;
                case DROPPED -> dropped++;
            }
        }

        double averageRating = all.stream()
                .filter(m -> m.getRating() > 0)
                .mapToInt(Movie::getRating)
                .average()
                .orElse(0.0);

        Map<Genre, Long> countsByGenre = all.stream()
                .collect(Collectors.groupingBy(Movie::getGenre, () -> new EnumMap<>(Genre.class), Collectors.counting()));

        return new WatchlistStats(all.size(), watching, watched, planToWatch, dropped, averageRating, countsByGenre);
    }

    /**
     * Returns up to {@code limit} movies, most-recently-added first. "Recently added"
     * is approximated by insertion order, since Movie carries no added-on timestamp.
     */
    public List<Movie> getRecentlyAdded(int limit) {
        List<Movie> all = new ArrayList<>(watchlist.getAll());
        Collections.reverse(all);
        return all.size() <= limit ? all : all.subList(0, limit);
    }

    // ---------- Persistence ----------

    /** Loads the watchlist from disk. Call once at startup. */
    public void loadFromDisk() {
        try {
            watchlist.replaceAll(storage.load());
            notifyChanged();
        } catch (IOException e) {
            notifyError("Failed to load watchlist: " + e.getMessage());
        }
    }

    private void persist() {
        try {
            storage.save(watchlist.getAll());
        } catch (IOException e) {
            notifyError("Failed to save watchlist: " + e.getMessage());
        }
    }
}
