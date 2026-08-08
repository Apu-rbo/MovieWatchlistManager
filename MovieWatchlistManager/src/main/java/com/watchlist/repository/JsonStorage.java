package com.watchlist.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.watchlist.model.Movie;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Persists the watchlist to a single JSON file via Gson.
 *
 * Writes are atomic: data is first written to a sibling temp file and
 * only moved into place with StandardCopyOption.ATOMIC_MOVE once the
 * write succeeds. That way a crash or power loss mid-write can never
 * leave watchlist.json half-written/corrupted - the reader either sees
 * the old complete file or the new complete file, never a partial one.
 *
 * Before every save, the previous watchlist.json is copied into a
 * backups/ folder (timestamped), keeping the most recent MAX_BACKUPS
 * copies, so a bad edit or accidental mass-delete is still recoverable.
 *
 * load() also transparently migrates JSON written by older versions of
 * this app: a single "genre" field becomes a one-element "genres" array,
 * and a missing "addedOn" is filled in with the current time.
 */
public class JsonStorage implements StorageInterface {

    private static final Type MOVIE_LIST_TYPE = new TypeToken<List<Movie>>() {}.getType();
    private static final int MAX_BACKUPS = 5;
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Gson gson;
    private final Path storagePath;

    public JsonStorage(Path storagePath) {
        this.storagePath = storagePath;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>)
                        (src, type, ctx) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>)
                        (json, type, ctx) -> Instant.parse(json.getAsString()))
                .create();
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

        rotateBackup(parentDir);

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

    /** Copies the current watchlist.json into backups/ before it gets overwritten, pruning old ones. */
    private void rotateBackup(Path parentDir) throws IOException {
        if (parentDir == null || !Files.exists(storagePath)) {
            return;
        }
        Path backupDir = parentDir.resolve("backups");
        Files.createDirectories(backupDir);

        String stamp = BACKUP_STAMP.format(LocalDateTime.now());
        Path backupFile = backupDir.resolve("watchlist-" + stamp + ".json");
        Files.copy(storagePath, backupFile, StandardCopyOption.REPLACE_EXISTING);

        try (var stream = Files.list(backupDir)) {
            List<Path> backups = stream
                    .filter(p -> p.getFileName().toString().startsWith("watchlist-"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
            int excess = backups.size() - MAX_BACKUPS;
            for (int i = 0; i < excess; i++) {
                Files.deleteIfExists(backups.get(i));
            }
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

        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonArray()) {
            return new java.util.ArrayList<>();
        }
        JsonArray migrated = migrateLegacyEntries(parsed.getAsJsonArray());

        List<Movie> movies = gson.fromJson(migrated, MOVIE_LIST_TYPE);
        return movies == null ? new java.util.ArrayList<>() : movies;
    }

    /**
     * Upgrades entries written by older versions of this app in place, so an existing
     * watchlist.json keeps working after this update instead of failing to load:
     *   - a singular "genre": "ACTION" field becomes "genres": ["ACTION"]
     *   - a missing "addedOn" is filled in with the current time
     */
    private JsonArray migrateLegacyEntries(JsonArray entries) {
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("genre") && !obj.has("genres")) {
                JsonArray genresArray = new JsonArray();
                genresArray.add(obj.get("genre"));
                obj.add("genres", genresArray);
            }
            obj.remove("genre");

            if (!obj.has("addedOn") || obj.get("addedOn").isJsonNull()) {
                obj.addProperty("addedOn", Instant.now().toString());
            }
        }
        return entries;
    }
}
