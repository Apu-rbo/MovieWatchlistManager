package com.watchlist.model;

/**
 * Represents the genre of a movie.
 * Each constant carries a user-friendly display name so the raw
 * enum identifier (e.g. SCI_FI) never has to leak into the UI.
 */
public enum Genre {
    ACTION("Action"),
    COMEDY("Comedy"),
    DRAMA("Drama"),
    HORROR("Horror"),
    SCI_FI("Sci-Fi"),
    ROMANCE("Romance"),
    THRILLER("Thriller"),
    DOCUMENTARY("Documentary"),
    ANIMATION("Animation"),
    FANTASY("Fantasy"),
    OTHER("Other");

    private final String displayName;

    Genre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        // Swing components (JComboBox, table cells) call toString() by
        // default, so routing it through displayName keeps every widget
        // consistent without extra renderer code.
        return displayName;
    }
}
