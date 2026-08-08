package com.watchlist.gui.components;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Loads poster images off the Swing Event Dispatch Thread and caches them in
 * memory by URL, so scrolling the card grid or revisiting the dashboard
 * doesn't re-download the same poster over and over. Deliberately simple -
 * no disk cache, no eviction - fine for a single-session desktop app with a
 * personal-sized watchlist.
 */
public final class PosterImageCache {

    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> FAILED = new ConcurrentHashMap<>();

    private PosterImageCache() {
    }

    /**
     * Returns the cached image immediately if we already have it (or already
     * know it fails to load); otherwise kicks off a background fetch and
     * calls {@code onLoaded} on the EDT once it completes. onLoaded is never
     * called for a URL that fails to load.
     */
    public static Image getOrLoad(String url, Consumer<Image> onLoaded) {
        if (url == null || url.isBlank() || Boolean.TRUE.equals(FAILED.get(url))) {
            return null;
        }
        Image cached = CACHE.get(url);
        if (cached != null) {
            return cached;
        }

        new SwingWorker<Image, Void>() {
            @Override
            protected Image doInBackground() {
                try {
                    URL parsed = URI.create(url).toURL();
                    return ImageIO.read(parsed);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Image image = get();
                    if (image == null) {
                        FAILED.put(url, Boolean.TRUE);
                        return;
                    }
                    CACHE.put(url, image);
                    onLoaded.accept(image);
                } catch (Exception e) {
                    FAILED.put(url, Boolean.TRUE);
                }
            }
        }.execute();

        return null; // not ready yet; caller falls back to the painted gradient for this paint cycle
    }
}
