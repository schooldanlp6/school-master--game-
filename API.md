<!-- Author: danlp6 -->
# API definitions

The contracts between the parts of the project. Server and Client are separate projects that
share **no code**. They only depend on what's written here.

```
             game messages (2.)                      assets over HTTP (1.)
 Client  ◄──────────────────────►  Server/Backend      Client  ◄─────────  Server/Assets (CDN + mirrors)
 (stateless,                       (authoritative,
  inputs + rendering)               all game logic)
```

| Part             | Role                                                                                   |
|------------------|----------------------------------------------------------------------------------------|
| `Server/Backend` | The real server. Owns all game state and rules (cards, plugins, games).                 |
| `Server/Assets`  | Static files only (pictures, sounds). Any static web server / CDN / number of mirrors.  |
| `Client`         | A dynamic, stateless dummy: it sends inputs and renders what the backend tells it. Its only local data is a disposable asset cache. |

## 1. Assets CDN API

**Status: implemented.** Produced by `Server/Makefile`
([Server/DEFINITIONS.md](Server/DEFINITIONS.md)) and consumed by
`Client/.../AssetClient`.

### Base URLs

A CDN or mirror is identified by a base URL, e.g. `https://cdn.example.com/assets/`. A client
has an ordered list of them (CDN first, then mirrors). All of them serve **byte-identical**
copies of `Server/Assets/`.

### Asset path

```
path    = segment "/" segment *( "/" segment )
segment = [a-z0-9] *[a-z0-9_.-]

<type>/<group>/<name>.<ext>              picture/rarity/elite.png
<type>/plugins/<plugin>/<name>.<ext>     sound/plugins/demo/win.mp3
```

| Type      | Extension | Content type |
|-----------|-----------|--------------|
| `picture` | `png`     | `image/png`  |
| `sound`   | `mp3`     | `audio/mpeg` |

Paths are always relative (no leading `/`), lower-case, and never contain `.` or `..`
segments, so they need no URL encoding. Clients must reject anything else.

### `GET <base>/<path>`

| Status | Meaning                                                  |
|--------|----------------------------------------------------------|
| `200`  | The asset bytes.                                         |
| `404`  | Not available (not made yet, or removed). Not an error.  |
| other / no answer | This mirror is down; try the next one.        |

A client may append `?v=<sha256>` (from the index) for cache busting. Static servers ignore
the query string.

### `GET <base>/index.json`

Lists every asset that is actually available (empty placeholder files are left out):

```json
{
  "assets": {
    "picture/rarity/elite.png": { "size": 48213, "sha256": "9fddb80d…(64 lower-case hex)" },
    "sound/rarity/elite.mp3":   { "size": 91022, "sha256": "…" }
  }
}
```

- `size` is the length in bytes and `sha256` is the lower-case hex SHA-256 of the file.
- An asset that isn't listed doesn't exist on the CDN.
- Clients ignore unknown fields and skip invalid entries.

### Client behaviour (normative, implemented by `AssetClient`)

1. On start, fetch `index.json` from the first base URL that returns a valid one, sending
   `Cache-Control: no-cache`. Keep a copy on disk. If no mirror answers, use the cached copy.
2. For an asset, use the cached file if its size and sha256 match the index. Otherwise
   download it from the mirrors in order, and only accept bytes that match the index. Write the
   file atomically (temp file + rename).
3. If the index is loaded and doesn't list a path, don't request it.
4. If every mirror fails, use an older cached copy if there is one. Otherwise the asset is
   missing and the client shows a placeholder. **A missing asset never breaks the game.**
5. The cache is disposable. Deleting it only costs a re-download.

### Deploying (server side)

1. `make -C Server asset-update`, then `make -C Server asset-check STRICT=1`.
2. Copy `Server/Assets/` to every mirror. Upload `index.json` **last**, so no mirror ever lists
   a file it doesn't have yet:
   `rsync -a --exclude index.json Server/Assets/ host:/srv/assets/ && rsync -a Server/Assets/index.json host:/srv/assets/`
3. Recommended caching headers: short or no caching for `index.json`; long caching for
   everything else (changed files get a new `?v=`).

## 2. Backend ↔ Client game API

**Status: draft, not implemented.** The backend has no network layer yet. This section
fixes the rules the implementation has to follow, and proposes a message format.

### Rules

1. **The backend is authoritative.** All game state, rules, randomness and validation live
   in `Server/Backend` (`Game`, `GamePlugin`s, `CardAPI`). The client never decides anything.
2. **The client is stateless.** It holds no game state beyond what it's currently showing. On
   (re)connect the backend sends the full state again, so a client can crash, restart or be
   swapped at any time.
3. **Client → backend: inputs only** (what the player clicked or chose), never results.
4. **Backend → client: what to show.** The full view state, or changes to it, with assets
   referenced by **path** (section 1), never by URL or file.
5. **Plugins don't need client updates.** Plugin content reaches the client the same way as
   everything else: as view state plus asset paths (`picture/plugins/<plugin>/…`).

### Proposed format (to be confirmed when the network layer is built)

The transport is open (WebSocket is the natural fit). Every message is one JSON object:

```json
{ "type": "<message type>", "data": { } }
```

| Direction | `type`    | `data`                                                                       |
|-----------|-----------|------------------------------------------------------------------------------|
| C → S     | `hello`   | `{ "protocol": 1 }`                                                           |
| S → C     | `welcome` | `{ "protocol": 1, "assets": { "baseUrls": ["https://cdn…/", "…"] } }`         |
| S → C     | `state`   | `{ "view": { … } }`: everything the client should show right now             |
| C → S     | `input`   | `{ "action": "<action>", … }`, e.g. `{ "action": "play_card", "card": "robert" }` |
| S → C     | `error`   | `{ "message": "…" }`: e.g. an input that isn't allowed right now              |

`welcome.assets.baseUrls` lets the backend hand out the current mirror list, so a client
needs no configuration besides the backend address. `ASSET_BASE_URLS` on the client overrides
it.

Example `state.view` fragment, with assets as paths only:

```json
{ "hand": [ { "id": "robert", "picture": "picture/cards/robert.png", "rarity": "picture/rarity/elite.png" } ] }
```
