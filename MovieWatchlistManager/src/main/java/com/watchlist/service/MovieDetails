package com.watchlist.service;

/**
 * Full detail payload from an OMDb by-ID lookup (the "i=" endpoint), used
 * once the user picks a MovieSuggestion. Carries the raw OMDb genre string
 * (e.g. "Action, Adventure, Sci-Fi") - mapping that onto this app's Genre
 * enum happens in MovieDialog, not here, since OMDb's genre vocabulary
 * doesn't line up one-to-one with ours.
 */
public record MovieDetails(String title, int year, String rawGenres, String posterUrl) {
}
