package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapType;
import java.util.List;
import java.util.Objects;

/**
 * Small graphics-free adapter for testing combat against a stationary dummy in battle maps.
 * Owns temporary placement/reset policy, not hit detection or damage rules. Screen input supplies
 * booleans; rendering reads geometry. No enemy AI or final player skill is implemented here.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class CombatDemo {
    /** Existing map/player coordinator; all transitions remain its responsibility. */
    private final GameSession session;
    /** Reused arena pipeline, cleared on every successful map transition. */
    private final CombatSystem combat = new CombatSystem();
    /** Same attack tuning for both directions of the debug demonstration. */
    private final AttackSpec strike = new AttackSpec("debug-strike", GameConfig.COMBAT_DAMAGE,
            GameConfig.COMBAT_ACTIVE_SECONDS, GameConfig.COMBAT_COOLDOWN_SECONDS);
    /** Player adapter in the current scope, following the existing player's movement. */
    private Combatant player;
    /** Temporary dummy entity, or null in Hub/when no valid nearby placement exists. */
    private Enemy enemy;
    /** Dummy's independent hurtbox adapter, or null when no dummy is present. */
    private Combatant dummy;
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
     * Refreshes hurtboxes, handles explicit debug requests, and delegates combat simulation.
     * Map changes discard that frame's combat requests and old attacks; reset also consumes requests.
     * Travel does not heal the player; reset explicitly does. A new map receives a fresh dummy.
     * @param delta finite non-negative simulation seconds
     * @param playerAttack whether the player requested one strike
     * @param enemyAttack whether a debug command requested one dummy strike
     * @param reset whether to clear attacks/cooldowns and restore both actors' HP
     * @throws IllegalArgumentException if delta is invalid, before any state change
     */
    public void update(double delta, boolean playerAttack, boolean enemyAttack, boolean reset) {
        if (!Double.isFinite(delta) || delta < 0) {
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
            if (dummy != null) {
                enemy.getHealth().restoreFull();
                combat.register(dummy);
            }
            return;
        }
        if (playerAttack) {
            combat.tryAttack(player, strike, strikeBox(player.getHurtbox()));
        }
        if (enemyAttack && dummy != null) {
            combat.tryAttack(dummy, strike, strikeBox(dummy.getHurtbox()));
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
     * Formats HP feedback for the native window title, requiring no font asset.
     * @return player/dummy HP and temporary key hints
     */
    public String getStatus() {
        return "HP " + session.getPlayer().getHealth().getCurrentHp()
                + " | Dummy " + (enemy == null ? "none (battle maps only)" : enemy.getHealth().getCurrentHp())
                + " | J attack | H dummy attack | R reset HP";
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
        enemy = null;
        dummy = null;
        if (session.getCurrentMap().getType() == MapType.BATTLE) {
            CollisionBox placement = findDummyPlacement();
            if (placement != null) {
                enemy = new Enemy();
                dummy = new Combatant(enemy, Faction.ENEMY, placement);
                combat.register(dummy);
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
     * Tries four nearby positions using current map collision data, without an enemy spawn manager.
     * @return first walkable non-overlapping rectangle, or null when none fits
     */
    private CollisionBox findDummyPlacement() {
        CollisionBox body = session.getPlayer().getBounds();
        double offset = GameConfig.COMBAT_DUMMY_OFFSET;
        double[][] offsets = {{offset, 0}, {-offset, 0}, {0, offset}, {0, -offset}};
        for (double[] shift : offsets) {
            CollisionBox candidate = new CollisionBox(body.x() + shift[0], body.y() + shift[1],
                    GameConfig.COMBAT_DUMMY_SIZE, GameConfig.COMBAT_DUMMY_SIZE);
            if (session.getCurrentMap().isWalkable(candidate) && !candidate.overlaps(body)) {
                return candidate;
            }
        }
        return null;
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
