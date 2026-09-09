package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.enemy.EnemyAI;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapType;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Graphics-free arena adapter coordinating player requests and multiple basic enemy controllers.
 * Owns temporary placement/reset policy, not AI decisions, hit detection, or damage rules.
 * Screen input supplies booleans; rendering reads geometry. No final player skill is implemented here.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class CombatDemo {
    /** Existing map/player coordinator; all transitions remain its responsibility. */
    private final GameSession session;
    /** Reused arena pipeline, cleared on every successful map transition. */
    private final CombatSystem combat = new CombatSystem();
    /** Temporary player attack tuning; enemies supply their own shared AttackSpec through AI. */
    private final AttackSpec strike = new AttackSpec("debug-strike", GameConfig.COMBAT_DAMAGE,
            GameConfig.COMBAT_ACTIVE_SECONDS, GameConfig.COMBAT_COOLDOWN_SECONDS);
    /** Player adapter in the current scope, following the existing player's movement. */
    private Combatant player;
    /** Independent enemy controllers in this arena; Hub contains none. */
    private final List<EnemyAI> enemies = new ArrayList<>();
    /** Last synchronized transition stamp, including re-entry to the same map. */
    private long transitionVersion = Long.MIN_VALUE;

    /**
     * Creates and synchronizes the debug arena without loading assets or changing player health.
     * @param session existing game session
     * @throws NullPointerException if session is null
     */
    public CombatDemo(GameSession session) {
        this.session = Objects.requireNonNull(session, "session");
        syncMap();
    }

    /**
     * Refreshes hurtboxes, handles player requests, updates every AI, then advances combat once.
     * Map changes discard that frame's combat requests and old attacks; reset also consumes requests.
     * Travel does not heal the player; reset explicitly does. A new map receives fresh enemies.
     * @param delta finite non-negative simulation seconds with representable enemy movement
     * @param playerAttack whether the player requested one strike
     * @param reset whether to clear attacks/cooldowns, reset awareness, and restore all actors' HP
     * @throws IllegalArgumentException if delta is invalid, before any state change
     */
    public void update(double delta, boolean playerAttack, boolean reset) {
        if (!Double.isFinite(delta) || delta < 0 || !Double.isFinite(GameConfig.BASIC_ENEMY.speed() * delta)) {
            throw new IllegalArgumentException("Demo delta must be finite and non-negative");
        }
        if (syncMap()) {
            return;
        }
        player.setHurtbox(playerHurtbox());
        if (reset) {
            combat.clear();
            session.getPlayer().getHealth().restoreFull();
            combat.register(player);
            for (EnemyAI ai : enemies) {
                ai.getEnemy().getHealth().restoreFull();
                ai.resetAwareness();
                combat.register(ai.getCombatant());
            }
            return;
        }
        if (playerAttack) {
            combat.tryAttack(player, strike, strikeBox(player.getHurtbox()));
        }
        for (EnemyAI ai : enemies) {
            ai.update(player, delta);
        }
        combat.update(delta);
    }

    /**
     * Provides hurtboxes and receiver status to the placeholder renderer.
     * @return immutable membership snapshot of the current arena
     */
    public List<Combatant> getCombatants() {
        return combat.getCombatants();
    }

    /**
     * Provides currently active hitboxes for short-lived debug outlines.
     * @return immutable list of observational attack references
     */
    public List<Attack> getActiveAttacks() {
        return combat.getActiveAttacks();
    }

    /**
     * Returns current enemy controllers for state/geometry diagnostics.
     * @return immutable membership snapshot; individual controllers retain live state
     */
    public List<EnemyAI> getEnemies() {
        return List.copyOf(enemies);
    }

    /**
     * Formats HP feedback for the native window title, requiring no font asset.
     * @return player HP, per-enemy HP/state, and temporary key hints
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder("HP ")
                .append(session.getPlayer().getHealth().getCurrentHp());
        for (int i = 0; i < enemies.size(); i++) {
            EnemyAI ai = enemies.get(i);
            status.append(" | E").append(i + 1).append(' ')
                    .append(ai.getEnemy().getHealth().getCurrentHp()).append(' ').append(ai.getState());
        }
        return status.append(" | J attack | R reset HP").toString();
    }

    /**
     * Rebuilds only debug registrations after a map transition and cancels every old invocation.
     * @return true if a new scope was created
     */
    private boolean syncMap() {
        if (transitionVersion == session.getTransitionVersion()) {
            return false;
        }
        combat.clear();
        player = new Combatant(session.getPlayer(), Faction.PLAYER, playerHurtbox());
        combat.register(player);
        enemies.clear();
        if (session.getCurrentMap().getType() == MapType.BATTLE) {
            for (CollisionBox placement : findEnemyPlacements()) {
                enemies.add(new EnemyAI(new Enemy(), placement, session.getCurrentMap(),
                        GameConfig.BASIC_ENEMY, combat));
            }
        }
        transitionVersion = session.getTransitionVersion();
        return true;
    }

    /**
     * Insets the player's collision rectangle for a visibly separate vulnerable region.
     * @return positive hurtbox independent of visual dimensions
     */
    private CollisionBox playerHurtbox() {
        CollisionBox body = session.getPlayer().getBounds();
        double inset = Math.min(GameConfig.COMBAT_HURTBOX_INSET, Math.min(body.width(), body.height()) / 4);
        return new CollisionBox(body.x() + inset, body.y() + inset,
                body.width() - 2 * inset, body.height() - 2 * inset);
    }

    /**
     * Tries four nearby candidates using current map collision data and limits their count.
     * Placement avoids the player and earlier enemies; later movement has no entity-body collision.
     * @return walkable non-overlapping rectangles, possibly fewer than the configured count
     */
    private List<CollisionBox> findEnemyPlacements() {
        CollisionBox body = session.getPlayer().getBounds();
        List<CollisionBox> placements = new ArrayList<>();
        double offset = GameConfig.ENEMY_SPAWN_OFFSET;
        double[][] offsets = {{offset, 0}, {0, offset}, {-offset, 0}, {0, -offset}};
        for (double[] shift : offsets) {
            CollisionBox candidate = new CollisionBox(body.x() + shift[0], body.y() + shift[1],
                    GameConfig.ENEMY_SIZE, GameConfig.ENEMY_SIZE);
            if (session.getCurrentMap().isWalkable(candidate) && !candidate.overlaps(body)
                    && placements.stream().noneMatch(candidate::overlaps)) {
                placements.add(candidate);
                if (placements.size() == GameConfig.ENEMY_COUNT) {
                    break;
                }
            }
        }
        return placements;
    }

    /**
     * Builds an intentionally simple all-direction debug strike, not a final class skill.
     * @param hurtbox source world-space rectangle
     * @return fixed rectangle extending the configured range on every side
     */
    private CollisionBox strikeBox(CollisionBox hurtbox) {
        double range = GameConfig.COMBAT_RANGE;
        return new CollisionBox(hurtbox.x() - range, hurtbox.y() - range,
                hurtbox.width() + 2 * range, hurtbox.height() + 2 * range);
    }
}
