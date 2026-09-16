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
import com.badlogic.gdx.utils.Align;
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
    /** Reference window height used to auto-scale the debug HUD text. */
    private static final float HUD_BASE_HEIGHT = 720f;
    /** Smallest allowed HUD scale, so text stays readable on tiny windows. */
    private static final float HUD_MIN_SCALE = 1f;
    /** Extra size multiplier on top of the auto scale; raise this for bigger text. */
    private static final float HUD_FONT_SCALE = 1.4f;
    /** Speaker name shown in the dialogue box. */
    private String dialogueSpeaker = "";
    /** Current dialogue line shown in the box. */
    private String dialogueText = "";
    /** Prompt text shown at the bottom-left of the box. */
    private String dialogueHint = "";
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
     * Shows an in-screen dialogue box at the bottom.
     *
     * @param speaker NPC name drawn in the heading colour
     * @param text    current dialogue line
     * @param hint    prompt such as "[E] Next"
     */
    public void setDialogue(String speaker, String text, String hint) {
        this.dialogueSpeaker = speaker == null ? "" : speaker;
        this.dialogueText = text == null ? "" : text;
        this.dialogueHint = hint == null ? "" : hint;
    }

    /** Hides the dialogue box. */
    public void clearDialogue() {
        dialogueText = "";
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
    public void render(GameMap map, Player player, List<NPC> npcs, List<Portal> portals,
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
        if (npcs != null) {
            for (NPC npc : npcs) {
                boolean grantsReward = npc.getRewardItemId() != null;
                shapes.setColor(grantsReward ? 1f : 0.30f,
                        grantsReward ? 1f : 0.90f,
                        grantsReward ? 0f : 0.95f, 1);
                draw(new CollisionBox(npc.getX(), npc.getY(), npc.getWidth(), npc.getHeight()));
            }
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
            float scale = Math.max(HUD_MIN_SCALE, screenH / HUD_BASE_HEIGHT) * HUD_FONT_SCALE;
            debugFont.getData().setScale(scale);
            debugBatch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
            debugBatch.begin();
            debugFont.setColor(0.20f, 0.95f, 0.55f, 1);
            debugFont.draw(debugBatch,
                    "Player x=" + (int) player.getX() + "  y=" + (int) player.getY(),
                    12f * scale, screenH - 12f * scale);
            debugBatch.end();
            drawDialogue();
        }
    }
    /** Draws the dialogue box in screen space above the world. */
    private void drawDialogue() {
        if (dialogueText.isEmpty()) {
            return;
        }
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float boxW = w * 0.7f;
        float boxH = h * 0.22f;
        float x = (w - boxW) / 2f;
        float y = 18f;
        shapes.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.05f, 0.06f, 0.09f, 0.92f);
        shapes.rect(x, y, boxW, boxH);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.95f, 0.85f, 0.35f, 1);
        shapes.rect(x, y, boxW, boxH);
        shapes.end();
        float scale = Math.max(HUD_MIN_SCALE, h / HUD_BASE_HEIGHT) * HUD_FONT_SCALE;
        debugFont.getData().setScale(scale);
        debugBatch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
        debugBatch.begin();
        float pad = 14f * scale;
        debugFont.setColor(1f, 0.90f, 0.40f, 1);
        debugFont.draw(debugBatch, dialogueSpeaker, x + pad, y + boxH - pad);
        debugFont.setColor(1f, 1f, 1f, 1);
        debugFont.draw(debugBatch, dialogueText, x + pad, y + boxH - pad - 26f * scale,
                boxW - 2 * pad, Align.left, true);
        debugFont.setColor(0.60f, 0.90f, 1f, 1);
        debugFont.draw(debugBatch, dialogueHint, x + pad, y + pad + 4f);
        debugBatch.end();
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
