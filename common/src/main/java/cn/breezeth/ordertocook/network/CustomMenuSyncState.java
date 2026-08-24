package cn.breezeth.ordertocook.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.function.Consumer;

/** Client-side session state. Loader-specific networking feeds manifests and chunks into this class. */
public final class CustomMenuSyncState {
    public enum Status { IDLE, SYNCING, COMPLETE, FAILED }
    private static volatile Status status = Status.IDLE;
    private static volatile int percent;
    private static String currentHash = "";
    private static String incomingHash;
    private static int rawSize;
    private static int compressedSize;
    private static byte[][] chunks;
    private static BitSet received;

    public static synchronized String beginRequest() {
        status = Status.SYNCING;
        percent = 0;
        clearIncoming();
        return currentHash;
    }

    public static synchronized void manifest(String hash, int expectedRawSize, int compressedSize, int chunkCount, boolean unchanged, boolean failed) {
        if (failed || hash == null || hash.length() != 64 || expectedRawSize < 0 || expectedRawSize > CustomMenuSyncCodec.MAX_RAW_BYTES
                || compressedSize < 0 || compressedSize > CustomMenuSyncCodec.MAX_COMPRESSED_BYTES
                || chunkCount < 0 || chunkCount > CustomMenuSyncCodec.MAX_CHUNKS
                || (!unchanged && (compressedSize <= 0 || chunkCount <= 0
                || chunkCount != (compressedSize + CustomMenuSyncCodec.CHUNK_SIZE - 1) / CustomMenuSyncCodec.CHUNK_SIZE))) {
            fail();
            return;
        }
        if (unchanged) {
            currentHash = hash;
            status = Status.COMPLETE;
            percent = 100;
            clearIncoming();
            return;
        }
        incomingHash = hash;
        rawSize = expectedRawSize;
        CustomMenuSyncState.compressedSize = compressedSize;
        chunks = new byte[chunkCount][];
        received = new BitSet(chunkCount);
        status = Status.SYNCING;
        percent = 0;
    }

    public static synchronized void chunk(String hash, int index, byte[] data, Consumer<String> apply) {
        if (status != Status.SYNCING || incomingHash == null || !incomingHash.equals(hash) || chunks == null
                || index < 0 || index >= chunks.length || data == null || data.length > CustomMenuSyncCodec.CHUNK_SIZE) {
            fail();
            return;
        }
        if (!received.get(index)) {
            chunks[index] = data;
            received.set(index);
        }
        percent = chunks.length == 0 ? 100 : received.cardinality() * 100 / chunks.length;
        if (received.cardinality() != chunks.length) return;
        try {
            ByteArrayOutputStream joined = new ByteArrayOutputStream();
            for (byte[] part : chunks) {
                if (part == null || joined.size() + part.length > CustomMenuSyncCodec.MAX_COMPRESSED_BYTES) throw new IOException("Invalid custom menu chunks");
                joined.write(part);
            }
            if (joined.size() != compressedSize) throw new IOException("Custom menu compressed size mismatch");
            String json = CustomMenuSyncCodec.decode(joined.toByteArray(), rawSize, incomingHash);
            apply.accept(json);
            currentHash = incomingHash;
            status = Status.COMPLETE;
            percent = 100;
            clearIncoming();
        } catch (Throwable error) {
            fail();
        }
    }

    public static synchronized void invalidate(String hash) {
        if (hash != null && !hash.equals(currentHash)) status = Status.IDLE;
    }

    public static Status status() { return status; }
    public static int percent() { return percent; }
    public static boolean isSyncing() { return status == Status.SYNCING; }
    public static synchronized String currentHash() { return currentHash; }

    private static void fail() { status = Status.FAILED; percent = 0; clearIncoming(); }
    private static void clearIncoming() { incomingHash = null; rawSize = 0; compressedSize = 0; chunks = null; received = null; }
    private CustomMenuSyncState() {}
}
