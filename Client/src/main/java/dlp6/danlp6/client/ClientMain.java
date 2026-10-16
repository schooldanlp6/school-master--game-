// Author: danlp6
package dlp6.danlp6.client;

import java.util.Arrays;

/**
 * Client entry point. For now only the asset side exists (the game connection follows API.md once the
 * backend has a network layer).
 * <pre>
 *   ClientMain prefetch            refresh index.json and download every asset that isn't cached
 *   ClientMain get &lt;path&gt;...       print the cached file for each asset path ("missing" if unavailable)
 * </pre>
 * Configured with ASSET_BASE_URLS and ASSET_CACHE_DIR, see {@link AssetClient#fromEnv()}.
 */
public final class ClientMain {

    public static void main(String[] args) {
        if (args.length == 0 || !(args[0].equals("prefetch") || args[0].equals("get") && args.length > 1)) {
            System.err.println("usage: ClientMain prefetch | get <path>...");
            System.exit(2);
        }
        AssetClient assets = AssetClient.fromEnv();
        boolean fresh = assets.refreshIndex();
        System.out.println("index: " + (fresh ? "fresh from " + assets.getBaseUrls() : "cached (mirrors unreachable)")
                + ", " + assets.getIndex().size() + " assets, cache " + assets.getCacheDir());

        if (args[0].equals("prefetch")) {
            System.out.println(assets.prefetchAll() + " of " + assets.getIndex().size() + " assets available");
            return;
        }
        for (String path : Arrays.copyOfRange(args, 1, args.length)) {
            if (!AssetClient.isValidPath(path)) {
                System.out.println("invalid  " + path);
                continue;
            }
            System.out.println(assets.get(path).map(p -> "ok       " + path + " -> " + p).orElse("missing  " + path));
        }
    }
}
