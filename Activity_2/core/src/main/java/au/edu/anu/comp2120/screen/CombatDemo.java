package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.enemy.Enemy;
import au.edu.anu.comp2120.enemy.EnemyAI;
import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.player.PlayerAbilities;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.player.AbilitySlot;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerClasses;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.MapCatalog;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Graphics-free arena adapter coordinating player requests, basic enemies and an optional Boss encounter.
 * Owns temporary placement/reset policy, not AI decisions, hit detection, or damage rules.
 * Screen input supplies commands; common abilities own execution while rendering reads geometry.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class CombatDemo {
    /** Existing map/player coordinator; all transitions remain its responsibility. */
    private final GameSession session;
    /** Reused arena pipeline, cleared on every successful map transition. */
    private final CombatSystem combat = new CombatSystem();
    /** Shared player ability controller, retaining slot cooldowns across map transitions. */
    private PlayerAbilities abilities;
    /** Player adapter in the current scope, following the existing player's movement. */
    private Combatant player;
    /** Independent enemy controllers in this arena; Hub contains none. */
    private final List<EnemyAI> enemies = new ArrayList<>();
    /** Optional current-scope encounter; only Crossroads receives the placeholder sentinel. */
    private Boss boss;
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
        update(delta, new PlayerCommand(0, 0, playerAttack, false, false, false), reset);
    }

    /**
     * Coordinates common player movement/abilities, enemy decisions and one shared combat step.
     * Transitions discard commands and cancel active abilities, preserving slot cooldowns.
     * Reset consumes commands, restores health/cooldowns and recreates the Boss encounter without old loot.
     * @param delta finite non-negative seconds with representable player/enemy movement
     * @param command movement and ability requests sampled once by the input adapter
     * @param reset whether the explicit debug reset was requested
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delta is invalid, before state changes
     */
    public void update(double delta, PlayerCommand command, boolean reset) {
        update(delta, command, reset, null);
    }

    /**
     * Adds debug class selection to the normal coordinated simulation step.
     * Transition/reset take priority; otherwise a selection attempt consumes new movement/ability
     * input, even if unsafe to switch. Existing dash/attacks and simulation time still advance.
     * @param delta finite non-negative seconds with representable player/enemy movement
     * @param command shared movement/ability intent
     * @param reset whether debug restoration was requested
     * @param classSelection newly requested class, or null when no selection was requested
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delta is invalid, before state changes
     */
    public void update(double delta, PlayerCommand command, boolean reset, PlayerClass classSelection) {
        Objects.requireNonNull(command, "command");
        if (!Double.isFinite(delta) || delta < 0 || !Double.isFinite(GameConfig.BASIC_ENEMY.speed() * delta)
                || !Double.isFinite(GameConfig.SENTINEL.navigation().speed() * delta)
                || !Double.isFinite(session.getPlayer().getSpeed() * delta)) {
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
            abilities.reset();
            for (EnemyAI ai : enemies) {
                ai.getEnemy().getHealth().restoreFull();
                ai.resetAwareness();
                combat.register(ai.getCombatant());
            }
            createBoss();
            return;
        }
        if (classSelection != null) {
            abilities.selectClass(PlayerClasses.get(classSelection));
        }
        abilities.update(delta, classSelection != null ? PlayerCommand.NONE : command);
        for (EnemyAI ai : enemies) {
            ai.update(player, delta);
        }
        if (boss != null) {
            boss.update(player, delta);
        }
        combat.update(delta);
        if (boss != null) {
            boss.synchronizeOutcome();
        }
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
     * Exposes the optional encounter for diagnostics and future pickup integration.
     * @return current Boss, empty in maps without a valid encounter placement
     */
    public Optional<Boss> getBoss() {
        return Optional.ofNullable(boss);
    }

    /**
     * Returns unclaimed current-map reward data for the geometry-only view.
     * @return immutable list containing zero or one Boss material drop
     */
    public List<MaterialDrop> getDrops() {
        return boss == null ? List.of() : boss.getDrop().stream().toList();
    }

    /**
     * Exposes current common ability lifecycle for diagnostics, independent of rendering.
     * @return persistent player ability controller
     */
    public PlayerAbilities getAbilities() {
        return abilities;
    }

    /**
     * Formats HP feedback for the native window title, requiring no font asset.
     * @return player/enemy HP, Boss phase/state/reward feedback and temporary key hints
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder("HP ")
                .append(session.getPlayer().getHealth().getCurrentHp()).append(elementLabel(player));
        for (int i = 0; i < enemies.size(); i++) {
            EnemyAI ai = enemies.get(i);
            status.append(" | E").append(i + 1).append(' ')
                    .append(ai.getEnemy().getHealth().getCurrentHp()).append(' ').append(ai.getState())
                    .append(elementLabel(ai.getCombatant()));
        }
        if (boss != null) {
            status.append(" | ").append(boss.getName()).append(' ').append(boss.getHealth().getCurrentHp())
                    .append('/').append(boss.getHealth().getMaxHp()).append(" P").append(boss.getPhase())
                    .append(' ').append(boss.getState()).append(elementLabel(boss.getCombatant()));
            boss.getDrop().ifPresent(drop -> status.append(" Drop ").append(drop.materialId())
                    .append(" x").append(drop.quantity()));
        }
        return status.append(" | ").append(session.getPlayer().getPlayerClass().getDisplayName())
                .append(" [F1 Archer/F2 Mage/F3 Katana] | J ").append(abilities.getState(AbilitySlot.LIGHT))
                .append(" K ").append(abilities.getState(AbilitySlot.SPECIAL))
                .append(" L ").append(abilities.getState(AbilitySlot.DASH))
                .append(" O ").append(abilities.getState(AbilitySlot.ULTIMATE))
                .append(" | R reset HP/abilities").toString();
    }

    /**
     * Adds an optional element name to native-title diagnostics without loading a font or status icon.
     * @param actor current-scope registered receiver
     * @return empty text or the attached element label
     */
    private String elementLabel(Combatant actor) {
        ElementType element = combat.getElementStatus(actor).element();
        return element == ElementType.NONE ? "" : " [" + element.name() + "]";
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
        if (abilities == null) {
            abilities = new PlayerAbilities(session.getPlayer(), player, combat);
        } else {
            abilities.bindAfterTransition(player);
        }
        enemies.clear();
        if (session.getCurrentMap().getType() == MapType.BATTLE) {
            for (CollisionBox placement : findEnemyPlacements()) {
                enemies.add(new EnemyAI(new Enemy(), placement, session.getCurrentMap(),
                        GameConfig.BASIC_ENEMY, combat));
            }
        }
        createBoss();
        transitionVersion = session.getTransitionVersion();
        return true;
    }

    /**
     * Creates a fresh debug encounter after scope clear/reset, without changing map definitions.
     * Skips blocked or actor-overlapping placements; no invalid Boss is forced into a map.
     * Unclaimed loot belongs to the previous encounter and is discarded on reset/travel.
     */
    private void createBoss() {
        boss = null;
        if (!session.getCurrentMap().getId().equals(MapCatalog.CROSSROADS)) {
            return;
        }
        for (CollisionBox placement : GameConfig.BOSS_PLACEMENTS) {
            if (session.getCurrentMap().isWalkable(placement) && !placement.overlaps(session.getPlayer().getBounds())
                    && enemies.stream().noneMatch(ai -> placement.overlaps(ai.getBounds()))) {
                boss = new Boss("sentinel", "Sentinel", placement, session.getCurrentMap(), GameConfig.SENTINEL, combat);
                return;
            }
        }
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

}
