package com.watchlist.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Collection layer that owns the set of tracked Movie items.
 * Keeps a single internal list and exposes only safe, intention-revealing
 * operations (add/update/remove/find) rather than the raw mutable list,
 * so callers can't bypass Movie's own validation by poking at the list.
 */
public class Watchlist {

    private final List<Movie> movies = new ArrayList<>();

    public void add(Movie movie) {
        movies.add(movie);
    }

    public boolean remove(String movieId) {
        return movies.removeIf(m -> m.getId().equals(movieId));
    }

    /**
     * Replaces the existing entry that shares the given movie's id.
     * Used when a MovieDialog edit is confirmed.
     */
    public boolean update(Movie updated) {
        for (int i = 0; i < movies.size(); i++) {
            if (movies.get(i).getId().equals(updated.getId())) {
                movies.set(i, updated);
                return true;
            }
        }
        return false;
    }

    public Optional<Movie> findById(String movieId) {
        return movies.stream().filter(m -> m.getId().equals(movieId)).findFirst();
    }

    /** Returns an unmodifiable view so the GUI/controller can read but never mutate directly. */
    public List<Movie> getAll() {
        return Collections.unmodifiableList(movies);
    }

    public int size() {
        return movies.size();
    }

    public boolean isEmpty() {
        return movies.isEmpty();
    }

    /** Wipes the in-memory list and reloads it from a freshly deserialized collection. */
    public void replaceAll(List<Movie> newMovies) {
        movies.clear();
        movies.addAll(newMovies);
    }
}
