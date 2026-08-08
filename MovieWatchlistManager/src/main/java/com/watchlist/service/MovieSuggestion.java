package com.watchlist.service;

/**
 * A single row from an OMDb title search (the "s=" endpoint) - just enough
 * to populate a suggestion dropdown. Genre isn't included by that endpoint,
 * so a follow-up lookup (see OmdbClient.fetchDetails) is needed once the
 * user actually picks one of these.
 */
public record MovieSuggestion(String title, String year, String imdbId, String posterUrl) {

    /** What shows in the suggestion dropdown, e.g. "Spider-Man (2002)". */
    @Override
    public String toString() {
        return title + " (" + year + ")";
    }
}
