package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapType;
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
 * Spawn points are invisible gameplay anchors and receive no debug marker.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class PlaceholderRenderer implements Disposable {
    /** Read-only artwork presentation owned with this screen's GPU resources. */
    private final SceneArt art = new SceneArt();
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
    /** Size of the NPC interaction marker (exclamation) in world units. */
    private static final float HEAD_MARKER_SIZE = 24f;
    /** Gap above the visual body top before the interaction marker appears. */
    private static final float HEAD_MARKER_OFFSET = 12f;
    /** Font scale for negative-status labels; larger than a name tag so it reads at a glance. */
    private static final float STATUS_SCALE = 0.62f;
    /** Vertical spacing between stacked status labels in world units. */
    private static final float STATUS_LINE = 11f;
    /** Height of the player's artwork, matching the scene's own visual body. */
    private static final float PLAYER_ART_HEIGHT = 54f;
    /** Height of a guardian's artwork, matching the scene's own visual body. */
    private static final float ENEMY_ART_HEIGHT = 92f;
    /** Gap above the player's artwork that clears its existing name tag. */
    private static final float PLAYER_STATUS_GAP = 18f;
    /** Gap above a guardian's artwork, which carries no name tag. */
    private static final float ENEMY_STATUS_GAP = 6f;
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
     * Spawn anchors remain invisible; portals, actors and objective markers supply visual guidance.
     *
     * @param map        collision world to display
     * @param player     player to display
     * @param npcs       current-map NPCs to draw, or null when none are supplied
     * @param portals    immutable portal snapshots; only current-map entries are drawn
     * @param demo       existing current-scope gameplay observations
     * @param delta      presentation seconds, zero while paused
     * @param drops      current-map unclaimed material rectangles, independent of inventory icons
     * @param completed      defeated guardians whose stones are lit in Memorial Hall
     * @param objectiveNpcId NPC that currently drives the quest progression; highlighted with a '!' marker
     */
    public void render(GameMap map, Player player, List<NPC> npcs, List<Portal> portals,
                       CombatDemo demo, double delta, List<MaterialDrop> drops,
                       Set<BossChallenge> completed, String objectiveNpcId) {
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
        drawBossWarnings(demo);
        shapes.end();
        drawNames(player, npcs, objectiveNpcId);
        drawStatusTags(player, demo);
    }

    /**
     * Stacks prominent negative-status labels above every actor in the current arena.
     * Uses the same world-space batch and camera projection as the name tags, so labels follow
     * their subject without projecting coordinates by hand. Labels are read from shared combat
     * state and from the guardian's own affliction, and require no new artwork.
     * Anchors match the scene's visual bodies, so text clears the artwork rather than the hitbox.
     *
     * @param player player whose collision body anchors its own labels
     * @param demo read-only current-scope combat observations
     */
    private void drawStatusTags(Player player, CombatDemo demo) {
        String affliction = demo.getBoss().map(Boss::getAfflictionLabel).orElse("");
        debugFont.getData().setScale(STATUS_SCALE);
        debugBatch.setProjectionMatrix(viewport.getCamera().combined);
        debugBatch.begin();
        for (Combatant actor : demo.getCombatants()) {
            boolean isPlayer = actor.getFaction() == Faction.PLAYER;
            List<StatusLabels.Tag> tags = StatusLabels.forActor(actor.getStatus(),
                    isPlayer ? affliction : "");
            if (tags.isEmpty()) {
                continue;
            }
            CollisionBox anchor = isPlayer ? player.getBounds() : actor.getHurtbox();
            double baseline = anchor.y() + (isPlayer ? PLAYER_ART_HEIGHT + PLAYER_STATUS_GAP
                    : ENEMY_ART_HEIGHT + ENEMY_STATUS_GAP);
            for (int line = 0; line < tags.size(); line++) {
                StatusLabels.Tag tag = tags.get(line);
                debugFont.setColor(tag.red(), tag.green(), tag.blue(), 1);
                drawNameTag(tag.text(), anchor.x(), baseline + line * STATUS_LINE, anchor.width());
            }
        }
        debugBatch.end();
        debugFont.getData().setScale(NAME_SCALE);
    }

    /**
     * Outlines the exact locked warning and active melee hitboxes, never applying damage.
     * @param demo read-only current encounter observations
     */
    private void drawBossWarnings(CombatDemo demo) {
        demo.getBoss().ifPresent(boss -> {
            shapes.setColor(1, 0.08f, 0.08f, 1);
            boss.getMeleeWarning().ifPresent(this::drawAttackBorder);
            for (var attack : demo.getActiveAttacks()) {
                if (attack.isActive() && attack.getSource() == boss.getCombatant()
                        && attack.getProjectile() == null && !attack.isDerived()) {
                    drawAttackBorder(attack.getHitbox());
                }
            }
        });
    }

    /**
     * Draws an inward two-world-unit red outline independent of platform line-width support.
     * @param box actual gameplay hitbox
     */
    private void drawAttackBorder(CollisionBox box) {
        float x = (float) box.x(), y = (float) box.y();
        float w = (float) box.width(), h = (float) box.height();
        float edge = Math.min(2, Math.min(w, h));
        shapes.rect(x, y, w, edge);
        shapes.rect(x, y + h - edge, w, edge);
        shapes.rect(x, y, edge, h);
        shapes.rect(x + w - edge, y, edge, h);
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
     * Draws shared class/ability icons using the caller's screen-space batch and quickbar scale.
     * @param batch active HUD batch
     * @param font existing HUD font
     * @param player current player
     * @param demo existing ability observations
     * @param hud size/visibility policy
     * @param width window width
     */
    public void drawSkillHud(SpriteBatch batch, BitmapFont font, Player player, CombatDemo demo, HudLayout hud, int width) {
        art.hud(batch, font, shapes, player, demo.getAbilities(), hud, width);
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
     * Advances and draws return feedback separately from the persistent, hideable skill HUD.
     * @param batch idle screen-space UI batch
     * @param font shared UI font
     * @param uiShapes idle screen-space geometry renderer
     * @param width logical window width
     * @param height logical window height
     * @param delta finite non-negative UI elapsed seconds, including time in menus
     * @throws IllegalArgumentException if elapsed time is invalid
     */
    public void drawOutcome(SpriteBatch batch, BitmapFont font, ShapeRenderer uiShapes,
                            int width, int height, double delta) {
        art.advanceOutcome(delta);
        art.drawOutcome(batch, font, uiShapes, width, height);
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
     * @param player         player to label
     * @param npcs           NPCs to label; null or empty labels only the player
     * @param objectiveNpcId NPC that currently drives the quest progression; highlighted with a '!' marker
     */
    private void drawNames(Player player, List<NPC> npcs, String objectiveNpcId) {
        debugFont.getData().setScale(NAME_SCALE);
        debugBatch.setProjectionMatrix(viewport.getCamera().combined);
        debugBatch.begin();
        if (npcs != null) {
            for (NPC npc : npcs) {
                debugFont.setColor(1f, 0.90f, 0.40f, 1);
                drawNameTag(npc.getName(), npc.getX(),
                        npc.getY() + 54, npc.getWidth());
                if (objectiveNpcId != null && objectiveNpcId.equals(npc.getId())) {
                    drawHeadMark(npc.getX(), npc.getY() + 54, npc.getWidth());
                }
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
     * Draws the exclamation interaction marker centred above an NPC's head.
     *
     * @param x     left edge of the subject in world units
     * @param top   top edge of the subject in world units
     * @param width subject width, used to centre the marker
     */
    private void drawHeadMark(double x, double top, double width) {
        float size = HEAD_MARKER_SIZE;
        CollisionBox marker = new CollisionBox(
                x + width / 2 - size / 2,
                top + HEAD_MARKER_OFFSET,
                size, size);
        art.draw(debugBatch, SceneArt.BOSS_WARNING_IMAGE, marker);
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
