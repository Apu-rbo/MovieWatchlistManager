package com.watchlist.gui.components;

import com.watchlist.gui.theme.AppTheme;
import com.watchlist.model.Movie;
import com.watchlist.model.WatchStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * A single poster-style tile in the card grid. There's no real poster art
 * source (no network/image dependency), so the "poster" is a genre-colored
 * gradient panel with the movie's initial, a status pill, and a star strip
 * painted on top — enough to make each card visually distinct by genre and
 * status at a glance, without pretending to be a real movie poster.
 *
 * Edit/delete are exposed as small always-visible icon buttons (rather than
 * hover-only overlays) so the actions stay reliably reachable/testable; a
 * hover highlight is layered on top purely for polish.
 */
public class MovieCard extends JPanel {

    private static final int POSTER_HEIGHT_FULL = 150;
    private static final int POSTER_HEIGHT_COMPACT = 110;

    private boolean hovered = false;

    public MovieCard(Movie movie, Consumer<Movie> onEdit, Consumer<Movie> onDelete, boolean compact) {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_CARD);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        add(new PosterPanel(movie, compact ? POSTER_HEIGHT_COMPACT : POSTER_HEIGHT_FULL), BorderLayout.NORTH);
        add(buildInfoPanel(movie, onEdit, onDelete, compact), BorderLayout.CENTER);

