package com.watchlist.controller;

import com.watchlist.model.Genre;
import com.watchlist.model.Movie;
import com.watchlist.model.Watchlist;
import com.watchlist.model.WatchStatus;
import com.watchlist.repository.StorageInterface;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public Movie addMovie(String title, Set<Genre> genres, int releaseYear, int rating,
                           WatchStatus status, String notes) {
        Movie movie = new Movie(title, genres, releaseYear, rating, status, notes);
        watchlist.add(movie);
        persist();
        notifyChanged();
        return movie;
    }

    public boolean updateMovie(String id, String title, Set<Genre> genres, int releaseYear,
                                int rating, WatchStatus status, String notes) {
        // Preserve the original addedOn timestamp across an edit rather than
        // resetting it, so "Recently Added" reflects true creation order.
        Instant addedOn = watchlist.findById(id).map(Movie::getAddedOn).orElseGet(Instant::now);
        Movie updated = new Movie(id, addedOn, title, genres, releaseYear, rating, status, notes);
        boolean success = watchlist.update(updated);
        if (success) {
            persist();
            notifyChanged();
        }
        return success;
    }

    /**
     * True if a movie with the same title (case-insensitive) and release year already
     * exists. Pass excludeId (the movie's own id) when checking during an edit, so a
     * movie doesn't flag itself as a duplicate of itself.
     */
    public boolean isDuplicate(String title, int releaseYear, String excludeId) {
        if (title == null) return false;
        String normalized = title.trim().toLowerCase();
        return watchlist.getAll().stream()
                .filter(m -> excludeId == null || !m.getId().equals(excludeId))
                .anyMatch(m -> m.getTitle().toLowerCase().equals(normalized) && m.getReleaseYear() == releaseYear);
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
                .filter(m -> m.getGenres().contains(genre))
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

        // A movie with several genres counts once toward each of them, so these
        // counts can sum to more than the total movie count - that's expected.
        Map<Genre, Long> countsByGenre = all.stream()
                .flatMap(m -> m.getGenres().stream())
                .collect(Collectors.groupingBy(g -> g, () -> new EnumMap<>(Genre.class), Collectors.counting()));

        return new WatchlistStats(all.size(), watching, watched, planToWatch, dropped, averageRating, countsByGenre);
    }

    /** Returns up to {@code limit} movies, most-recently-added first, by actual addedOn timestamp. */
    public List<Movie> getRecentlyAdded(int limit) {
        return watchlist.getAll().stream()
                .sorted(Comparator.comparing(Movie::getAddedOn).reversed())
                .limit(limit)
                .toList();
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
