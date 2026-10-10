# school-master--game-

A card game built around school life. The project is a Maven multi-module setup:

| Module    | Purpose                                                                                     |
|-----------|---------------------------------------------------------------------------------------------|
| `CardAPI` | Card model and parser: reads `CardMeta.json` into `BattleCardMeta` / `EquipmentCardMeta`.     |
| `Server`  | Bootstraps the `data/` folder, loads the card metadata, and creates the asset folder tree.     |
| `data/`   | Runtime data: card definitions (`data/cards/CardMeta.json`) and the asset tree.               |

## Assets server

Assets (pictures, sounds) are served by a **standalone static file server** that is defined
only by its folder. You don't need any server code. Point any static web server at the asset
root and it works:

```sh
# Python (dev)
python3 -m http.server 8080 --directory data/cards/assets

# nginx
location /assets/ { alias /path/to/data/cards/assets/; }

# Tomcat: drop/symlink the folder into webapps/, or add a Context
<Context path="/assets" docBase="/path/to/data/cards/assets" />
```

Since the server is just a folder, the **folder layout is the API**. A client builds the
URL from the asset type, the asset group and the lower-cased enum name:

```
data/cards/assets/
├── index.json               # path -> size + sha256 (generated)
├── picture/                 # CardAssetType.PICTURE -> .png
│   ├── cards/               # one per card id in CardMeta.json
│   │   ├── robert.png
│   │   └── …
│   ├── professions/         # one per Profession enum value
│   │   ├── sport.png
│   │   ├── wissenschaften.png
│   │   ├── gesellschafts_lehre.png
│   │   ├── kunst.png
│   │   └── staff.png
│   └── rarity/              # one per Rarity enum value
│       ├── basic.png
│       ├── advanced.png
│       └── elite.png
└── sound/                   # CardAssetType.SOUND -> .mp3
    └── rarity/
        ├── basic.mp3
        ├── advanced.mp3
        └── elite.mp3
```

e.g. `GET /assets/picture/rarity/elite.png`

The tree is **derived**, never hand-made: `data/cards/assets.defs.json` lists the asset
groups and their types and points at the sources of truth (the `Profession`/`Rarity` enums
and `CardMeta.json`). See [DEFINITIONS.md](DEFINITIONS.md) for the full definition format
and how to use `AssetAdapter` in code.

### Asset housekeeping (Makefile)

The asset tree stays in sync with the code without the Java server running. The targets
call `dlp6/danlp6/assets.py` (Python 3, stdlib only):

| Target              | What it does                                                                                              |
|---------------------|-----------------------------------------------------------------------------------------------------------|
| `make asset-paths`  | **Gather paths.** Derives every expected asset path from the defs and writes `data/cards/assets.manifest`. |
| `make asset-list`   | **Gather assets.** Lists the files that actually exist under the asset root.                              |
| `make asset-gather` | **Gather assets.** Copies matching files from `SRC=<dir>` into the tree (the source can be laid out any way). |
| `make asset-tree`   | **Create the structure.** Creates every folder from the manifest; `PLACEHOLDERS=1` also creates empty files. |
| `make asset-check`  | Reports **missing**, **placeholder** (empty) and **orphan** (not in the manifest) files. `STRICT=1` exits non-zero for CI. |
| `make asset-clean`  | **Clean.** Removes orphans and empty folders. Dry run unless `FORCE=1`; `PRUNE_EMPTY=1` also drops placeholders. |
| `make asset-index`  | Writes `index.json` (size + sha256 per asset) into the root, for client caches and mirrors. |
| `make asset-update` | **Update.** `paths` → `clean` → `tree` → `index`, so the tree matches the code again.                      |
| `make asset-serve`  | Serves the root with `python3 -m http.server` on `PORT` (default 8080).                                  |

Overridable: `ASSET_ROOT`, `ASSET_DEFS`, `MANIFEST`, `PYTHON`, `PORT`.

Typical workflow after adding a new `Rarity`, `Profession` or card:

```sh
make asset-update              # derive, show orphans, create folders, write index.json
make asset-gather SRC=~/art    # pull in the real files
make asset-check               # what's still missing
make asset-clean FORCE=1       # actually drop the orphans
```

Deploying is just copying (or `rsync`ing) `data/cards/assets/` to every mirror or CDN origin.

> **Note:** `CardAssets.createFile` currently writes **lower-case** file names
> (`elite.png`), while `ProfessionAsset/RarityAsset.getAsset` look them up with
> `asset.name()` (**upper-case**, `ELITE.png`). Lookups fail on case-sensitive
> file systems (Linux, nginx, Tomcat). Code should resolve assets through
> `dlp6.danlp6.AssetAdapter`, which always uses lower-case.
