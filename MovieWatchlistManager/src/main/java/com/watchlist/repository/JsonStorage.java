package com.watchlist.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.watchlist.model.Movie;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Persists the watchlist to a single JSON file via Gson.
 *
 * Writes are atomic: data is first written to a sibling temp file and
 * only moved into place with StandardCopyOption.ATOMIC_MOVE once the
 * write succeeds. That way a crash or power loss mid-write can never
 * leave watchlist.json half-written/corrupted - the reader either sees
 * the old complete file or the new complete file, never a partial one.
 */
public class JsonStorage implements StorageInterface {

    private static final Type MOVIE_LIST_TYPE = new TypeToken<List<Movie>>() {}.getType();

    private final Gson gson;
    private final Path storagePath;

    public JsonStorage(Path storagePath) {
        this.storagePath = storagePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /** Convenience constructor: stores the file at ~/.moviewatchlist/watchlist.json */
    public JsonStorage() {
        this(Path.of(System.getProperty("user.home"), ".moviewatchlist", "watchlist.json"));
    }

    @Override
    public synchronized void save(List<Movie> movies) throws IOException {
        Path parentDir = storagePath.toAbsolutePath().getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        Path tempFile = Files.createTempFile(parentDir, "watchlist", ".json.tmp");
        try {
            String json = gson.toJson(movies, MOVIE_LIST_TYPE);
            Files.writeString(tempFile, json, StandardCharsets.UTF_8);

            try {
                Files.move(tempFile, storagePath,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                // Some filesystems (notably certain network mounts) don't
                // support atomic moves. Falling back to a plain move still
                // beats leaving the temp file orphaned; it's just no
                // longer guaranteed atomic on that specific filesystem.
                Files.move(tempFile, storagePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public synchronized List<Movie> load() throws IOException {
        if (!Files.exists(storagePath)) {
            return new java.util.ArrayList<>();
        }
        String json = Files.readString(storagePath, StandardCharsets.UTF_8);
        if (json.isBlank()) {
            return new java.util.ArrayList<>();
        }
        List<Movie> movies = gson.fromJson(json, MOVIE_LIST_TYPE);
        return movies == null ? new java.util.ArrayList<>() : movies;
    }
}
