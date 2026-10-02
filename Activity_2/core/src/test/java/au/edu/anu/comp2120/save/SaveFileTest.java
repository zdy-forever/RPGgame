package au.edu.anu.comp2120.save;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies atomic, bounded, versioned save-file encoding and decoding.
 * @author COMP2120 Assignment 3 group
 * @author Jiaqi Chen (u7851266)
 */
final class SaveFileTest {
    /** Creates a new {@code SaveFileTest} instance. */
    SaveFileTest() {
        // Test instances are created by JUnit.
    }

    /**
     * Verifies that the destination path is normalized to an absolute path.
     *
     * @param dir the temporary directory supplied by JUnit
     */
    @Test void normalizesToAbsolutePath(@TempDir Path dir) {
        var file = new SaveFile(dir.resolve("slot.properties"));
        assertEquals(file.path().toAbsolutePath(), file.path());
    }

    /** A null destination path is rejected. */
    @Test void rejectsNullPath() {
        assertThrows(NullPointerException.class, () -> new SaveFile(null));
    }

    /**
     * Verifies that a written snapshot round-trips and that reserved markers are omitted.
     *
     * @param dir the temporary directory supplied by JUnit
     * @throws IOException if an I/O error occurs
     */
    @Test void writeThenReadRoundTripsFields(@TempDir Path dir) throws IOException {
        var file = new SaveFile(dir.resolve("slot.properties"));
        file.write(Map.of("level", "12", "name", "hero", "gold", "500"));
        assertTrue(Files.exists(file.path()));
        Map<String, String> read = file.read();
        assertEquals("12", read.get("level"));
        assertEquals("500", read.get("gold"));
        assertEquals(3, read.size());
        assertFalse(read.containsKey("save.format"));
        assertFalse(read.containsKey("save.version"));
    }

    /** Null field maps are rejected when writing. */
    @Test void writeRejectsNullFields() {
        var file = new SaveFile(Path.of("x.properties"));
        assertThrows(NullPointerException.class, () -> file.write(null));
    }

    /**
     * Blank keys and reserved save.* keys are rejected when writing.
     *
     * @param dir the temporary directory supplied by JUnit
     */
    @Test void writeRejectsBlankAndReservedKeys(@TempDir Path dir) {
        var file = new SaveFile(dir.resolve("s.properties"));
        assertThrows(IllegalArgumentException.class, () -> file.write(Map.of(" ", "v")));
        assertThrows(IllegalArgumentException.class, () -> file.write(Map.of("save.version", "1")));
    }

    /**
     * Verifies that reading a missing file fails with an {@link IOException}.
     *
     * @param dir the temporary directory supplied by JUnit
     */
    @Test void readMissingFileFails(@TempDir Path dir) {
        var file = new SaveFile(dir.resolve("missing.properties"));
        assertThrows(IOException.class, file::read);
    }

    /**
     * Verifies that a file without the format marker is rejected as unsupported.
     *
     * @param dir the temporary directory supplied by JUnit
     * @throws IOException if an I/O error occurs
     */
    @Test void readRejectsUnsupportedFormat(@TempDir Path dir) throws IOException {
        var path = dir.resolve("bad.properties");
        Files.writeString(path, "level=1\n");
        var file = new SaveFile(path);
        assertThrows(IOException.class, file::read);
    }
}
