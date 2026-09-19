package au.edu.anu.comp2120.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import au.edu.anu.comp2120.world.CollisionBox;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lazily owned pixel-art textures and horizontal strips, used exclusively by rendering.
 * Neither sprite dimensions nor animation clocks participate in collision or damage.
 * @author Daoyan Zhu (u7782042)
 */
public final class ArtAssets implements Disposable {
    /** Frame counts explicitly supplied in character strip filenames. */
    private static final Pattern FRAMES = Pattern.compile("_(\\d+)f\\.png$");
    /** GPU textures loaded at most once per screen lifetime. */
    private final Map<String, Texture> textures = new HashMap<>();
    /** Immutable-region strips; flipped rendering never mutates shared frames. */
    private final Map<String, TextureRegion[]> strips = new HashMap<>();
    /** Presentation frame duration, independent of attack active windows. */
    public static final double FRAME_SECONDS = 0.10;

    /** Creates an empty cache; the caller must dispose it before losing the graphics context. */
    public ArtAssets() { }

    /**
     * Infers documented horizontal strip layout without needing a graphics context.
     * @param path asset-relative PNG path
     * @param width positive image width
     * @param height positive image height
     * @return frame count; single images are never split by their aspect ratio alone
     * @throws IllegalArgumentException if dimensions or strip divisions are invalid
     * @throws NullPointerException if path is null
     */
    public static int frameCount(String path, int width, int height) {
        if (width <= 0 || height <= 0) { throw new IllegalArgumentException("Positive image dimensions required"); }
        var match = FRAMES.matcher(path);
        int count = match.find() ? Integer.parseInt(match.group(1))
                : path.startsWith("art/portal/") || path.startsWith("art/common_vfx/")
                || path.startsWith("art/skills/") ? Math.max(1, width / 256) : 1;
        if (count <= 0 || width % count != 0) { throw new IllegalArgumentException("Invalid strip: " + path); }
        return count;
    }

    /**
     * Resolves a horizontal frame, with a bounded presentation-only clock.
     * @param path internal asset path
     * @param seconds elapsed animation seconds
     * @param loop whether to repeat or hold the final frame
     * @return cached immutable frame region
     * @throws IllegalArgumentException if time is negative or non-finite
     */
    public TextureRegion frame(String path, double seconds, boolean loop) {
        if (!Double.isFinite(seconds) || seconds < 0) { throw new IllegalArgumentException("Invalid animation time"); }
        TextureRegion[] frames = strips.computeIfAbsent(path, key -> {
            Texture texture = new Texture(Gdx.files.internal(key));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.put(key, texture);
            int count = frameCount(key, texture.getWidth(), texture.getHeight());
            return TextureRegion.split(texture, texture.getWidth() / count, texture.getHeight())[0];
        });
        double index = Math.floor(seconds / FRAME_SECONDS);
        return frames[(int) (loop ? index % frames.length : Math.min(frames.length - 1, index))];
    }

    /**
     * Draws one frame inside visual-only bounds, optionally reflecting side-facing artwork.
     * @param batch active sprite batch
     * @param path internal PNG path
     * @param box visual rectangle, not collision data to mutate
     * @param seconds animation clock
     * @param loop repeat flag
     * @param flip horizontal reflection
     */
    public void draw(SpriteBatch batch, String path, CollisionBox box, double seconds, boolean loop, boolean flip) {
        TextureRegion frame = frame(path, seconds, loop);
        batch.draw(frame, (float) (flip ? box.right() : box.x()), (float) box.y(),
                (float) (flip ? -box.width() : box.width()), (float) box.height());
    }

    /** Releases every cached GPU texture once; safe to call again after the cache is empty. */
    @Override public void dispose() {
        textures.values().forEach(Texture::dispose);
        textures.clear();
        strips.clear();
    }
}
