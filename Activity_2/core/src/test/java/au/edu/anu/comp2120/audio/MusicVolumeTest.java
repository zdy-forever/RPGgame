package au.edu.anu.comp2120.audio;

import au.edu.anu.comp2120.world.MapType;
import com.badlogic.gdx.audio.Music;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies volume on current, cached and future streams without native audio.
 * @author Hao Liu (u7998420)
 */
final class MusicVolumeTest {
    /** Latest gain observed by each loaded stream. */
    private final Map<String, Float> gains = new HashMap<>();
    /** Playback actions must not be introduced by volume changes. */
    private final List<String> playback = new ArrayList<>();
    /** Creates independent observations. */
    MusicVolumeTest() { }

    /** Supplies a minimal backend recording gain and playback commands.
     * @param path track path
     * @return native-free stream
     */
    private Music load(String path) {
        return (Music) Proxy.newProxyInstance(Music.class.getClassLoader(), new Class<?>[] {Music.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setVolume")) { gains.put(path, (Float) args[0]); }
                    if (List.of("play", "pause", "stop").contains(method.getName())) {
                        playback.add(path + ":" + method.getName());
                    }
                    return null;
                });
    }

    /** Slider changes reach cached and future songs without seeking or restarting music. */
    @Test void volumeSurvivesTrackChangesAndPause() {
        var music = new BackgroundMusic(this::load);
        music.setVolume(0.23f);
        assertTrue(gains.isEmpty());
        music.update(MapType.HUB, false);
        assertEquals(0.23f, gains.get(BackgroundMusic.Track.HUB.path()));
        var before = List.copyOf(playback);
        music.setVolume(0);
        assertEquals(before, playback);
        music.update(MapType.BATTLE, false);
        assertEquals(0, gains.get(BackgroundMusic.Track.BATTLE.path()));
        music.pause(); before = List.copyOf(playback);
        music.setVolume(1);
        assertEquals(before, playback);
        assertTrue(gains.values().stream().allMatch(gain -> gain == 1));
        music.resume(); music.update(MapType.HUB, true);
        assertEquals(1, gains.get(BackgroundMusic.Track.VICTORY.path()));
        music.update(MapType.HUB, false);
        assertEquals(1, music.getVolume());
        music.dispose();
    }

    /** Invalid values do not change gain and disposed streams cannot be modified. */
    @Test void rejectsInvalidGainWithoutMutation() {
        var music = new BackgroundMusic(this::load);
        for (float invalid : new float[] {-1, 1.01f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> music.setVolume(invalid));
            assertEquals(BackgroundMusic.VOLUME, music.getVolume());
        }
        music.dispose();
        assertThrows(IllegalStateException.class, () -> music.setVolume(0.5f));
    }
}
