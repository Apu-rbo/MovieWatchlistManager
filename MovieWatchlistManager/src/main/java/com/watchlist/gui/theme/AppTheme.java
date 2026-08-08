package com.watchlist.gui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.watchlist.model.Genre;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Single source of truth for the app's dark visual identity: palette,
 * fonts, and the FlatLaf bootstrap. Every GUI class pulls its colors and
 * fonts from here instead of hardcoding hex values, so the whole app can
 * be re-themed by editing one file.
 */
public final class AppTheme {

    private AppTheme() {
    }

    // ---------- Palette ----------

    public static final Color BG_APP = new Color(0x141414);
    public static final Color BG_SIDEBAR = new Color(0x0D0D0D);
    public static final Color BG_CARD = new Color(0x1F1F1F);
    public static final Color BG_CARD_HOVER = new Color(0x2A2A2A);
    public static final Color BG_INPUT = new Color(0x252525);

    public static final Color BORDER = new Color(0x2E2E2E);

    public static final Color TEXT_PRIMARY = new Color(0xF5F5F5);
    public static final Color TEXT_SECONDARY = new Color(0xA0A0A0);
    public static final Color TEXT_MUTED = new Color(0x6E6E6E);

    /** Primary brand accent — used for the active nav item, primary buttons, and highlights. */
    public static final Color ACCENT = new Color(0xE63946);
    public static final Color ACCENT_HOVER = new Color(0xF3495A);
    /** Secondary accent for positive/informational stats (kept distinct from the primary accent). */
    public static final Color ACCENT_TEAL = new Color(0x2EC4B6);
    public static final Color ACCENT_GOLD = new Color(0xFFC107);

    // ---------- Fonts ----------

    public static final Font FONT_LOGO = new Font("SansSerif", Font.BOLD, 20);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_STAT_VALUE = new Font("SansSerif", Font.BOLD, 26);
    public static final Font FONT_NAV = new Font("SansSerif", Font.PLAIN, 14);

    // ---------- Per-genre color coding (used on poster cards + dashboard bars) ----------

    private static final Map<Genre, Color> GENRE_COLORS = new EnumMap<>(Genre.class);

    static {
        GENRE_COLORS.put(Genre.ACTION, new Color(0xE63946));
        GENRE_COLORS.put(Genre.COMEDY, new Color(0xFFB703));
        GENRE_COLORS.put(Genre.DRAMA, new Color(0x8D6A9F));
        GENRE_COLORS.put(Genre.HORROR, new Color(0x6A0572));
        GENRE_COLORS.put(Genre.SCI_FI, new Color(0x2EC4B6));
        GENRE_COLORS.put(Genre.ROMANCE, new Color(0xEF476F));
        GENRE_COLORS.put(Genre.THRILLER, new Color(0x3A506B));
        GENRE_COLORS.put(Genre.DOCUMENTARY, new Color(0x588157));
        GENRE_COLORS.put(Genre.ANIMATION, new Color(0x4CC9F0));
        GENRE_COLORS.put(Genre.FANTASY, new Color(0x7209B7));
        GENRE_COLORS.put(Genre.OTHER, new Color(0x5C6BC0));
    }

    /** Returns a stable accent color per genre, used for poster gradients and genre bars. */
    public static Color genreColor(Genre genre) {
        return GENRE_COLORS.getOrDefault(genre, new Color(0x5C6BC0));
    }

    // ---------- Bootstrap ----------

    /** Installs the dark look and feel plus a handful of global default overrides. Call once, before any component is created. */
    public static void install() {
        FlatDarkLaf.setup();

        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.trackArc", 8);

        UIManager.put("@background", toHex(BG_APP));
        UIManager.put("@foreground", toHex(TEXT_PRIMARY));
        UIManager.put("@accentColor", toHex(ACCENT));

        UIManager.put("Panel.background", BG_APP);
        UIManager.put("OptionPane.background", BG_APP);
        UIManager.put("Button.font", FONT_BODY);
        UIManager.put("Label.font", FONT_BODY);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
