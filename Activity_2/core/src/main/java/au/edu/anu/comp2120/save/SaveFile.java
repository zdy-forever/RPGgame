package au.edu.anu.comp2120.save;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serial;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Reads and atomically writes versioned, bounded property snapshots with Unicode escaping.
 * This class never touches live gameplay state; callers validate decoded fields before restoring.
 * No Java object deserialization or graphics context is required.
 * @author Hao Liu (u7998420)
 */
public final class SaveFile {
    /** Current on-disk schema version. */
    public static final int VERSION = 1;
    /** Bounded file size prevents an invalid file from allocating unbounded memory. */
    private static final int MAX_BYTES = 1024 * 1024;
    /** Reserved format marker, distinct from gameplay fields. */
    private static final String FORMAT_KEY = "save.format";
    /** Reserved version marker. */
    private static final String VERSION_KEY = "save.version";
    /** Stable format name used to reject unrelated files. */
    private static final String FORMAT = "comp2120-game";
    /** Explicit file destination, independent of working directory conventions. */
    private final Path path;

    /** Creates a file boundary without reading or writing anything.
     * @param path destination for a single save slot
     * @throws NullPointerException if path is null
     */
    public SaveFile(Path path) {
        this.path = Objects.requireNonNull(path, "save path").toAbsolutePath().normalize();
    }

    /** Reads the actual destination for UI feedback.
     * @return absolute normalized save path
     */
    public Path path() { return path; }

    /** Encodes a detached snapshot and atomically replaces the slot after a complete write.
     * Unsupported atomic replacement is reported as failure; the previous slot is retained.
     * @param fields validated gameplay fields without reserved save markers
     * @throws IOException if encoding, writing, syncing or atomic replacement fails
     * @throws IllegalArgumentException if fields are oversized, blank or use reserved keys
     * @throws NullPointerException if fields or an entry is null
     */
    public void write(Map<String, String> fields) throws IOException {
        Properties properties = new Properties();
        Objects.requireNonNull(fields, "fields").forEach((key, value) -> {
            Objects.requireNonNull(key, "field name");
            Objects.requireNonNull(value, "field value");
            if (key.isBlank() || key.startsWith("save.")) {
                throw new IllegalArgumentException("Invalid or reserved save field: " + key);
            }
            properties.setProperty(key, value);
        });
        properties.setProperty(FORMAT_KEY, FORMAT);
        properties.setProperty(VERSION_KEY, Integer.toString(VERSION));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        properties.store(buffer, "COMP2120 game state");
        byte[] bytes = buffer.toByteArray();
        if (bytes.length > MAX_BYTES) { throw new IllegalArgumentException("Save snapshot is too large"); }
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), ".game-save-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer data = ByteBuffer.wrap(bytes);
                while (data.hasRemaining()) {
                    if (channel.write(data) == 0) {
                        throw new IOException("Save write made no progress");
                    }
                }
                channel.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Decodes a versioned file without mutating any game object.
     * @return immutable gameplay fields; format markers are checked and omitted
     * @throws IOException if the file is missing, unreadable, malformed or unsupported
     */
    public Map<String, String> read() throws IOException {
        byte[] bytes;
        try (InputStream input = Files.newInputStream(path)) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        }
        if (bytes.length > MAX_BYTES) { throw new IOException("Save file exceeds the size limit"); }
        Properties properties = new UniqueProperties();
        try {
            properties.load(new ByteArrayInputStream(bytes));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Malformed save file", invalid);
        }
        if (!FORMAT.equals(properties.getProperty(FORMAT_KEY))) {
            throw new IOException("Not a supported game save file");
        }
        if (!Integer.toString(VERSION).equals(properties.getProperty(VERSION_KEY))) {
            throw new IOException("Unsupported save version: " + properties.getProperty(VERSION_KEY));
        }
        Map<String, String> fields = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.equals(FORMAT_KEY) || key.equals(VERSION_KEY)) { continue; }
            if (key.isBlank() || key.startsWith("save.")) {
                throw new IOException("Invalid save field: " + key);
            }
            fields.put(key, properties.getProperty(key));
        }
        return Map.copyOf(fields);
    }

    /** Rejects ambiguous duplicate fields instead of silently accepting the last value.
     * @author Hao Liu (u7998420)
     */
    private static final class UniqueProperties extends Properties {
        /** Serialization identifier inherited from Properties; object serialization is not used. */
        @Serial
        private static final long serialVersionUID = 1L;

        /** Creates an empty duplicate-checking property set. */
        private UniqueProperties() { }

        /** Inserts each parsed field only once.
         * @param key property name
         * @param value property value
         * @return previous value, always null for valid insertion
         * @throws IllegalArgumentException if key already exists
         */
        @Override
        public synchronized Object put(Object key, Object value) {
            if (containsKey(key)) { throw new IllegalArgumentException("Duplicate save field: " + key); }
            return super.put(key, value);
        }
    }
}
