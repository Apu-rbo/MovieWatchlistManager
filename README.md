

Readme · MD
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
│   ├── Movie.java                Validation-guarded entity (throws IllegalArgumentException on bad input);
│   │                              carries a Set<Genre> (a movie can belong to more than one genre), a real
│   │                              addedOn timestamp, and an optional posterUrl
│   └── Watchlist.java            In-memory collection of Movie
├── repository/                  Persistence, decoupled behind an interface
│   ├── StorageInterface.java
│   └── JsonStorage.java          Gson + atomic write-then-rename to a temp file; backs up the previous
│                                  watchlist.json (last 5 kept) before every save, and transparently
│                                  migrates JSON written by older versions of this app on load
├── controller/
│   ├── WatchlistController.java  The only class that talks to both model and storage;
│   │                              exposes add/update/delete/search/filter, isDuplicate(),
│   │                              getStats(), getRecentlyAdded() (sorted by real addedOn),
│   │                              and a change-listener API
│   └── WatchlistStats.java       Immutable snapshot record consumed by the dashboard
├── service/                     Optional external data source, decoupled from the GUI
│   ├── OmdbClient.java            Thin client for the OMDb API (title search + by-ID detail lookup);
│   │                              needs an OMDB_API_KEY environment variable, degrades to "no live
│   │                              search" (not a crash) if it's unset
│   ├── MovieSuggestion.java       One row from an OMDb title search
│   └── MovieDetails.java          Full detail payload (genre, poster) from an OMDb by-ID lookup
└── gui/
    ├── MainWindow.java           Sidebar + CardLayout content area; owns all add/edit/delete wiring,
    │                              including the duplicate-title confirmation prompt
    ├── View.java                 DASHBOARD / LIBRARY enum
    ├── Sidebar.java              Nav with an animated active-item indicator
    ├── DashboardPanel.java       Stat tiles, genre breakdown bars, recently-added row
    ├── LibraryPanel.java         Search/filter/sort + the poster card grid
    ├── MovieDialog.java          Modal add/edit form; only writes back on "Save". The title field
    │                              doubles as a live OMDb search box (debounced ~400ms) - picking a
    │                              suggestion auto-fills year, a best-effort genre mapping, and a
    │                              real poster URL
    ├── theme/
    │   └── AppTheme.java          Palette, fonts, per-genre colors, FlatLaf bootstrap
    └── components/
        ├── MovieCard.java              Poster-style tile. Renders the real poster image once it's
        │                                loaded if the movie has a posterUrl; otherwise (or while
        │                                loading) falls back to a painted genre-gradient poster with
        │                                the title's initial, a status pill, and a star strip
        ├── PosterImageCache.java        Loads poster images off the EDT via SwingWorker and caches
        │                                them in memory by URL, so revisiting a card doesn't re-fetch
        ├── ResponsiveCardGrid.java     Reflows its column count as the window resizes
        ├── StatCard.java               Dashboard tile with a count-up animation
        └── StatBar.java                Proportional bar row for the genre breakdown
```
 
**Design notes**
- All validation (blank titles, empty genre selection, out-of-range years/ratings) lives in
  `Movie`'s setters/constructor, so both the GUI dialog and any future entry point (CLI, tests,
  import feature) get the same rules for free.
- A movie can belong to more than one genre (`Set<Genre>`, backed by `EnumSet`); the genre
  breakdown on the dashboard counts a movie once per genre it belongs to, so those totals can
  exceed the total movie count - that's expected, not a bug.
- `Movie.addedOn` is a real `Instant`, stamped once at creation and preserved across edits.
  "Recently Added" on the dashboard sorts by this, not by list order.
- Adding a movie with a title+year that already exists in the watchlist prompts a
  "possible duplicate" confirmation rather than silently blocking or silently allowing it.
- The controller never imports anything from `javax.swing` — it only exposes plain data and a
  `Runnable`-based listener, so it could be reused behind a completely different UI.
- Saves are atomic: `JsonStorage` writes to a temp file and does an atomic move into place,
  so a crash mid-save can't corrupt `watchlist.json`. The previous file is also copied into
  `backups/` (timestamped, last 5 kept) before every save.
- `JsonStorage.load()` transparently upgrades JSON written by older versions of this app: a
  singular `"genre"` field becomes a one-element `"genres"` array, and a missing `"addedOn"` is
  filled in with the current time - so an existing `watchlist.json` from before these changes
  still loads correctly.
- Data is stored at `~/.moviewatchlist/watchlist.json` by default.
- Live title search and real poster art are optional: `service/OmdbClient` calls the free
  [OMDb API](https://www.omdbapi.com), which needs an `OMDB_API_KEY` environment variable (get
  one at https://www.omdbapi.com/apikey.aspx). Without it, `MovieDialog` just behaves as a plain
  manual-entry form - nothing crashes, there's no live search or poster autofill, and cards
  fall back to the painted gradient poster. Note OMDb's own dataset can lag behind very recent
  theatrical releases by some weeks.
- Sorting in `LibraryPanel` uses `Comparator.reversed()` (Java 8+), not `List.reversed()`
  (Java 21+), to stay compatible with the project's Java 17 target.
## Opening in IntelliJ
 
1. `File > Open...` and select the `MovieWatchlistManager` folder (the one containing `pom.xml`).
2. IntelliJ will detect it as a Maven project and prompt to load it — accept, or click the
   Maven refresh icon in the right-hand sidebar. This downloads FlatLaf and Gson from Maven Central.
3. Make sure Project SDK is set to Java 17+ (`File > Project Structure > Project`).
4. (Optional, for live search + posters) Set `OMDB_API_KEY` in your run configuration:
   `Run > Edit Configurations... > Environment variables`.
5. Run `Main.java` (right-click it > Run 'Main.main()').
## Building / running from the command line
 
```bash
mvn clean package          # compiles + runs (none yet) tests + builds a runnable fat jar
OMDB_API_KEY=your_key_here java -jar target/movie-watchlist-manager.jar
 
# or, without building a jar:
OMDB_API_KEY=your_key_here mvn exec:java
```
 
## Possible extensions (good OOP-course talking points)
 
- Swap `JsonStorage` for a `SqliteStorage` implementing the same `StorageInterface` — no other
  class would need to change.
- Add a `WatchlistChangeListener` interface instead of raw `Runnable`/`Consumer<String>` if you
  want to demonstrate custom listener interfaces (currently kept minimal on purpose).
- Unit tests for `Movie`'s validation rules and `WatchlistController`'s CRUD/filter/duplicate
  logic — both are pure logic with no Swing involved, so they're the cheapest, highest-value
  place to start.
- Export/import via the same Gson pipeline `JsonStorage` already uses.
- Undo-delete: keep the last deleted `Movie` in the controller for a few seconds and offer an
  "Undo" action, rather than deletes being instant and permanent.
- A second `service` client for TMDb, tried as a fallback when OMDb has no results — TMDb tends
  to index brand-new theatrical releases faster than OMDb does.

 
