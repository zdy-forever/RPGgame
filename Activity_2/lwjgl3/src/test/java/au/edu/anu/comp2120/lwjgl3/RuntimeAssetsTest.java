package au.edu.anu.comp2120.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import au.edu.anu.comp2120.audio.BackgroundMusic;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the real desktop runtime classpath, including LibGDX internal-file fallback.
 * No working-directory override, hard-coded checkout path or graphics context is needed.
 * @author Daoyan Zhu (u7782042)
 */
final class RuntimeAssetsTest {
    /** PNG signature shared by every imported runtime image. */
    private static final byte[] PNG_SIGNATURE = {(byte)137,80,78,71,13,10,26,10};

    /** Creates an independent resource-loading test without initializing OpenGL. */
    RuntimeAssetsTest() { }

    /**
     * Reproduces the formerly failing forge lookup with no art folder in the working directory.
     * @throws IOException if the packaged resource cannot be read
     */
    @Test void internalLookupWorksOutsideAssetsDirectory() throws IOException {
        String path = "art/hub/props/forge.png";
        assertFalse(Files.exists(Path.of(path)), "This regression must exercise classpath fallback");
        assertNotNull(Lwjgl3Launcher.class.getResource("/" + path));
        try (var stream = new Lwjgl3Files().internal(path).read()) {
            assertArrayEquals(PNG_SIGNATURE,stream.readNBytes(PNG_SIGNATURE.length));
        }
    }

    /**
     * Every manifest PNG is available through the same API used by Texture, not source-tree reads.
     * @throws IOException if packaged metadata or a PNG cannot be read
     */
    @Test void entireArtManifestIsOnRuntimeClasspath() throws IOException {
        var files = new Lwjgl3Files();
        var paths = new HashSet<String>();
        try (var reader = new BufferedReader(new InputStreamReader(
                files.internal("art/ASSET_MANIFEST.csv").read(),StandardCharsets.UTF_8))) {
            assertEquals("path,source_media,width,height",reader.readLine());
            for (String row; (row=reader.readLine())!=null;) {
                String path = row.split(",")[0];
                assertTrue(paths.add(path),"Duplicate: " + path);
                assertNotNull(Lwjgl3Launcher.class.getResource("/"+path),path);
                try (var stream = files.internal(path).read()) {
                    assertArrayEquals(PNG_SIGNATURE,stream.readNBytes(PNG_SIGNATURE.length),path);
                }
            }
        }
        assertEquals(232,paths.size());
    }

    /** Removed map pictures and import staging are not accidentally reintroduced into the runtime. */
    @Test void excludedArtIsNotPackaged() {
        assertNull(Lwjgl3Launcher.class.getResource("/art/maps/hub/hub_main.png"));
        assertNull(Lwjgl3Launcher.class.getResource("/art/maps/hub/achievement_corridor.png"));
        assertNull(Lwjgl3Launcher.class.getResource("/art_resources/ASSET_MANIFEST.csv"));
    }

    /**
     * All renamed WAV files are on the runtime classpath with valid RIFF/WAVE headers.
     * @throws IOException if a packaged audio stream cannot be read
     */
    @Test void renamedMusicIsPackagedForInternalLoading() throws IOException {
        var files = new Lwjgl3Files();
        for (var track : BackgroundMusic.Track.values()) {
            assertNotNull(Lwjgl3Launcher.class.getResource("/" + track.path()));
            try (var stream = files.internal(track.path()).read()) {
                byte[] header = stream.readNBytes(12);
                assertEquals(12, header.length);
                assertEquals("RIFF", new String(header, 0, 4, StandardCharsets.US_ASCII));
                assertEquals("WAVE", new String(header, 8, 4, StandardCharsets.US_ASCII));
            }
        }
    }
}
