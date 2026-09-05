package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
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
    /** GPU resource used exclusively for simple geometry. */
    private final ShapeRenderer shapes = new ShapeRenderer();
    /** Fixed-size world view; resizing adds letterboxing instead of changing map geometry. */
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
     * Draws the floor, obstacles, and player using only immutable geometry.
     *
     * @param map collision world to display
     * @param player player to display
     */
    public void render(GameMap map, Player player) {
        ScreenUtils.clear(0.02f, 0.025f, 0.035f, 1);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.10f, 0.16f, 1);
        draw(map.getBounds());
        shapes.setColor(0.55f, 0.58f, 0.65f, 1);
        for (CollisionBox obstacle : map.getObstacles()) {
            draw(obstacle);
        }
        shapes.setColor(0.20f, 0.85f, 0.60f, 1);
        draw(player.getBounds());
        shapes.end();
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
