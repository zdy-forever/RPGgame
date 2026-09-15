package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import au.edu.anu.comp2120.world.Portal;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Draws collision rectangles on a solid floor without textures, fonts, or other assets.
 * Replacing this renderer does not change movement, collision, or combat rules.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class PlaceholderRenderer implements Disposable {
    /** Radius of temporary spawn markers in world units. */
    private static final float SPAWN_MARKER_RADIUS = 5;
    /** Number of segments in a temporary circular marker. */
    private static final int SPAWN_MARKER_SEGMENTS = 12;
    /** GPU resource used exclusively for simple geometry. */
    private final ShapeRenderer shapes;
    /** Fixed-size world view; window resizing adds letterboxing instead of changing its extent. */
    private final FitViewport viewport;
    /** LibGDX built-in debug font; requires no external art files. */
    private final BitmapFont debugFont;
    /** Screen-space batch used only for the optional debug HUD text. */
    private final SpriteBatch debugBatch;
    /** Whether the live coordinate overlay is drawn. */
    private boolean debugCoords;
    /**
     * Creates drawing resources after LibGDX has initialized its graphics context.
     *
     * @param worldWidth positive finite visible world width, independent of map dimensions
     * @param worldHeight positive finite visible world height, independent of map dimensions
     * @throws IllegalArgumentException if view dimensions are not positive and finite
     */
    public PlaceholderRenderer(float worldWidth, float worldHeight) {
        if (!Float.isFinite(worldWidth) || !Float.isFinite(worldHeight)
                || worldWidth <= 0 || worldHeight <= 0) {
            throw new IllegalArgumentException("Camera view dimensions must be positive and finite");
        }
        viewport = new FitViewport(worldWidth, worldHeight);
        shapes = new ShapeRenderer();

        // ↓↓↓ 加这三行 ↓↓↓
        debugFont = new BitmapFont();
        debugBatch = new SpriteBatch();
        debugCoords = false;
    }
    /**
     * Enables or disables the live player-coordinate overlay in the top-left corner.
     *
     * @param enabled true to draw the debug HUD
     */
    public void setDebugCoords(boolean enabled) {
        this.debugCoords = enabled;
    }
    /**
     * Follows the player's centre with map clamping, then draws the placeholder scene.
     * Position is recalculated from current data every frame, including the first frame after travel.
     *
     * @param map        collision world to display
     * @param player     player to display
     * @param portals    immutable portal snapshots; only current-map entries are drawn
     * @param combatants current-scope actors for enemy rectangles and hurtbox outlines
     * @param attacks    active current-scope invocations for temporary hitbox outlines
     * @param drops      current-map unclaimed material rectangles, independent of inventory icons
     */
    public void render(GameMap map, Player player, NPC npc, List<Portal> portals,
                       List<Combatant> combatants, List<Attack> attacks, List<MaterialDrop> drops) {
        ScreenUtils.clear(0.02f, 0.025f, 0.035f, 1);
        CollisionBox view = CameraFollow.viewFor(player.getBounds(), map.getBounds(),
                viewport.getWorldWidth(), viewport.getWorldHeight());
        viewport.getCamera().position.set((float) (view.x() + view.width() / 2),
                (float) (view.y() + view.height() / 2), 0);
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
        for (Portal portal : portals) {
            if (portal.sourceMapId().equals(map.getId())) {
                if (portal.enabled()) {
                    shapes.setColor(0.65f, 0.25f, 0.85f, 1);
                } else {
                    shapes.setColor(0.28f, 0.22f, 0.32f, 1);
                }
                draw(portal.trigger());
            }
        }
        shapes.setColor(0.95f, 0.75f, 0.20f, 1);
        for (SpawnPoint spawn : map.getSpawnPoints().values()) {
            shapes.circle((float) spawn.x(), (float) spawn.y(),
                    SPAWN_MARKER_RADIUS, SPAWN_MARKER_SEGMENTS);
        }
        shapes.setColor(0.20f, 0.85f, 0.60f, 1);
        draw(player.getBounds());
        // Draw NPC (yellow square)
        if (npc != null) {
            shapes.setColor(1f, 1f, 0f, 1);
            draw(new CollisionBox(npc.getX(), npc.getY(), npc.getWidth(), npc.getHeight()));
        }
        for (Combatant actor : combatants) {
            if (actor.getFaction() == Faction.ENEMY) {
                if (actor.getEntity().isAlive()) {
                    shapes.setColor(0.85f, 0.25f, 0.20f, 1);
                } else {
                    shapes.setColor(0.30f, 0.30f, 0.30f, 1);
                }
                draw(actor.getHurtbox());
            }
        }
        shapes.setColor(1.0f, 0.7f, 0.1f, 1);
        for (MaterialDrop drop : drops) {
            if (drop.mapId().equals(map.getId())) {
                draw(drop.bounds());
            }
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.20f, 0.90f, 1.0f, 1);
        for (Combatant actor : combatants) {
            draw(actor.getHurtbox());
        }
        shapes.setColor(1.0f, 0.85f, 0.10f, 1);
        for (Attack attack : attacks) {
            if (attack.isActive()) {
                draw(attack.getHitbox());
            }
        }
        shapes.end();
        if (debugCoords) {
            int screenW = Gdx.graphics.getWidth();
            int screenH = Gdx.graphics.getHeight();
            debugBatch.setProjectionMatrix(
                    new Matrix4().setToOrtho2D(0, 0, screenW, screenH));
            debugBatch.begin();
            debugFont.setColor(0.20f, 0.95f, 0.55f, 1);
            debugFont.draw(debugBatch,
                    "Player x=" + (int) player.getX() + "  y=" + (int) player.getY(),
                    10, screenH - 10);
            debugBatch.end();
        }
    }

    /**
     * Resizes the screen viewport without recentering the camera or changing its world extent.
     * Zero/negative window dimensions (for example during minimization) preserve the last view.
     *
     * @param width window width in screen units
     * @param height window height in screen units
     */
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            viewport.update(width, height, false);
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
        debugFont.dispose();
        debugBatch.dispose();
    }

}
