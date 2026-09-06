package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Draws collision rectangles on a solid floor without textures, fonts, or other assets.
 * Replacing this renderer does not change movement or collision rules.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class PlaceholderRenderer implements Disposable {
    /** Radius of temporary spawn markers in world units. */
    private static final float SPAWN_MARKER_RADIUS = 5;
    /** Number of segments in a temporary circular marker. */
    private static final int SPAWN_MARKER_SEGMENTS = 12;
    /** GPU resource used exclusively for simple geometry. */
    private final ShapeRenderer shapes = new ShapeRenderer();
    /** View fitted to the current map; window resizing never changes collision geometry. */
    private final FitViewport viewport;

    /**
     * Creates drawing resources after LibGDX has initialized its graphics context.
     *
     * @param worldWidth visible world width
     * @param worldHeight visible world height
     */
    public PlaceholderRenderer(float worldWidth, float worldHeight) {
        viewport = new FitViewport(worldWidth, worldHeight);
    }

    /**
     * Draws map-role colours, obstacles, debug spawn markers, and the player using geometry.
     *
     * @param map collision world to display
     * @param player player to display
     */
    public void render(GameMap map, Player player) {
        ScreenUtils.clear(0.02f, 0.025f, 0.035f, 1);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (map.getType() == MapType.HUB) {
            shapes.setColor(0.08f, 0.18f, 0.22f, 1);
        } else {
            shapes.setColor(0.24f, 0.11f, 0.10f, 1);
        }
        draw(map.getBounds());
        shapes.setColor(0.55f, 0.58f, 0.65f, 1);
        for (CollisionBox obstacle : map.getObstacles()) {
            draw(obstacle);
        }
        shapes.setColor(0.95f, 0.75f, 0.20f, 1);
        for (SpawnPoint spawn : map.getSpawnPoints().values()) {
            shapes.circle((float) spawn.x(), (float) spawn.y(),
                    SPAWN_MARKER_RADIUS, SPAWN_MARKER_SEGMENTS);
        }
        shapes.setColor(0.20f, 0.85f, 0.60f, 1);
        draw(player.getBounds());
        shapes.end();
    }

    /**
     * Fits the view to a newly selected map without creating new GPU resources.
     *
     * @param map map whose dimensions define the visible world
     * @param width current window width
     * @param height current window height
     */
    public void showMap(GameMap map, int width, int height) {
        viewport.setWorldSize((float) map.getBounds().width(), (float) map.getBounds().height());
        resize(width, height);
    }

    /**
     * Preserves the world aspect ratio and centres the camera on resize.
     *
     * @param width window width in screen units
     * @param height window height in screen units
     */
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            viewport.update(width, height, true);
        }
    }

    /**
     * Converts model coordinates only at the rendering boundary.
     *
     * @param box rectangle to draw
     */
    private void draw(CollisionBox box) {
        shapes.rect((float) box.x(), (float) box.y(), (float) box.width(), (float) box.height());
    }

    /** Releases the shape renderer's GPU resources. */
    @Override
    public void dispose() {
        shapes.dispose();
    }
}
