package com.watchlist.controller;

import com.watchlist.model.Genre;

import java.util.Map;

/**
 * Immutable snapshot of aggregate watchlist numbers, computed on demand by
 * WatchlistController.getStats(). Kept as a plain data carrier (no behavior)
 * so DashboardPanel can render it directly without reaching back into the
 * controller mid-paint.
 */
public record WatchlistStats(
        int total,
        int watching,
        int watched,
        int planToWatch,
        int dropped,
        double averageRating,
        Map<Genre, Long> countsByGenre
) {
}
