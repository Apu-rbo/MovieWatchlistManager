package com.watchlist.model;

import java.awt.Color;

/**
 * Tracks where a movie sits in the user's viewing pipeline.
 * Each status owns a badge color so StatusBadgeRenderer can stay
 * dumb and just ask the enum how to paint itself.
 */
public enum WatchStatus {
    PLAN_TO_WATCH("Plan to Watch", new Color(0x5C6BC0)),
    WATCHING("Watching", new Color(0xFFA726)),
    WATCHED("Watched", new Color(0x66BB6A)),
    DROPPED("Dropped", new Color(0xEF5350));

    private final String displayName;
    private final Color badgeColor;

    WatchStatus(String displayName, Color badgeColor) {
        this.displayName = displayName;
        this.badgeColor = badgeColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getBadgeColor() {
        return badgeColor;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /** Looks up a status by its display name, used by StatusBadgeRenderer to recover
     *  the enum (and thus its color) from a table cell's rendered String value. */
    public static WatchStatus fromDisplayName(String displayName) {
        for (WatchStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status display name: " + displayName);
    }
}
