package com.watchlist.model;

import java.time.Year;
import java.util.Objects;
import java.util.UUID;

/**
 * Encapsulated, validation-guarded data object representing a single
 * watchlist entry. No field is ever exposed for direct mutation;
 * everything goes through setters that enforce invariants, so a
 * Movie object can never exist in an invalid state once constructed.
 */
public class Movie {

    private final String id;      // stable identity, independent of title edits
    private String title;
    private Genre genre;
    private int releaseYear;
    private int rating;           // 0 (unrated) to 5 stars
    private WatchStatus status;
    private String notes;

    public Movie(String title, Genre genre, int releaseYear, int rating,
                 WatchStatus status, String notes) {
        this(UUID.randomUUID().toString(), title, genre, releaseYear, rating, status, notes);
    }

    /**
     * Full constructor used when reconstructing a Movie from storage,
     * where the id must be preserved rather than regenerated.
     */
    public Movie(String id, String title, Genre genre, int releaseYear, int rating,
                 WatchStatus status, String notes) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        setTitle(title);
        setGenre(genre);
        setReleaseYear(releaseYear);
        setRating(rating);
        setStatus(status);
        setNotes(notes);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        this.title = title.trim();
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = Objects.requireNonNull(genre, "Genre must be selected");
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        int currentYear = Year.now().getValue();
        // 1888 is generally cited as the year of the earliest surviving
        // motion picture (Roundhay Garden Scene), so it's a sane floor.
        if (releaseYear < 1888 || releaseYear > currentYear + 5) {
            throw new IllegalArgumentException(
                    "Release year must be between 1888 and " + (currentYear + 5));
        }
        this.releaseYear = releaseYear;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5 stars");
        }
        this.rating = rating;
    }

    public WatchStatus getStatus() {
        return status;
    }

    public void setStatus(WatchStatus status) {
        this.status = Objects.requireNonNull(status, "Status must be selected");
    }

    public String getNotes() {
        return notes == null ? "" : notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movie movie)) return false;
        return id.equals(movie.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title + " (" + releaseYear + ")";
    }
}
