package au.edu.anu.comp2120.audio;

import au.edu.anu.comp2120.world.MapType;
import com.badlogic.gdx.audio.Music;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies looping, map/progress selection and uninterrupted same-track travel without an audio device.
 * @author Daoyan Zhu (u7782042)
 */
final class BackgroundMusicTest {
    /** Factory invocations, exposing accidental per-frame or per-travel reloads. */
    private final List<String> loads = new ArrayList<>();
    /** Per-resource playback/lifecycle commands received by the backend. */
    private final Map<String, List<String>> commands = new HashMap<>();
    /** Global command order, used to verify previous playback stops before another song begins. */
    private final List<String> events = new ArrayList<>();
    /** Actual controller with a native-free, command-recording factory. */
    private final BackgroundMusic music = new BackgroundMusic(this::load);

    /** Creates independent music/controller observations per test. */
    BackgroundMusicTest() { }

    /**
     * All map/progress combinations select the specified soundtrack.
     * @param map map role
     * @param complete whether all stones are collected
     * @param track expected soundtrack
     */
    @ParameterizedTest
    @CsvSource({"HUB,false,HUB", "MEMORIAL,false,HUB", "HUB,true,VICTORY", "MEMORIAL,true,VICTORY",
            "TRAINING,false,BATTLE", "BATTLE,false,BATTLE", "TRAINING,true,BATTLE", "BATTLE,true,BATTLE"})
    void selectsExpectedSong(MapType map, boolean complete, BackgroundMusic.Track track) {
        assertEquals(track, BackgroundMusic.select(map, complete));
        music.update(map, complete);
        assertEquals(List.of(track.path()), loads);
        assertEquals(List.of("setLooping", "setVolume", "play"), commands.get(track.path()));
    }

    /**
     * Repeated Hub/Hall travel sends no stop/play/seek/reload commands, before or after completion.
     * @param complete whether the shared peaceful track is victory music
     */
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void hubHallTravelNeverInterruptsSameTrack(boolean complete) {
        music.update(MapType.HUB, complete);
        var before = List.copyOf(events);
        for (int i = 0; i < 100; i++) {
            music.update(MapType.MEMORIAL, complete);
            music.update(MapType.HUB, complete);
        }
        assertEquals(before, events);
        assertEquals(1, loads.size());
    }

    /** Battle has priority even after victory; Training/Battle transitions also keep a shared stream. */
    @Test void combatTrackIsSharedAndPreviousMusicStopsFirst() {
        music.update(MapType.HUB, true);
        music.update(MapType.TRAINING, true);
        var before = List.copyOf(events);
        music.update(MapType.BATTLE, true);
        assertEquals(before, events);
        assertTrue(events.indexOf(BackgroundMusic.Track.VICTORY.path() + ":stop")
                < events.indexOf(BackgroundMusic.Track.BATTLE.path() + ":play"));
        music.update(MapType.MEMORIAL, true);
        assertEquals(2, loads.size());
        assertEquals(2, commands.get(BackgroundMusic.Track.VICTORY.path()).stream().filter("play"::equals).count());
        assertEquals("stop", commands.get(BackgroundMusic.Track.BATTLE.path()).get(3));
    }

    /** The newly collected fifth stone changes a peaceful song once, without needing ending playback. */
    @Test void fifthStoneSwitchesToVictoryOnce() {
        music.update(MapType.HUB, false);
        music.update(MapType.HUB, true);
        music.update(MapType.MEMORIAL, true);
        music.update(MapType.HUB, true);
        assertEquals(List.of(BackgroundMusic.Track.HUB.path(), BackgroundMusic.Track.VICTORY.path()), loads);
        assertEquals(List.of("setLooping", "setVolume", "play", "stop"), commands.get(BackgroundMusic.Track.HUB.path()));
        assertEquals(List.of("setLooping", "setVolume", "play"), commands.get(BackgroundMusic.Track.VICTORY.path()));
    }

