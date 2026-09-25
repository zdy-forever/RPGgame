package au.edu.anu.comp2120.audio;

import au.edu.anu.comp2120.world.MapType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Screen-lifetime owner of streamed background music, independent of map/encounter lifetimes.
 * Re-selecting the same track is a no-op, so Hub/Hall travel never resets its playback position.
 * Calls belong on the game-loop thread; progression is supplied as a read-only completed-stones flag.
 * @author Daoyan Zhu (u7782042)
 * @author Hao Liu (u7998420)
 */
public final class BackgroundMusic implements Disposable {
    /** Central default gain; does not change source files or system audio settings. */
    public static final float VOLUME = 0.5f;

    /**
     * Three shipped streaming tracks with portable internal-resource paths.
     * @author Daoyan Zhu (u7782042)
     */
    public enum Track {
        /** Peaceful areas before all five guardian stones have been collected. */
        HUB("audio/hub_theme.wav"),
        /** Both practice and hostile encounters, regardless of campaign progress. */
        BATTLE("audio/battle_theme.wav"),
        /** Hub and Memorial Hall after all stones, without waiting for the ending dialogue. */
        VICTORY("audio/victory_theme.wav");

        /** Internal runtime path, independent of the working directory. */
        private final String path;

        /**
         * Defines one shipped track.
         * @param path portable asset path
         */
        Track(String path) { this.path = path; }

        /**
         * Exposes the packaged resource path for loading and packaging checks.
         * @return internal WAV resource path
         */
        public String path() { return path; }
    }

    /** Streaming factory; injection lets tests observe commands without a sound device. */
    private final Function<String, Music> loader;
    /** At most three owned stream objects, lazily loaded and reused on later visits. */
    private final Map<Track, Music> streams = new EnumMap<>(Track.class);
    /** Last selected track, or null before the first update. */
    private Track current;
    /** Whether the application has suspended playback. */
    private boolean paused;
    /** Prevents reopening disposed streams and repeated native disposal. */
    private boolean disposed;
    /** Current application music gain, independent of saved gameplay. */
    private float volume = VOLUME;

    /** Creates a lazy LibGDX streaming player after application audio initialization. */
    public BackgroundMusic() {
        this(path -> Gdx.audio.newMusic(Gdx.files.internal(path)));
    }

    /**
     * Creates a player with an injected streaming factory, without loading or playing anything yet.
     * @param loader non-null factory returning a distinct owned stream for each resource
     * @throws NullPointerException if loader is null
     */
    public BackgroundMusic(Function<String, Music> loader) {
        this.loader = Objects.requireNonNull(loader, "music loader");
    }

    /**
     * Resolves the track without graphics, audio or mutable quest state.
     * @param mapType current map role
     * @param allStones whether all five distinct guardians have been defeated
     * @return battle music for Training/Battle; otherwise Hub or victory music
     * @throws NullPointerException if mapType is null
     */
    public static Track select(MapType mapType, boolean allStones) {
        return switch (Objects.requireNonNull(mapType, "map type")) {
            case TRAINING, BATTLE -> Track.BATTLE;
            case HUB, MEMORIAL -> allStones ? Track.VICTORY : Track.HUB;
        };
    }

    /**
     * Changes tracks only when the selected song changes; repeated frames/travel never restart it.
     * Loads the new stream before stopping the previous one; only one stream may play at a time.
     * A different song starts from its beginning; all songs loop through the streaming backend.
     * @param mapType current map role after gameplay and quest settlement
     * @param allStones whether all five stones are collected, not whether the ending was viewed
     * @throws NullPointerException if mapType or a factory result is null
     * @throws IllegalStateException if already disposed
     */
    public void update(MapType mapType, boolean allStones) {
        requireOpen();
        Track next = select(mapType, allStones);
        if (next == current) { return; }
        Music music = streams.computeIfAbsent(next, track -> {
            Music loaded = Objects.requireNonNull(loader.apply(track.path()), "music stream");
            loaded.setLooping(true);
            loaded.setVolume(volume);
            return loaded;
        });
        if (current != null) { streams.get(current).stop(); }
        current = next;
        if (!paused) { music.play(); }
    }

    /** Returns the current music gain.
     * @return gain between zero and one
     */
    public float getVolume() { return volume; }

    /** Changes every loaded stream and the default for future tracks without restarting playback.
     * @param value finite gain between zero and one
     * @throws IllegalArgumentException if outside the allowed range
     * @throws IllegalStateException if disposed
     */
    public void setVolume(float value) {
        requireOpen();
        if (!Float.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Volume must be between 0 and 1");
        }
        volume = value;
        streams.values().forEach(stream -> stream.setVolume(value));
    }

    /**
     * Suspends playback without resetting the current song's position; repeated pauses are harmless.
     * @throws IllegalStateException if already disposed
     */
    public void pause() {
        requireOpen();
        if (paused) { return; }
        paused = true;
        if (current != null) { streams.get(current).pause(); }
    }

    /**
     * Continues the selected stream after an application pause without restarting it.
     * @throws IllegalStateException if already disposed
     */
    public void resume() {
        requireOpen();
        if (!paused) { return; }
        paused = false;
        if (current != null) { streams.get(current).play(); }
    }

    /** Stops and releases each loaded native stream once; unloaded songs allocate no resources. */
    @Override public void dispose() {
        if (disposed) { return; }
        disposed = true;
        streams.values().forEach(Music::dispose);
        streams.clear();
        current = null;
    }

    /**
     * Rejects late lifecycle/update calls after the owning screen has been destroyed.
     * @throws IllegalStateException if already disposed
     */
    private void requireOpen() {
        if (disposed) { throw new IllegalStateException("Background music is disposed"); }
    }
}
