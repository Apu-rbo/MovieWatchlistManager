package com.watchlist.model;

import java.time.Instant;
import java.time.Year;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Encapsulated, validation-guarded data object representing a single
 * watchlist entry. No field is ever exposed for direct mutation;
 * everything goes through setters that enforce invariants, so a
 * Movie object can never exist in an invalid state once constructed.
 */
public class Movie {

    private final String id;        // stable identity, independent of title edits
    private final Instant addedOn;  // when this entry was first created; never changes on edit
    private String title;
    private Set<Genre> genres;      // always non-empty; a movie can belong to more than one genre
    private int releaseYear;
    private int rating;             // 0 (unrated) to 5 stars
    private WatchStatus status;
    private String notes;

    /** New-movie constructor: generates a fresh id and stamps the current time as addedOn. */
    public Movie(String title, Set<Genre> genres, int releaseYear, int rating,
                 WatchStatus status, String notes) {
        this(UUID.randomUUID().toString(), Instant.now(), title, genres, releaseYear, rating, status, notes);
    }

    /**
     * Full constructor used when reconstructing a Movie from storage, or when editing an
     * existing movie and the caller wants to preserve its original id/addedOn.
     */
    public Movie(String id, Instant addedOn, String title, Set<Genre> genres, int releaseYear, int rating,
                 WatchStatus status, String notes) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.addedOn = addedOn == null ? Instant.now() : addedOn;
        setTitle(title);
        setGenres(genres);
        setReleaseYear(releaseYear);
        setRating(rating);
        setStatus(status);
        setNotes(notes);
    }

    public String getId() {
        return id;
    }

    /** When this entry was first added. Used for a real (not insertion-order-approximated) "Recently Added". */
    public Instant getAddedOn() {
        return addedOn;
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

    /** Unmodifiable view. A movie always carries at least one genre. */
    public Set<Genre> getGenres() {
        return Collections.unmodifiableSet(genres);
    }

    public void setGenres(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            throw new IllegalArgumentException("Select at least one genre");
        }
        this.genres = EnumSet.copyOf(genres);
    }

    /** Comma-separated display label, e.g. "Action, Sci-Fi" — for GUI use instead of a raw Set. */
    public String getGenreLabel() {
        return genres.stream()
                .map(Genre::getDisplayName)
                .sorted()
                .collect(Collectors.joining(", "));
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
