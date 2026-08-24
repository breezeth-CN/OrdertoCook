package cn.breezeth.ordertocook.network;

import java.io.IOException;

public final class CustomMenuSyncServerCache {
    private static String cachedJson;
    private static CustomMenuSyncCodec.Prepared cached;

    public static synchronized CustomMenuSyncCodec.Prepared prepare(String json) throws IOException {
        if (cached == null || !json.equals(cachedJson)) {
            cached = CustomMenuSyncCodec.prepare(json);
            cachedJson = json;
        }
        return cached;
    }

    public static synchronized String hash(String json) throws IOException { return prepare(json).hash(); }
    private CustomMenuSyncServerCache() {}
}
