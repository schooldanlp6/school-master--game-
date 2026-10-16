<!-- Author: danlp6 -->
# Asset definitions (Server housekeeping)

How assets are defined, derived and deployed on the server side. The contract clients rely on
(URLs, `index.json`, caching) is in [API.md](../API.md#1-assets-cdn-api). All commands below
run in `Server/` (or use `make -C Server <target>` from the repo root).

## Principle

The Assets CDN (`Server/Assets/`) is **just a folder**. Anything that can serve static files can
serve it: a CDN, nginx, Tomcat, or `python -m http.server`. Unlike the backend it isn't mission
critical. Clients cache assets, and every copy of the folder is identical, so any number of
mirrors can serve it. If one goes down, a client tries the next one or uses its cached copy.

So:

- the **folder layout is the contract** between the backend, the CDN and the client,
- the layout is **derived** from one definitions file, never written by hand,
- backend code never builds asset paths itself; it always goes through `AssetAdapter`.

## Source of truth

```
Server/assets.defs.json         ← what asset groups exist and which types they have
        │  points at
        ├── Backend/CardAPI/.../utils/Profession.java   (enum constants → file names)
        ├── Backend/CardAPI/.../utils/Rarity.java       (enum constants → file names)
        ├── Backend/data/cards/CardMeta.json             (card ids → file names)
        └── Backend/data/plugins/*.jar                   (assets/<type>/* inside each Game-Plugin jar)
        │
        ▼  make asset-paths   (Server/assets.py)
Server/assets.manifest          ← every expected path, one per line (generated, gitignored)
        │
        ▼  make asset-tree / asset-gather / asset-plugins / asset-clean / asset-index
Server/Assets/                  ← the CDN root: deploy it / point the web server at it
        └── index.json          ← path → size + sha256 (generated, gitignored)
```

The Makefile reads the same sources from the defs (`assets.py sources`), so the manifest is
rebuilt whenever one of them changes. There's no second list to keep in sync.

### `assets.defs.json`

```json
{
  "types":  { "picture": "png", "sound": "mp3" },
  "groups": {
    "rarity": {
      "types":  ["picture", "sound"],
      "source": { "enum": "Backend/CardAPI/src/main/java/de/adrian/utils/Rarity.java" }
    }
  }
}
```

| Key                  | Meaning                                                                        |
|----------------------|--------------------------------------------------------------------------------|
| `types`              | Asset type → file extension. Becomes the top-level folder.                      |
| `groups.<name>`      | Second-level folder.                                                            |
| `.types`             | The types this group has assets for.                                            |
| `.source.enum`       | Java file; the names are the enum constants.                                    |
| `.source.cardMeta`   | JSON file; the names are its top-level keys (card ids).                         |
| `.source.names`      | A literal list of names, for groups that don't come from code.                  |
| `.source.pluginJars` | Folder of Game-Plugin jars; the names are the files each jar ships (see below). |

Source paths are relative to `Server/`. Top-level keys other than `types` and `groups` (like
`_author`) are ignored; every key inside `groups` is a group.

### Path rule

```
<type>/<group>/<name>.<ext>              picture/rarity/elite.png, picture/cards/robert.png
<type>/plugins/<plugin>/<name>.<ext>     picture/plugins/demo/icon.png
```

Everything is lower-cased, and every segment must match `[a-z0-9][a-z0-9_.-]*`, so paths are
URL safe without encoding. `make asset-paths` fails on a name that breaks the rule, e.g. a
card id with a space or an umlaut. `AssetAdapter` throws, and the client rejects such paths.

## Adding things

| You add…                         | Do                                                                                |
|----------------------------------|-----------------------------------------------------------------------------------|
| a `Rarity` / `Profession` value  | Nothing in the defs. Run `make asset-update` and drop the files in.               |
| a card in `CardMeta.json`        | Same as above.                                                                    |
| a new group (e.g. `stats`)       | Add it to `assets.defs.json`, add a constant + `path(...)` overload to `AssetAdapter`. |
| a new type (e.g. `video`)        | Add it to `types` in the defs **and** to `AssetAdapter.Type`.                     |
| a Game-Plugin with assets        | Drop the jar in `Backend/data/plugins/`, run `make asset-update`.                 |

Then:

```sh
make asset-update              # derive, show orphans, create folders, extract plugin assets, write index.json
make asset-gather SRC=~/art    # pull files from wherever the artists keep them
make asset-check               # what's still missing / still a placeholder
make asset-check STRICT=1      # same, but fail (for CI before a deploy)
make asset-clean FORCE=1       # actually delete orphans
make asset-serve               # serve Server/Assets on :8080 for local dev
```

`asset-gather` matches each expected path in `SRC` in this order: the same relative path,
then `<group>/[<plugin>/]<file>` anywhere below `SRC`, then just `<file>` anywhere. The source
folder can be organised however you like.

Dotfiles in `Server/Assets/` (e.g. `.gitkeep`) are never treated as assets or orphans.

## Using the adapter in the backend

`dlp6.danlp6.AssetAdapter` (in `Backend/CardAPI`) turns game objects into asset **paths**.
That's all the backend needs. URLs, mirrors and caching are the client's job
(`Client/.../AssetClient`, see [API.md](../API.md)).

```java
import dlp6.danlp6.AssetAdapter;
import dlp6.danlp6.AssetAdapter.Type;

String rarityIcon  = AssetAdapter.path(Rarity.ELITE, Type.PICTURE);              // picture/rarity/elite.png
String raritySound = AssetAdapter.path(Rarity.ELITE, Type.SOUND);                // sound/rarity/elite.mp3
String profIcon    = AssetAdapter.path(Profession.KUNST, Type.PICTURE);          // picture/professions/kunst.png
String cardArt     = AssetAdapter.path(api.identifier("robert"), Type.PICTURE);  // picture/cards/robert.png
```

Rules:

1. **Never concatenate asset paths by hand.** Use `AssetAdapter.path(...)`. That way the layout
   stays defined in one place in Java and one place in the defs.
2. **Send paths, not URLs.** Messages to the client contain `picture/rarity/elite.png`, and the
   client resolves it against its mirrors. The backend's game logic doesn't need to know where
   the CDN is.
3. **A missing asset is not an error.** The client shows a placeholder. The game must keep
   working with the CDN down.

## Game-Plugin assets

Game-Plugins (`Backend/data/plugins/*.jar`, loaded by `PluginDefsInitiator`) bring their own
assets inside the jar. The jar is their source of truth, so they need no defs entry of their
own:

```
demo.jar
├── plugin.json            {"main": "com.example.demo.DemoPlugin", "version": "1.0", "name": "Demo"}  (all strings)
├── com/example/demo/…
└── assets/
    ├── picture/icon.png   → picture/plugins/demo/icon.png
    └── sound/win.mp3      → sound/plugins/demo/win.mp3
```

- Only `assets/<type>/<name>.<ext>` is picked up (flat, no subfolders). The extension must
  match the type from the defs, and the name must follow the path rule; anything else is
  skipped with a warning.
- Unlike the enum/card groups, a plugin only gets the paths for files it actually ships, not
  every type × name combination.
- The plugin `name` becomes the folder (lower-cased). It must start with `A-Z a-z 0-9` and may
  only contain `A-Z a-z 0-9 _ . -`. The backend and `assets.py` both skip jars with an invalid
  `plugin.json` or a name that's already taken (case-insensitive; the first jar by file name
  wins), so they always agree on which plugins exist.
