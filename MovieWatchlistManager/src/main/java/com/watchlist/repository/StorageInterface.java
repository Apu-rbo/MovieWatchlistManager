package com.watchlist.repository;

import com.watchlist.model.Movie;

import java.io.IOException;
import java.util.List;

/**
 * Decoupled data access contract. The controller depends only on this
 * interface, not on JsonStorage directly, so the persistence mechanism
 * (JSON file, database, in-memory stub for tests) can be swapped without
 * touching controller or GUI code.
 */
public interface StorageInterface {

    /** Persists the full set of movies, overwriting whatever was previously stored. */
    void save(List<Movie> movies) throws IOException;

    /** Loads the full set of movies. Returns an empty list if no data exists yet. */
    List<Movie> load() throws IOException;
}
