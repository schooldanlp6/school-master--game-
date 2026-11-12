<!-- Modified by danlp6: project structure, Server/Client split, assets CDN, Makefile, Game-Plugins -->
# SMG WaoW

A card game built around school life. This repo is the project root for the **Server** and the
**Client**. They're separate projects that share no code or build; they only talk over the
APIs in [API.md](API.md).

```
.
├── API.md                     contracts: Assets CDN API + Backend ↔ Client game API
├── Server/
│   ├── Backend/               the real server: all game state and logic
│   │   ├── CardAPI/           card model + parser (Maven, de.adrian:CardAPI), AssetAdapter
│   │   ├── GameServer/        App, games, Game-Plugins (Maven, de.adrian:Server, needs CardAPI)
│   │   └── data/              runtime data, read relative to the working directory
│   │       ├── cards/CardMeta.json
│   │       └── plugins/*.jar
│   ├── Assets/                the Assets CDN root: static files only, point any web server at it
│   ├── Makefile               asset housekeeping
│   ├── assets.py              …the script behind it (Python 3, stdlib only)
│   ├── assets.defs.json       what assets exist, derived from the sources of truth
│   └── DEFINITIONS.md         asset definitions + housekeeping guide
└── Client/                    stateless client: sends inputs, renders server state, caches assets
```

## Server

### Backend

The authoritative game server. Build `CardAPI` first, because `GameServer` depends on it
through the local Maven repository:

```sh
mvn -f Server/Backend/CardAPI/pom.xml install
mvn -f Server/Backend/GameServer/pom.xml package
```

Run `de.adrian.App` with the **working directory `Server/Backend`**. It reads and creates
`data/` relative to the working directory, i.e. `data/cards/CardMeta.json` and
`data/plugins/`.

#### Game-Plugins

Game logic is extended with plugin jars in `Server/Backend/data/plugins/`. Each jar has a
`plugin.json` (`main`, `version`, `name`, all strings), and its main class extends
`de.adrian.game.GamePlugin`:

```java
public class DemoPlugin extends GamePlugin {
    @Override protected void onEnable()  { registerEvents(new DemoListener()); }
    @Override protected void onDisable() { }
}

class DemoListener implements Listener {
    @EventHandler void onPacket(C2SPacketEvent e) { /* … */ }   // handlers may also take a super type, e.g. Event
}
```

Every `Game` gets fresh instances of all plugins. All of them are created first, then
`onEnable()` runs on each, so `getPlugin(Other.class)` already works in `onEnable()`.
`Game.stop()` calls `onDisable()`.