- Two files that differ only in case (`Icon.png`, `icon.png`) map to the same path; the first
  is kept and a warning is printed.
- `make asset-plugins` (also part of `asset-update`) copies the files out of the jars. It
  overwrites changed files, so a jar always wins over anything copied in with `asset-gather`.
- When a jar is removed, its paths drop out of the manifest on the next `make` run and
  `asset-clean` lists them as orphans.

In plugin code:

```java
public class DemoPlugin extends GamePlugin {
    @Override protected void onEnable() {
        registerEvents(new DemoListener());
        String icon = assetPath("icon", AssetAdapter.Type.PICTURE);   // picture/plugins/demo/icon.png
    }
    @Override protected void onDisable() { }
}
```

`assetPath(...)` is the same as `AssetAdapter.pluginPath(getDef().name(), name, type)`.

## Old backend asset code

`Backend/GameServer/.../cardapi/CardAssets` (with `AssetElement`, `ProfessionAsset`,
`RarityAsset`, `CardAssetType`) still creates an empty asset tree in
`Backend/data/cards/assets/` at startup (gitignored) and resolves local `File`s. With
`Server/Assets` and the client cache, nothing needs it any more. It also has a casing bug:
files are created lower-case but looked up with `name()` (upper-case). It's left untouched
until it's decided whether to remove it.
