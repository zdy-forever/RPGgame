package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import au.edu.anu.comp2120.world.Portal;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Composes pixel art with the retained procedural Hub and geometry-only world collision.
 * The historical class name is retained; animation does not control gameplay.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class PlaceholderRenderer implements Disposable {
    /** Read-only artwork presentation owned with this screen's GPU resources. */
    private final SceneArt art = new SceneArt();
    /** Radius of the single visible default-spawn marker in world units. */
    private static final float SPAWN_MARKER_RADIUS = 5;
    /** Number of segments in a temporary circular marker. */
    private static final int SPAWN_MARKER_SEGMENTS = 12;
    /** GPU resource used exclusively for simple geometry. */
    private final ShapeRenderer shapes;
    /** Fixed-size world view; window resizing adds letterboxing instead of changing its extent. */
    private final FitViewport viewport;
    /** LibGDX built-in debug font; requires no external art files. */
    private final BitmapFont debugFont;
    /** World-space batch used for player and NPC name tags. */
    private final SpriteBatch debugBatch;
    /** Width of the world-space name tag box in world units. */
    private static final float NAME_TAG_WIDTH = 140f;
    /** Gap between a subject's top edge and its name tag in world units. */
    private static final float NAME_OFFSET = 6f;
    /** Font scale for world-space name tags; small because it is measured in world units. */
    private static final float NAME_SCALE = 0.45f;
    /** Display name shown above the player. */
    private static final String PLAYER_NAME = "Adventurer";
    /** Half-size of each decorative fence post in world units. */
    private static final float FENCE_POST = 4f;
    /** Spacing between decorative fence posts in world units. */
    private static final float FENCE_SPACING = 44f;
    /** Inset of the decorative fence from the map border in world units. */
    private static final float FENCE_INSET = 14f;
    /** Y of the decorative main road in world units. */
    private static final float ROAD_Y = 180f;
    /** Height of the decorative main road in world units. */
    private static final float ROAD_HEIGHT = 34f;
    /** Decorative hut footprints as x, y, width, height in world units. */
    private static final float[][] HUTS = {
            {430f, 250f, 70f, 50f}, {530f, 250f, 70f, 50f}, {430f, 120f, 70f, 50f}
    };
    /** Decorative tree trunk base positions as x, y in world units. */
    private static final float[][] TREES = {
            {90f, 250f}, {120f, 95f}, {270f, 320f}, {390f, 95f},
            {640f, 300f}, {700f, 120f}, {830f, 250f}, {890f, 95f}
    };
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

        debugFont = new BitmapFont();
        debugFont.getRegion().getTexture().setFilter(
                com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        debugBatch = new SpriteBatch();
    }
    /**
     * Follows the player's centre with map clamping, then draws the art-backed scene.
     * Position is recalculated from current data every frame, including the first frame after travel.
     * Only the default spawn is marked; alternate test spawns remain usable but are not drawn.
     *
     * @param map        collision world to display
     * @param player     player to display
     * @param npcs       current-map NPCs to draw, or null when none are supplied
     * @param portals    immutable portal snapshots; only current-map entries are drawn
     * @param demo       existing current-scope gameplay observations
     * @param delta      presentation seconds, zero while paused
     * @param drops      current-map unclaimed material rectangles, independent of inventory icons
     * @param completed  defeated guardians whose stones are lit in Memorial Hall
     */
    public void render(GameMap map, Player player, List<NPC> npcs, List<Portal> portals,
                       CombatDemo demo, double delta, List<MaterialDrop> drops,
                       Set<BossChallenge> completed) {
        art.update(map, player, demo, delta);
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
        } else if (map.getType() == MapType.MEMORIAL) {
            shapes.setColor(0.08f, 0.07f, 0.12f, 1);
        } else {
            shapes.setColor(0.24f, 0.11f, 0.10f, 1);
        }
        draw(map.getBounds());
        drawDecorations(map);
        shapes.end();
        debugBatch.setProjectionMatrix(viewport.getCamera().combined);
        debugBatch.begin();
        art.background(debugBatch, map, demo);
        debugBatch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.55f, 0.58f, 0.65f, 1);
        for (CollisionBox obstacle : map.getObstacles()) {
            draw(obstacle);
        }
        shapes.end();
        debugBatch.begin();
        art.ground(debugBatch, map, demo);
        art.memorial(debugBatch, completed);
        art.actors(debugBatch, player, npcs, portals, demo, drops);
        debugBatch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.95f, 0.75f, 0.20f, 1);
        SpawnPoint spawn = map.getSpawnPoint(map.getDefaultSpawnId());
        shapes.circle((float) spawn.x(), (float) spawn.y(), SPAWN_MARKER_RADIUS, SPAWN_MARKER_SEGMENTS);
        shapes.end();
        drawNames(player, npcs);
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
     * Draws shared class/ability icons using the caller's screen-space batch and capped HUD policy.
     * @param batch active HUD batch
     * @param font existing HUD font
     * @param player current player
     * @param demo existing ability observations
     * @param hud size/visibility policy
     * @param width window width
     * @param height window height
     */
    public void drawSkillHud(SpriteBatch batch, BitmapFont font, Player player, CombatDemo demo, HudLayout hud, int width, int height) {
        art.hud(batch, font, player, demo.getAbilities(), hud, width, height);
    }

    /**
     * Latches a non-blocking HUD replay after automatic gameplay return, without retaining actors.
     * @param result settled immutable outcome
     * @param playerClass class at settlement
     */
    public void showOutcome(au.edu.anu.comp2120.BattleResult result, au.edu.anu.comp2120.player.PlayerClass playerClass) {
        art.showOutcome(result, playerClass);
    }
    /**
     * Draws static Hub scenery or a tiled Memorial Hall floor beneath all actors.
     * Decorations are geometry only and never affect movement, collision or combat.
     *
     * @param map current map; only peaceful maps receive procedural scenery
     */
    private void drawDecorations(GameMap map) {
        if (map.getType() == MapType.MEMORIAL) {
            CollisionBox bounds = map.getBounds();
            shapes.setColor(0.15f, 0.13f, 0.22f, 1);
            for (int x = 16; x < bounds.width() - 16; x += 32) {
                for (int y = 16; y < bounds.height() - 16; y += 32) {
                    shapes.rect(x, y, (float) Math.min(30, bounds.width() - 16 - x),
                            (float) Math.min(30, bounds.height() - 16 - y));
                }
            }
            return;
        }
        if (map.getType() != MapType.HUB) {
            return;
        }
        CollisionBox bounds = map.getBounds();
        drawRoad(bounds);
        drawFence(bounds);
        for (float[] hut : HUTS) {
            drawHut(hut[0], hut[1], hut[2], hut[3]);
        }
        for (float[] tree : TREES) {
            drawTree(tree[0], tree[1]);
        }
        shapes.setColor(0.13f, 0.42f, 0.68f, 1);
        draw(new CollisionBox(136f, 140f, 44f, 28f));
    }
    /**
     * Draws small name tags above the player and every listed NPC in world space.
     * Uses the camera projection so tags follow their subjects on screen.
     *
     * @param player player to label
     * @param npcs   NPCs to label; null or empty labels only the player
     */
    private void drawNames(Player player, List<NPC> npcs) {
        debugFont.getData().setScale(NAME_SCALE);
        debugBatch.setProjectionMatrix(viewport.getCamera().combined);
        debugBatch.begin();
        if (npcs != null) {
            for (NPC npc : npcs) {
                debugFont.setColor(1f, 0.90f, 0.40f, 1);
                drawNameTag(npc.getName(), npc.getX(),
                        npc.getY() + 54, npc.getWidth());
            }
        }
        debugFont.setColor(0.60f, 0.95f, 1f, 1);
        CollisionBox body = player.getBounds();
        drawNameTag(PLAYER_NAME, body.x(), body.y() + 54, body.width());
        debugBatch.end();
    }

    /**
     * Draws one centred name tag above a world rectangle.
     *
     * @param text  label to show
     * @param x     left edge of the subject in world units
     * @param top   top edge of the subject in world units
     * @param width subject width, used to centre the label
     */
    private void drawNameTag(String text, double x, double top, double width) {
        if (text == null || text.isEmpty()) {
            return;
        }
        debugFont.draw(debugBatch, text,
                (float) (x + width / 2 - NAME_TAG_WIDTH / 2),
                (float) top + NAME_OFFSET,
                NAME_TAG_WIDTH, Align.center, false);
    }
    /**
     * Draws the main road and the branch leading to the weapon merchant.
     * @param bounds map rectangle providing the road's horizontal extent
     */
    private void drawRoad(CollisionBox bounds) {
        shapes.setColor(0.22f, 0.26f, 0.30f, 1);
        draw(new CollisionBox(bounds.x(), ROAD_Y, bounds.width(), ROAD_HEIGHT));
        draw(new CollisionBox(302f, ROAD_Y, 26f, 80f));
    }

    /**
     * Draws a ring of fence posts just inside the map border.
     * @param bounds map rectangle to surround with decorative posts
     */
    private void drawFence(CollisionBox bounds) {
        float left = (float) bounds.x() + FENCE_INSET;
        float bottom = (float) bounds.y() + FENCE_INSET;
        float right = (float) (bounds.x() + bounds.width()) - FENCE_INSET;
        float top = (float) (bounds.y() + bounds.height()) - FENCE_INSET;
        shapes.setColor(0.36f, 0.28f, 0.20f, 1);
        for (float x = left; x <= right; x += FENCE_SPACING) {
            draw(new CollisionBox(x, bottom, FENCE_POST, FENCE_POST * 3));
            draw(new CollisionBox(x, top - FENCE_POST * 3, FENCE_POST, FENCE_POST * 3));
        }
        for (float y = bottom; y <= top; y += FENCE_SPACING) {
            draw(new CollisionBox(left, y, FENCE_POST, FENCE_POST * 3));
            draw(new CollisionBox(right - FENCE_POST, y, FENCE_POST, FENCE_POST * 3));
        }
    }

    /**
     * Draws one hut with a pointed roof and a lit window.
     * @param x left edge in world units
     * @param y bottom edge in world units
     * @param w body width in world units
     * @param h body height in world units, excluding the roof
     */
    private void drawHut(float x, float y, float w, float h) {
        shapes.setColor(0.30f, 0.26f, 0.22f, 1);
        draw(new CollisionBox(x, y, w, h));
        shapes.setColor(0.45f, 0.20f, 0.18f, 1);
        shapes.triangle(x - 6f, y + h, x + w + 6f, y + h, x + w / 2f, y + h + 22f);
        shapes.setColor(0.95f, 0.80f, 0.35f, 1);
        draw(new CollisionBox(x + w * 0.35f, y + h * 0.25f, w * 0.3f, h * 0.3f));
    }

    /**
     * Draws one tree with a trunk and a canopy block.
     * @param x left edge in world units
     * @param y trunk base in world units
     */
    private void drawTree(float x, float y) {
        shapes.setColor(0.32f, 0.22f, 0.14f, 1);
        draw(new CollisionBox(x + 4f, y, 6f, 18f));
        shapes.setColor(0.16f, 0.42f, 0.22f, 1);
        draw(new CollisionBox(x, y + 18f, 14f, 12f));
        shapes.setColor(0.20f, 0.50f, 0.26f, 1);
        draw(new CollisionBox(x + 2f, y + 26f, 10f, 8f));
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
        art.dispose();
        shapes.dispose();
        debugFont.dispose();
        debugBatch.dispose();
    }

}