Handlers run in registration order, and inside one listener in method-name order. Events
that implement `CancelableEvent` (e.g. `C2SPacketEvent`) can be cancelled with
`setCanceled(true)`. After that, later handlers are skipped unless they are annotated
`@EventHandler(ignoreCanceled = false)`. Plugins can ship their own pictures and sounds; see
[Server/DEFINITIONS.md](Server/DEFINITIONS.md#game-plugin-assets).

### Assets CDN

`Server/Assets/` is a **standalone static file server defined only by its folder**. No server
code is involved. Point any static web server at it:

```sh
# Python (dev)
make -C Server asset-serve          # = python3 -m http.server 8080 --directory Server/Assets

# nginx
location /assets/ { alias /path/to/Server/Assets/; }

# Tomcat: drop/symlink the folder into webapps/, or add a Context
<Context path="/assets" docBase="/path/to/Server/Assets" />
```

The folder layout is the API (`GET /assets/picture/rarity/elite.png`), plus a generated
`index.json` with size and sha256 per file. See [API.md](API.md#1-assets-cdn-api). Any number
of mirrors can serve copies of it. It isn't mission critical, because clients cache everything
and fall back to the next mirror.

### Housekeeping (Makefile)

Run in `Server/`, or from the repo root with `make -C Server <target>`. The asset tree stays in
sync with the code without the backend running:

| Target               | What it does                                                                                              |
|----------------------|-----------------------------------------------------------------------------------------------------------|
| `make asset-paths`   | **Gather paths.** Derives every expected asset path from the defs and writes `Server/assets.manifest`.     |
| `make asset-list`    | **Gather assets.** Lists the files that actually exist under `Server/Assets`.                              |
| `make asset-gather`  | **Gather assets.** Copies matching files from `SRC=<dir>` into the tree (the source can be laid out any way). |
| `make asset-tree`    | **Create the structure.** Creates every folder from the manifest; `PLACEHOLDERS=1` also creates empty files. |
| `make asset-check`   | Reports **missing**, **placeholder** (empty) and **orphan** (not in the manifest) files. `STRICT=1` exits non-zero for CI. |
| `make asset-clean`   | **Clean.** Removes orphans and empty folders. Dry run unless `FORCE=1`; `PRUNE_EMPTY=1` also drops placeholders. |
| `make asset-plugins` | Extracts the assets shipped in `Backend/data/plugins/*.jar` into `<type>/plugins/<plugin>/`.               |
| `make asset-index`   | Writes `Server/Assets/index.json` (size + sha256 per asset) for client caches and mirrors.                 |
| `make asset-update`  | **Update.** `paths` → `clean` → `tree` → `plugins` → `index`, so the tree matches the code again.          |
| `make asset-serve`   | Serves `Server/Assets` with `python3 -m http.server` on `PORT` (default 8080).                             |

Overridable: `ASSET_ROOT`, `ASSET_DEFS`, `MANIFEST`, `PYTHON`, `PORT` (paths relative to `Server/`).

Typical workflow after adding a new `Rarity`, `Profession`, card or plugin jar:

```sh
make -C Server asset-update              # derive, show orphans, create folders, extract plugin assets, write index.json
make -C Server asset-gather SRC=~/art    # pull in the real files
make -C Server asset-check               # what's still missing
make -C Server asset-clean FORCE=1       # actually drop the orphans
```

Deploying the assets is copying `Server/Assets/` to every mirror, with `index.json` last
(see [API.md](API.md#deploying-server-side)). The definition format and the rules for backend
code are in [Server/DEFINITIONS.md](Server/DEFINITIONS.md).

## Client

A dynamic, stateless client: it sends the player's inputs to the backend and renders whatever
the backend says. It holds no game state. The only thing on disk is a disposable asset cache.
The game connection follows [API.md](API.md#2-backend--client-game-api) once the backend has a
network layer. The asset side exists already. See [Client/README.md](Client/README.md).

```sh
mvn -f Client/pom.xml package
```

## TODO

### Backend
- [ ] **Network layer.** Build the Backend ↔ Client connection from
      [API.md](API.md#2-backend--client-game-api) (WebSocket is the proposal) and confirm the message
      format; that section is still a draft.
- [ ] **Turn client inputs into events.** Nothing creates a `C2SPacketEvent` yet. The network layer
      should fire one per `input` message, so plugins can react to it or cancel it.
- [ ] **Start and stop games.** `App.main` only loads cards and plugins; nothing calls
      `Game.startGame(...)` or `Game.stop()` yet (lobby/matchmaking, when a game ends).
- [ ] **Game rules.** There are no Game-Plugins yet, so a started game does nothing.
- [ ] **Send the mirror list to the client** (`welcome.assets.baseUrls`), so the client needs only
      the backend address.
- [ ] **Old asset code.** Decide whether to delete `CardAssets`, `AssetElement`, `CardAssetType`,
      `ProfessionAsset` and `RarityAsset`. They're redundant and have a casing bug; see
      [Server/DEFINITIONS.md](Server/DEFINITIONS.md#old-backend-asset-code).
- [ ] `CardMetaType` (from the Server branch) isn't used anywhere yet.
- [ ] Event handlers declared in a listener's *superclass* aren't registered (only the listener
      class's own methods are). Fine for now; decide whether that should work.

### Client
- [ ] **Game connection:** connect to the backend, send `hello`/`input`, render `state`, show `error`.
      Only the asset cache (`AssetClient`) and its command line exist so far.
- [ ] **UI:** rendering and input handling. Pick the toolkit.

### Build & repo
- [ ] **Check the Maven builds.** The `mvn` commands above haven't been run yet: the code was only
      compiled with `javac` on JDK 21, while the poms target Java 26. `CardAPI` probably has to be
      installed (`mvn install`) before `GameServer` can find it.
- [ ] **Tests.** There are no automated tests (`src/test`). The checks done so far were one-off.
- [ ] `GameServer`'s Maven artifactId is still `Server` (from before the folder was renamed).
- [ ] The backend must run with `Server/Backend` as its working directory (`App` reads `data/`
      relative to it). Make the path configurable, or document a start script.
- [ ] `dlp6/` at the repo root only holds old `__pycache__` files and can probably be deleted.
