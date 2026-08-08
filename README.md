# Movie Watchlist Manager

A desktop Swing app (FlatLaf dark theme + Gson-backed JSON persistence) for
tracking movies you plan to watch, are watching, have watched, or dropped.
Dashboard + stats + poster-style card grid, not a raw JTable.

## Architecture

```
com.watchlist
├── Main.java                    Entry point: installs the dark theme, wires everything, launches GUI
├── model/                       Plain data + collection classes, zero Swing/IO knowledge
│   ├── Genre.java
│   ├── WatchStatus.java
│   ├── Movie.java                Validation-guarded entity (throws IllegalArgumentException on bad input)
│   └── Watchlist.java            In-memory collection of Movie
├── repository/                  Persistence, decoupled behind an interface
│   ├── StorageInterface.java
│   └── JsonStorage.java          Gson + atomic write-then-rename to a temp file
├── controller/
│   ├── WatchlistController.java  The only class that talks to both model and storage;
│   │                              exposes add/update/delete/search/filter, getStats(),
│   │                              getRecentlyAdded(), and a change-listener API
│   └── WatchlistStats.java       Immutable snapshot record consumed by the dashboard
└── gui/
    ├── MainWindow.java           Sidebar + CardLayout content area; owns all add/edit/delete wiring
    ├── View.java                 DASHBOARD / LIBRARY enum
    ├── Sidebar.java              Nav with an animated active-item indicator
    ├── DashboardPanel.java       Stat tiles, genre breakdown bars, recently-added row
    ├── LibraryPanel.java         Search/filter/sort + the poster card grid
    ├── MovieDialog.java          Modal add/edit form; only writes back on "Save"
    ├── theme/
    │   └── AppTheme.java          Palette, fonts, per-genre colors, FlatLaf bootstrap
    └── components/
        ├── MovieCard.java              Poster-style tile (genre gradient, status pill, star strip)
        ├── ResponsiveCardGrid.java     Reflows its column count as the window resizes
        ├── StatCard.java               Dashboard tile with a count-up animation
        └── StatBar.java                Proportional bar row for the genre breakdown
```

**Design notes**
- All validation (blank titles, out-of-range years/ratings) lives in `Movie`'s setters/constructor,
  so both the GUI dialog and any future entry point (CLI, tests, import feature) get the same rules for free.
- The controller never imports anything from `javax.swing` — it only exposes plain data and a
  `Runnable`-based listener, so it could be reused behind a completely different UI.
- Saves are atomic: `JsonStorage` writes to a temp file and does an atomic move into place,
  so a crash mid-save can't corrupt `watchlist.json`.
- Data is stored at `~/.moviewatchlist/watchlist.json` by default.
- There's no real poster-art source (no bundled images, no network image fetch), so `MovieCard`
  paints its own genre-colored gradient "poster" with the title's initial, a status pill, and a
  star strip instead of pretending to show real cover art.
- Sorting in `LibraryPanel` uses `Comparator.reversed()` (Java 8+), not `List.reversed()`
  (Java 21+), to stay compatible with the project's Java 17 target.

## Opening in IntelliJ

1. `File > Open...` and select the `MovieWatchlistManager` folder (the one containing `pom.xml`).
2. IntelliJ will detect it as a Maven project and prompt to load it — accept, or click the
   Maven refresh icon in the right-hand sidebar. This downloads FlatLaf and Gson from Maven Central.
3. Make sure Project SDK is set to Java 17+ (`File > Project Structure > Project`).
4. Run `Main.java` (right-click it > Run 'Main.main()').

## Building / running from the command line

```bash
mvn clean package          # compiles + runs (none yet) tests + builds a runnable fat jar
java -jar target/movie-watchlist-manager.jar

# or, without building a jar:
mvn exec:java
```

## Possible extensions (good OOP-course talking points)

- Swap `JsonStorage` for a `SqliteStorage` implementing the same `StorageInterface` — no other
  class would need to change.
- Add a `WatchlistChangeListener` interface instead of raw `Runnable`/`Consumer<String>` if you
  want to demonstrate custom listener interfaces (currently kept minimal on purpose).
- `Movie` has no added-on timestamp, so "Recently Added" is approximated by list insertion order.
  Adding a real `addedOn` field (and migrating old JSON files that lack it) would be a natural
  next step and a good spot to practice backward-compatible deserialization.
- A real poster-image field (with a bundled placeholder image or a user-supplied file path) could
  replace `MovieCard`'s painted gradient poster.