    /** Application pause uses pause/play, never stop/seek; repeated lifecycle calls are idempotent. */
    @Test void pauseAndResumeRetainPosition() {
        music.update(MapType.HUB, false);
        music.pause(); music.pause();
        music.update(MapType.MEMORIAL, false);
        music.resume(); music.resume();
        assertEquals(List.of("setLooping", "setVolume", "play", "pause", "play"),
                commands.get(BackgroundMusic.Track.HUB.path()));
    }

    /** A song selected while paused starts only when the application resumes. */
    @Test void pausedSelectionDoesNotStartAudioEarly() {
        music.pause();
        music.update(MapType.HUB, false);
        assertFalse(commands.get(BackgroundMusic.Track.HUB.path()).contains("play"));
        music.update(MapType.BATTLE, false);
        assertFalse(commands.get(BackgroundMusic.Track.BATTLE.path()).contains("play"));
        music.resume();
        assertEquals("play", commands.get(BackgroundMusic.Track.BATTLE.path()).get(2));
        assertFalse(commands.get(BackgroundMusic.Track.HUB.path()).contains("play"));
    }

    /** Only loaded streams are disposed, exactly once; later updates cannot resurrect native resources. */
    @Test void disposalReleasesEveryStreamOnce() {
        music.update(MapType.HUB, false);
        music.update(MapType.BATTLE, false);
        music.update(MapType.HUB, true);
        music.dispose(); music.dispose();
        assertEquals(3, loads.size());
        commands.values().forEach(log -> assertEquals(1, log.stream().filter("dispose"::equals).count()));
        assertThrows(IllegalStateException.class, () -> music.update(MapType.HUB, false));
        assertThrows(IllegalStateException.class, music::pause);
        assertThrows(IllegalStateException.class, music::resume);
    }

    /** Missing map/factory data is rejected, and an unopened player allocates/disposes nothing. */
    @Test void validatesInputsAndLoadsLazily() {
        assertTrue(loads.isEmpty());
        assertThrows(NullPointerException.class, () -> new BackgroundMusic(null));
        assertThrows(NullPointerException.class, () -> BackgroundMusic.select(null, false));
        assertThrows(NullPointerException.class, () -> music.update(null, false));
        music.resume(); music.pause(); music.dispose();
        assertTrue(events.isEmpty());
    }

    /** Object methods obey identity semantics without polluting the recorded audio commands. */
    @Test void proxyObjectMethodsReturnValidValues() {
        Music first = load(BackgroundMusic.Track.HUB.path());
        Music second = load(BackgroundMusic.Track.BATTLE.path());
        Predicate<Object> equals = first::equals;
        assertTrue(equals.test(first));
        assertFalse(equals.test(second));
        assertFalse(equals.test(null));
        assertEquals(System.identityHashCode(first), first.hashCode());
        assertEquals("MusicProxy[" + BackgroundMusic.Track.HUB.path() + "]", first.toString());
        assertThrows(UnsupportedOperationException.class, first::getPosition);
        assertTrue(events.isEmpty());
        assertTrue(commands.values().stream().allMatch(List::isEmpty));
    }

    /**
     * Creates a Music proxy with identity-based Object methods and command-only audio recording.
     * Unsupported non-void audio queries fail explicitly instead of returning null for a primitive.
     * @param path internal resource selected by the actual factory boundary
     * @return command-recording Music implementation without native buffers or sound output
     */
    private Music load(String path) {
        loads.add(path);
        List<String> log = new ArrayList<>();
        commands.put(path, log);
        return (Music) Proxy.newProxyInstance(Music.class.getClassLoader(), new Class<?>[] {Music.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "equals" -> { return proxy == args[0]; }
                        case "hashCode" -> { return System.identityHashCode(proxy); }
                        case "toString" -> { return "MusicProxy[" + path + "]"; }
                        default -> { }
                    }
                    if (method.getReturnType() != void.class) {
                        throw new UnsupportedOperationException("Unsupported music query: " + method.getName());
                    }
                    if (method.getName().equals("setLooping")) { assertEquals(true, args[0]); }
                    if (method.getName().equals("setVolume")) { assertEquals(BackgroundMusic.VOLUME, args[0]); }
                    log.add(method.getName());
                    events.add(path + ":" + method.getName());
                    return null;
                });
    }
}
