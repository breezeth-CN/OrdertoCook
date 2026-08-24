package cn.breezeth.ordertocook.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Shared, loader-independent wire format helpers for custom-menu synchronization. */
public final class CustomMenuSyncCodec {
    public static final int PROTOCOL_VERSION = 1;
    public static final int CHUNK_SIZE = 24 * 1024;
    public static final int MAX_RAW_BYTES = 8 * 1024 * 1024;
    public static final int MAX_COMPRESSED_BYTES = 2 * 1024 * 1024;
    public static final int MAX_CHUNKS = 128;

    public record Prepared(String hash, int rawSize, byte[] compressed, int chunkCount) {
        public byte[] chunk(int index) {
            if (index < 0 || index >= chunkCount) throw new IndexOutOfBoundsException(index);
            int start = index * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, compressed.length - start);
            return java.util.Arrays.copyOfRange(compressed, start, start + length);
        }
    }

    public static Prepared prepare(String json) throws IOException {
        byte[] raw = json.getBytes(StandardCharsets.UTF_8);
        if (raw.length > MAX_RAW_BYTES) throw new IOException("Custom menu exceeds raw limit: " + raw.length);
        byte[] compressed = gzip(raw);
        if (compressed.length > MAX_COMPRESSED_BYTES) throw new IOException("Custom menu exceeds compressed limit: " + compressed.length);
        int chunks = Math.max(1, (compressed.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
        if (chunks > MAX_CHUNKS) throw new IOException("Custom menu exceeds chunk limit: " + chunks);
        return new Prepared(sha256(raw), raw.length, compressed, chunks);
    }

    public static String decode(byte[] compressed, int expectedRawSize, String expectedHash) throws IOException {
        if (compressed.length > MAX_COMPRESSED_BYTES) throw new IOException("Compressed custom menu is too large");
        if (expectedRawSize < 0 || expectedRawSize > MAX_RAW_BYTES) throw new IOException("Invalid custom menu size");
        byte[] raw = gunzip(compressed, expectedRawSize);
        if (raw.length != expectedRawSize) throw new IOException("Custom menu size mismatch");
        if (!sha256(raw).equalsIgnoreCase(expectedHash)) throw new IOException("Custom menu hash mismatch");
        return new String(raw, StandardCharsets.UTF_8);
    }

    public static String sha256(String json) {
        return sha256(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static byte[] gzip(byte[] raw) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(raw.length, 64 * 1024));
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(raw);
        }
        return output.toByteArray();
    }

    private static byte[] gunzip(byte[] compressed, int expectedRawSize) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(expectedRawSize, 64 * 1024));
        byte[] buffer = new byte[8192];
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            int read;
            while ((read = gzip.read(buffer)) >= 0) {
                if (read == 0) continue;
                if (output.size() + read > MAX_RAW_BYTES) throw new IOException("Inflated custom menu is too large");
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }

    private CustomMenuSyncCodec() {}
}
