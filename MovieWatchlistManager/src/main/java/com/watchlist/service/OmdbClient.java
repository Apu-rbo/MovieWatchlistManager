package com.watchlist.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin client for the OMDb API (https://www.omdbapi.com) - a free, public
 * movie database. Needs an API key: get one at
 * https://www.omdbapi.com/apikey.aspx (free tier, ~1000 requests/day).
 *
 * Two calls only, matching what MovieDialog needs:
 *   - search(query): title-search-as-you-type suggestions ("s=" endpoint;
 *     returns title/year/poster but NOT genre)
 *   - fetchDetails(imdbId): full record for one title once picked ("i="
 *     endpoint; includes genre, which the search endpoint omits)
 *
 * Every call happens on whatever thread calls it - callers (MovieDialog) are
 * responsible for running these off the Swing Event Dispatch Thread, e.g.
 * via SwingWorker, so the UI never freezes waiting on the network.
 */
public class OmdbClient {

    private static final String BASE_URL = "https://www.omdbapi.com/";

    private final String apiKey;
    private final HttpClient httpClient;

    public OmdbClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "OMDb API key is required - set the OMDB_API_KEY environment variable. " +
                    "Get a free key at https://www.omdbapi.com/apikey.aspx");
        }
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    /** Reads the key from the OMDB_API_KEY environment variable. */
    public static OmdbClient fromEnvironment() {
        return new OmdbClient(System.getenv("OMDB_API_KEY"));
    }

    /**
     * Searches by (partial) title, e.g. "spider man" -> every matching movie
     * OMDb knows about. Returns an empty list on no matches or any error -
     * a search box should never throw, just show nothing.
     */
    public List<MovieSuggestion> search(String query) {
        List<MovieSuggestion> results = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return results;
        }

        String url = BASE_URL + "?apikey=" + encode(apiKey)
                + "&type=movie&s=" + encode(query.trim());

        JsonObject root = getJson(url);
        if (root == null || !"True".equalsIgnoreCase(getString(root, "Response"))) {
            return results; // includes the normal "Movie not found!" case
        }

        JsonArray search = root.getAsJsonArray("Search");
        if (search == null) {
            return results;
        }
        for (var element : search) {
            JsonObject obj = element.getAsJsonObject();
            results.add(new MovieSuggestion(
                    getString(obj, "Title"),
                    getString(obj, "Year"),
                    getString(obj, "imdbID"),
                    cleanPoster(getString(obj, "Poster"))));
        }
        return results;
    }

    /** Full lookup by IMDb id - the only call that returns genre. Returns null on failure. */
    public MovieDetails fetchDetails(String imdbId) {
        if (imdbId == null || imdbId.isBlank()) {
            return null;
        }
        String url = BASE_URL + "?apikey=" + encode(apiKey) + "&i=" + encode(imdbId);

        JsonObject root = getJson(url);
        if (root == null || !"True".equalsIgnoreCase(getString(root, "Response"))) {
            return null;
        }

        int year = parseYear(getString(root, "Year"));
        return new MovieDetails(
                getString(root, "Title"),
                year,
                getString(root, "Genre"),
                cleanPoster(getString(root, "Poster")));
    }

    private JsonObject getJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (IOException | InterruptedException | RuntimeException e) {
            // Network hiccups, malformed JSON, etc. - a failed lookup just means
            // "no suggestions this time", not a crash.
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private static String getString(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : "";
    }

    /** OMDb uses the literal string "N/A" for a missing poster; normalize that to null. */
    private static String cleanPoster(String poster) {
        return (poster == null || poster.isBlank() || poster.equals("N/A")) ? null : poster;
    }

    /** OMDb reports some entries as "2002–2003"; take just the first year. */
    private static int parseYear(String rawYear) {
        if (rawYear == null || rawYear.isBlank()) {
            return 0;
        }
        String digits = rawYear.replaceAll("[^0-9].*$", "");
        try {
            return digits.isBlank() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
