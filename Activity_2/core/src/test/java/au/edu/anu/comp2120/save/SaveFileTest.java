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
 */
final class SaveFileTest {
    /** The destination is normalized to an absolute path. */
    @Test void normalizesToAbsolutePath(@TempDir Path dir) {
        var file = new SaveFile(dir.resolve("slot.properties"));
        assertEquals(file.path().toAbsolutePath(), file.path());
    }

    /** A null destination is rejected. */
    @Test void rejectsNullPath() {
        assertThrows(NullPointerException.class, () -> new SaveFile(null));
    }

    /** A written snapshot round-trips and omits reserved markers. */
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

    /** Null field maps are rejected. */
    @Test void writeRejectsNullFields() {
        var file = new SaveFile(Path.of("x.properties"));
        assertThrows(NullPointerException.class, () -> file.write(null));
    }

    /** Blank keys and reserved save.* keys are rejected. */
    @Test void writeRejectsBlankAndReservedKeys(@TempDir Path dir) {
        var file = new SaveFile(dir.resolve("s.properties"));
        assertThrows(IllegalArgumentException.class, () -> file.write(Map.of(" ", "v")));
        assertThrows(IllegalArgumentException.class, () -> file.write(Map.of("save.version", "1")));
    }

    /** Reading a missing file fails. */
    @Test void readMissingFileFails(@TempDir Path dir) {
        var file = new SaveFile(dir.resolve("missing.properties"));
        assertThrows(IOException.class, file::read);
    }

    /** A file without the format marker is rejected as unsupported. */
    @Test void readRejectsUnsupportedFormat(@TempDir Path dir) throws IOException {
        var path = dir.resolve("bad.properties");
        Files.writeString(path, "level=1\n");
        var file = new SaveFile(path);
        assertThrows(IOException.class, file::read);
    }
}
