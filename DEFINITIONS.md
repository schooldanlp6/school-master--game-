# Asset definitions

How assets are defined, derived, served and used in the code.

## Principle

The assets server is **just a folder**. Anything that can serve static files can serve it:
a CDN, nginx, Tomcat, or `python -m http.server`. Unlike the game server it isn't mission
critical. Clients cache assets, and since every copy of the folder is identical, any number of
mirrors can serve it. If one goes down, a client tries the next one or keeps its cached copy.

So:

- the **folder layout is the contract** between the game and the asset server,
- the layout is **derived** from one definitions file, never written by hand,
- code never builds asset paths itself; it always goes through `AssetAdapter`.

## Source of truth

```
data/cards/assets.defs.json   ← what asset groups exist and which types they have
        │  points at
        ├── CardAPI/.../utils/Profession.java   (enum constants → file names)
        ├── CardAPI/.../utils/Rarity.java       (enum constants → file names)
        └── data/cards/CardMeta.json             (card ids → file names)
        │
        ▼  make asset-paths   (dlp6/danlp6/assets.py)
data/cards/assets.manifest    ← every expected path, one per line (generated, gitignored)
        │
        ▼  make asset-tree / asset-gather / asset-clean / asset-index
data/cards/assets/            ← the folder you deploy / point the server at
        └── index.json        ← path → size + sha256 (generated, gitignored)
```

### `assets.defs.json`

```json
{
  "types":  { "picture": "png", "sound": "mp3" },
  "groups": {
    "rarity": {
      "types":  ["picture", "sound"],
      "source": { "enum": "CardAPI/src/main/java/de/adrian/utils/Rarity.java" }
    }
  }
}
```

| Key              | Meaning                                                                   |
|------------------|---------------------------------------------------------------------------|
| `types`          | Asset type → file extension. Becomes the top-level folder.                 |
| `groups.<name>`  | Second-level folder.                                                       |
| `.types`         | The types this group has assets for.                                       |
| `.source.enum`   | Java file; the names are the enum constants.                               |
| `.source.cardMeta` | JSON file; the names are its top-level keys (card ids).                  |
| `.source.names`  | A literal list of names, for groups that don't come from code.             |

### Path rule

```
<type>/<group>/<lower-case name>.<ext>
picture/rarity/elite.png
sound/rarity/elite.mp3
picture/cards/robert.png
```

All lower-case, because nginx, Tomcat and most CDNs treat paths as case-sensitive.

## Adding things

| You add…                         | Do                                                               |
|----------------------------------|------------------------------------------------------------------|
| a `Rarity` / `Profession` value  | Nothing in the defs. Run `make asset-update` and drop the files in. |
| a card in `CardMeta.json`        | Same as above.                                                    |
| a new group (e.g. `stats`)       | Add it to `assets.defs.json`, add a constant + `path(...)` overload to `AssetAdapter`. |
| a new type (e.g. `video`)        | Add it to `types` in the defs **and** to `AssetAdapter.Type`.     |

Then:

```sh
make asset-update              # derive, show orphans, create folders, write index.json
make asset-gather SRC=~/art    # pull files from wherever the artists keep them
make asset-check               # what's still missing / still a placeholder
make asset-check STRICT=1      # same, but fail (for CI before a deploy)
make asset-clean FORCE=1       # actually delete orphans
```

`asset-gather` matches each expected path in `SRC` in this order: the same relative path,
then `<group>/<file>` anywhere below `SRC`, then just `<file>` anywhere. The source folder can
be organised however you like.

## Using the adapter in code

`dlp6.danlp6.AssetAdapter` (in `CardAPI`, so the server and clients can both use it) turns
game objects into asset paths and URLs.

```java
import dlp6.danlp6.AssetAdapter;
import dlp6.danlp6.AssetAdapter.Type;

// base URLs: CDN first, mirrors after. Or from env: ASSET_BASE_URLS="https://cdn/a,http://m2:8080"
AssetAdapter assets = AssetAdapter.fromEnv();

URI rarityIcon  = assets.url(Rarity.ELITE, Type.PICTURE);        // …/picture/rarity/elite.png
URI raritySound = assets.url(Rarity.ELITE, Type.SOUND);          // …/sound/rarity/elite.mp3
URI profIcon    = assets.url(Profession.KUNST, Type.PICTURE);    // …/picture/professions/kunst.png
URI cardArt     = assets.url(api.identifier("robert"), Type.PICTURE); // …/picture/cards/robert.png

// fallback over mirrors
for (URI u : assets.urls(AssetAdapter.path(Rarity.ELITE, Type.PICTURE))) { /* try in order */ }

// integrity / cache busting
URI index = assets.indexUrl();                                    // …/index.json
```

Rules:

1. **Never concatenate asset paths by hand.** Use `AssetAdapter.path(...)` / `url(...)`.
   That way the layout stays defined in one place in Java and one place in the defs.
2. **Send paths, not URLs, from the game server.** The server can put
   `AssetAdapter.path(...)` (e.g. `picture/rarity/elite.png`) in its payloads, and each client
   resolves it against its own configured mirrors. The game server doesn't need to know
   where the CDN is.
3. **A missing asset is not an error.** Show a default or placeholder and move on. The
   game must keep working with the asset server down.
4. **Cache by `index.json`.** Clients can fetch `index.json`, compare the `sha256` with their
   cache, and only download what changed. The hash also works as a cache-busting query
   parameter (`?v=<sha256>`) if the CDN caches aggressively.

## Relation to the existing server code

`Server/.../cardapi/CardAssets` (with `ProfessionAsset`, `RarityAsset`) currently creates the
same tree at game-server startup and resolves local `File`s. Now that the Makefile owns the
tree, that's redundant, and it has a casing bug: files are created lower-case but looked up
with `name()` (upper-case). Possible next step: have it delegate to
`AssetAdapter.local(root, AssetAdapter.path(...))`, or drop it in favour of URLs.