        MouseAdapter hoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                setBackground(AppTheme.BG_CARD_HOVER);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                setBackground(AppTheme.BG_CARD);
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onEdit.accept(movie);
                }
            }
        };
        addMouseListener(hoverListener);
    }

    private JPanel buildInfoPanel(Movie movie, Consumer<Movie> onEdit, Consumer<Movie> onDelete, boolean compact) {
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel title = new JLabel(truncate(movie.getTitle(), compact ? 16 : 22));
        title.setToolTipText(movie.getTitle());
        title.setFont(AppTheme.FONT_HEADER);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);

        String genreText = movie.getGenreLabel();
        if (genreText.length() > 24) {
            genreText = genreText.substring(0, 23) + "\u2026";
        }
        JLabel subtitle = new JLabel(genreText + "  \u2022  " + movie.getReleaseYear());
        subtitle.setFont(AppTheme.FONT_SMALL);
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        info.add(title);
        info.add(Box.createVerticalStrut(2));
        info.add(subtitle);

        if (!compact) {
            info.add(Box.createVerticalStrut(8));
            info.add(buildActionRow(movie, onEdit, onDelete));
        }

        return info;
    }

    private JPanel buildActionRow(Movie movie, Consumer<Movie> onEdit, Consumer<Movie> onDelete) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        JButton editButton = smallButton("\u270E Edit");
        editButton.addActionListener(e -> onEdit.accept(movie));

        JButton deleteButton = smallButton("\uD83D\uDDD1 Delete");
        deleteButton.addActionListener(e -> onDelete.accept(movie));

        row.add(editButton);
        row.add(deleteButton);
        return row;
    }

    private JButton smallButton(String text) {
        JButton button = new JButton(text);
        button.setFont(AppTheme.FONT_SMALL);
        button.setForeground(AppTheme.TEXT_SECONDARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(2, 2, 2, 2));
        return button;
    }

    private static String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 1)) + "\u2026";
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        if (hovered) {
            g2.setColor(AppTheme.ACCENT.darker());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        }
        g2.dispose();
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    /** The gradient "poster" area: genre-colored backdrop, initial glyph, status pill, star strip.
     *  If the movie has a real posterUrl (from live search), that image is painted instead once
     *  it finishes loading; until then, or if it has no poster / the load fails, the painted
     *  gradient is shown so the layout never has an empty gap. */
    private static class PosterPanel extends JPanel {
        private final Movie movie;
        private final int height;
        private Image posterImage;

        PosterPanel(Movie movie, int height) {
            this.movie = movie;
            this.height = height;
            setOpaque(false);
            setPreferredSize(new Dimension(10, height));

            String posterUrl = movie.getPosterUrl();
            if (posterUrl != null) {
                Consumer<Image> onLoaded = image -> {
                    posterImage = image;
                    repaint();
                };
                this.posterImage = com.watchlist.gui.components.PosterImageCache.getOrLoad(posterUrl, onLoaded);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (posterImage != null) {
                paintRealPoster(g2);
            } else {
                paintGradientPoster(g2);
            }

            g2.dispose();
        }

        /** Real poster, cropped/scaled to fill the tile and clipped to the same rounded-top shape. */
        private void paintRealPoster(Graphics2D g2) {
            Shape clip = new RoundRectangle2D.Float(0, 0, getWidth(), height, 12, 12);
            g2.setClip(clip);

            int iw = posterImage.getWidth(this);
            int ih = posterImage.getHeight(this);
            if (iw <= 0 || ih <= 0) {
                paintGradientPoster(g2);
                return;
            }
            // Cover-fit: scale so the image fills the tile, cropping whichever
            // dimension overflows, rather than stretching or letterboxing.
            double scale = Math.max((double) getWidth() / iw, (double) height / ih);
            int drawW = (int) Math.ceil(iw * scale);
            int drawH = (int) Math.ceil(ih * scale);
            int drawX = (getWidth() - drawW) / 2;
            int drawY = (height - drawH) / 2;
            g2.drawImage(posterImage, drawX, drawY, drawW, drawH, this);

            g2.setClip(null);
            paintStatusPill(g2);
            paintStars(g2);
        }

        private void paintGradientPoster(Graphics2D g2) {
            // Multiple genres are possible; the poster gradient just needs one
            // representative color, so we take the first from the (Enum-ordered) set.
            Color base = AppTheme.genreColor(movie.getGenres().iterator().next());
            Color dark = base.darker().darker();
            GradientPaint gradient = new GradientPaint(0, 0, base, 0, height, dark);
            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, getWidth(), height, 12, 12);
            // Square off the bottom corners so the poster meets the info panel cleanly.
            g2.fillRect(0, height - 12, getWidth(), 12);

            // Large initial glyph, centered.
            String initial = movie.getTitle().isBlank() ? "?" : movie.getTitle().substring(0, 1).toUpperCase();
            g2.setFont(AppTheme.FONT_TITLE.deriveFont(Font.BOLD, 42f));
            g2.setColor(new Color(255, 255, 255, 60));
            FontMetrics fm = g2.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(initial)) / 2;
            int textY = (height + fm.getAscent()) / 2 - 6;
            g2.drawString(initial, textX, textY);

            paintStatusPill(g2);
            paintStars(g2);
        }

        private void paintStatusPill(Graphics2D g2) {
            WatchStatus status = movie.getStatus();
            String statusText = status.getDisplayName();
            g2.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD));
            FontMetrics pillFm = g2.getFontMetrics();
            int pillWidth = pillFm.stringWidth(statusText) + 16;
            int pillHeight = 18;
            int pillX = getWidth() - pillWidth - 8;
            int pillY = 8;
            g2.setColor(status.getBadgeColor());
            g2.fillRoundRect(pillX, pillY, pillWidth, pillHeight, pillHeight, pillHeight);
            g2.setColor(Color.WHITE);
            g2.drawString(statusText, pillX + 8, pillY + pillHeight - 5);
        }

        private void paintStars(Graphics2D g2) {
            int rating = movie.getRating();
            String stars = "\u2605".repeat(rating) + "\u2606".repeat(5 - rating);
            g2.setFont(AppTheme.FONT_SMALL.deriveFont(12f));
            FontMetrics starFm = g2.getFontMetrics();
            int starsY = height - 8;
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillRoundRect(4, starsY - starFm.getAscent() - 2, starFm.stringWidth(stars) + 10, starFm.getAscent() + 8, 8, 8);
            g2.setColor(AppTheme.ACCENT_GOLD);
            g2.drawString(stars, 9, starsY);
        }
    }
}
