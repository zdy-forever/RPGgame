package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.combat.ElementType;
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
import java.util.Objects;
import java.util.Optional;

/**
 * Graphics-free coordinator for player requests, one Boss per battle map and failure recovery.
 * Owns encounter placement/cleanup and post-settlement outcomes, not AI or damage rules.
 * Screen input supplies commands; common abilities own execution while rendering reads geometry.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class CombatDemo {
    /** Existing map/player coordinator; all transitions remain its responsibility. */
    private final GameSession session;
    /** Reused arena pipeline, cleared on every successful map transition. */
    private final CombatSystem combat;
    /** Shared player ability controller, retaining slot cooldowns across map transitions. */
    private PlayerAbilities abilities;
    /** Player adapter in the current scope, following the existing player's movement. */
    private Combatant player;
    /** Optional current-scope encounter; battle maps contain a Boss and no ordinary enemies. */
    private Boss boss;
    /** Last synchronized transition stamp, including re-entry to the same map. */
    private long transitionVersion = Long.MIN_VALUE;
    /** Latest completed failure return, retained for read-only diagnostics, never quest mutation. */
    private Defeat lastDefeat;

    /**
     * Identifies one failed battle scope without owning any Quest state or entity references.
     * Consumers compare encounterVersion to distinguish retries; this is a snapshot, not an event queue.
     * @param mapId battle map that was left
     * @param encounterVersion session transition stamp of the failed encounter
     * @author Daoyan Zhu (u7782042)
     */
    public record Defeat(String mapId, long encounterVersion) {
        /**
         * Creates a minimal immutable failure snapshot.
         * @param mapId non-blank battle map ID
         * @param encounterVersion failed session scope stamp
         * @throws NullPointerException if mapId is null
         * @throws IllegalArgumentException if mapId is blank
         */
        public Defeat {
            Objects.requireNonNull(mapId, "map ID");
            if (mapId.isBlank()) {
                throw new IllegalArgumentException("Failure requires a map ID");
            }
        }
    }

    /**
     * Creates and synchronizes the debug arena without loading assets or changing player health.
     * @param session existing game session
     * @throws NullPointerException if session is null
     */
    public CombatDemo(GameSession session) {
        this(session, new CombatSystem());
    }

    /**
     * Binds one shared pipeline for scene-level integration with configured combat rules.
     * Initial synchronization clears its previous scope; no second damage engine is created.
     * @param session existing map/player session
     * @param combat pipeline owned and advanced by this coordinator
     * @throws NullPointerException if either argument is null
     */
    CombatDemo(GameSession session, CombatSystem combat) {
        this.session = Objects.requireNonNull(session, "session");
        this.combat = Objects.requireNonNull(combat, "combat");
        syncMap();
    }

    /**
     * Refreshes hurtboxes, handles player requests, updates every AI, then advances combat once.
     * Map changes discard that frame's combat requests and old attacks; reset also consumes requests.
     * Ordinary travel does not heal; battle failure returns to Hub and restores health.
     * @param delta finite non-negative simulation seconds with representable enemy movement
     * @param playerAttack whether the player requested one strike
     * @param reset whether to clear attacks/cooldowns, reset awareness, and restore all actors' HP
     * @throws IllegalArgumentException if delta or the failure-return Hub/spawn is invalid
     */
    public void update(double delta, boolean playerAttack, boolean reset) {
        update(delta, new PlayerCommand(0, 0, playerAttack, false, false, false), reset);
    }

    /**
     * Coordinates common player movement/abilities, Boss decisions and one shared combat step.
     * Transitions discard commands and cancel active abilities, preserving slot cooldowns.
     * Reset consumes commands, restores health/cooldowns and recreates the Boss encounter without old loot.
     * @param delta finite non-negative seconds with representable player/enemy movement
     * @param command movement and ability requests sampled once by the input adapter
     * @param reset whether the explicit debug reset was requested
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delta or the failure-return Hub/spawn is invalid
     */
    public void update(double delta, PlayerCommand command, boolean reset) {
        update(delta, command, reset, null);
    }

    /**
     * Adds debug class selection to the normal coordinated simulation step.
     * Defeat takes priority over reset/selection; otherwise a selection attempt consumes new movement/ability
     * input, even if unsafe to switch. Existing dash/attacks and simulation time still advance.
     * @param delta finite non-negative seconds with representable player/enemy movement
     * @param command shared movement/ability intent
     * @param reset whether debug restoration was requested
     * @param classSelection newly requested class, or null when no selection was requested
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delta or the failure-return Hub/spawn is invalid
     */
    public void update(double delta, PlayerCommand command, boolean reset, PlayerClass classSelection) {
        update(delta, command, reset, classSelection, false);
    }

    /**
     * Adds a debug element cycle to the existing frame arbitration, without bypassing selection guards.
     * Settled battle death wins before reset, class/element selection or ordinary commands.
     * Death detected after combat returns to Hub before Boss reward settlement or further input.
     * A rejected selection still consumes new commands while existing simulation continues.
     * Class changes preserve the selected light-slot element; F4 assigns one element to all J/K/O slots.
     * @param delta finite non-negative simulation seconds
     * @param command normal movement/ability intent
     * @param reset explicit debug reset request
     * @param classSelection requested class, or null
     * @param cycleElement whether F4 requested the next debug element
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delta or the failure-return Hub/spawn is invalid
     */
    public void update(double delta, PlayerCommand command, boolean reset, PlayerClass classSelection, boolean cycleElement) {
        Objects.requireNonNull(command, "command");
        if (!Double.isFinite(delta) || delta < 0
                || !Double.isFinite(GameConfig.SENTINEL.navigation().speed() * delta)
                || !Double.isFinite(session.getPlayer().getSpeed() * delta)) {
            throw new IllegalArgumentException("Demo delta must be finite and non-negative");
        }
        if (returnToHubIfDefeated() || syncMap()) {
            return;
        }
        player.setHurtbox(playerHurtbox());
        if (reset) {
            combat.clear();
            session.getPlayer().getHealth().restoreFull();
            combat.register(player);
            abilities.reset();
            createBoss();
            return;
        }
        if (classSelection != null) {
            abilities.selectClass(PlayerClasses.get(classSelection)
                    .withElement(session.getPlayer().getClassDefinition().abilities().light().attack().element()));
        } else if (cycleElement) {
            ElementType current = session.getPlayer().getClassDefinition().abilities().light().attack().element();
            ElementType[] elements = ElementType.values();
            abilities.selectClass(session.getPlayer().getClassDefinition()
                    .withElement(elements[(current.ordinal() + 1) % elements.length]));
        }
        abilities.update(delta, classSelection != null || cycleElement ? PlayerCommand.NONE : command);
        if (boss != null) {
            boss.update(player, delta);
        }
        combat.update(delta);
        if (returnToHubIfDefeated()) {
            return;
        }
        if (boss != null) {
            boss.synchronizeOutcome();
        }
    }

    /**
     * Handles settled battle death once, outside Health/damage callbacks and before further input.
     * The screen also calls this before travel shortcuts to prevent an already-dead player escaping
     * defeat processing. A successful GameSession transition clears/rebinds the existing combat scope,
     * cancelling all actors, attacks, attachments, dash and launch states before restoreFull.
     * Player, Health, complete class/ultimate configuration and remaining slot cooldowns are retained.
     * Hub death is not a battle failure. A successful return cannot repeat because the player is
     * now alive in Hub. Invalid Hub configuration throws without healing in the battle map.
     * @return true only when this call completes a failure return
     * @throws IllegalArgumentException if Hub or its full-body spawn is invalid/missing
     */
    public boolean returnToHubIfDefeated() {
        if (session.getCurrentMap().getType() != MapType.BATTLE || session.getPlayer().isAlive()) {
            return false;
        }
        if (session.getMap(MapCatalog.HUB).getType() != MapType.HUB) {
            throw new IllegalArgumentException("Failure return requires a Hub map");
        }
        Defeat failure = new Defeat(session.getCurrentMap().getId(), session.getTransitionVersion());
        session.switchMap(MapCatalog.HUB);
        syncMap();
        session.getPlayer().getHealth().restoreFull();
        lastDefeat = failure;
        return true;
    }

    /**
     * Exposes the latest completed FAILURE without changing external quest state or retaining old actors.
     * @return last failed battle scope, or empty before any failure return
     */
    public Optional<Defeat> getLastDefeat() {
        return Optional.ofNullable(lastDefeat);
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
     * Retains the diagnostics API while the gameplay scene no longer spawns ordinary enemies.
     * Boss navigation still reuses EnemyAI internally; ordinary AI remains available to isolated tests.
     * @return immutable empty list under the Boss-only encounter rule
     */
    public List<EnemyAI> getEnemies() {
        return List.of();
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
        if (lastDefeat != null) {
            status.append(" | Last challenge FAILURE: ").append(lastDefeat.mapId());
        }
        if (boss != null) {
            status.append(" | ").append(boss.getName()).append(' ').append(boss.getHealth().getCurrentHp())
                    .append('/').append(boss.getHealth().getMaxHp()).append(" P").append(boss.getPhase())
                    .append(' ').append(boss.getState()).append(elementLabel(boss.getCombatant()));
            boss.getDrop().ifPresent(drop -> status.append(" Drop ").append(drop.materialId())
                    .append(" x").append(drop.quantity()));
        }
        return status.append(" | ").append(session.getPlayer().getPlayerClass().getDisplayName())
                .append(" | Element ").append(session.getPlayer().getClassDefinition().abilities().light().attack().element())
                .append(" [F4 cycle]")
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
        if (session.getCurrentMap().getType() != MapType.BATTLE) {
            return;
        }
        for (CollisionBox placement : GameConfig.BOSS_PLACEMENTS) {
            if (session.getCurrentMap().isWalkable(placement) && !placement.overlaps(session.getPlayer().getBounds())) {
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

}
