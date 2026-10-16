<!-- Author: danlp6 -->
# Client

A stateless client: inputs go to the backend, and what the backend sends gets rendered. It
shares no code with `Server/`; the contracts are in [API.md](../API.md).

## Assets: `dlp6.danlp6.client.AssetClient`

Fetches assets from the Assets CDN and its mirrors and caches them on disk, following
[API.md](../API.md#client-behaviour-normative-implemented-by-assetclient):

```java
AssetClient assets = AssetClient.fromEnv();      // or new AssetClient(List.of("https://cdn…/", "http://mirror…/"), cacheDir)
assets.refreshIndex();                           // index.json from the first mirror that answers, else the cached one

// the backend sends paths like this one
Optional<Path> file = assets.get("picture/rarity/elite.png");
file.ifPresentOrElse(this::show, this::showPlaceholder);   // empty = not available, never an exception

assets.prefetchAll();                            // e.g. on a loading screen
```

- Cached files are checked against the size and sha256 in `index.json`. A changed asset is
  downloaded again, and a download that doesn't match is rejected and the next mirror is tried.
- With every mirror down, the client keeps working from the cache. An outdated copy is
  better than none.
- Paths that don't follow the asset path rule (e.g. `../x`, upper case) throw
  `IllegalArgumentException`. Invalid entries in a broken or malicious `index.json` are ignored.
- `get` blocks while downloading, so call it off the render thread.

| Environment variable | Default                                                    |
|----------------------|------------------------------------------------------------|
| `ASSET_BASE_URLS`    | `http://localhost:8080/` (comma separated, CDN first)      |
| `ASSET_CACHE_DIR`    | `$XDG_CACHE_HOME/school-master/assets`, else `~/.cache/school-master/assets` |

## Command line

`dlp6.danlp6.client.ClientMain` (needs gson on the classpath):

```sh
ClientMain prefetch                                   # refresh the index, download everything missing
ClientMain get picture/rarity/elite.png sound/…       # print the cached file per path, or "missing"
```

Local test against the dev server: `make -C Server asset-serve`, then run the client with
`ASSET_BASE_URLS=http://localhost:8080/`.
