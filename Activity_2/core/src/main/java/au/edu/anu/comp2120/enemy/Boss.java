package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Health;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One Boss encounter composed of existing Enemy health, EnemyAI navigation and shared combat.
 * The canonical damage receiver is the composed Enemy, never a second HP model or adapter.
 * Call update before the arena's single combat step, then synchronizeOutcome afterwards.
 * Defeat is terminal for this encounter; explicit debug respawn creates a new Boss instance.
 * All methods belong on the game-loop thread, outside damage receiver callbacks.
 * @author Daoyan Zhu (u7782042)
 */
public final class Boss {
    /** Reusable non-blank Boss identity, distinct from a reward instance ID. */
    private final String id;
    /** Debug display name, independent of artwork. */
    private final String name;
    /** Immutable encounter tuning. */
    private final BossConfig config;
    /** Fixed map owning this encounter and its loot. */
    private final GameMap map;
    /** Existing health-owning hostile entity. */
    private final Enemy enemy;
    /** Existing cached A* pursuit and collision controller. */
    private final EnemyAI navigation;
    /** Shared pipeline, advanced only by the arena owner. */
    private final CombatSystem combat;
    /** One-way phase index, initially one. */
    private int phase = 1;
    /** Index of the next pattern within the current phase. */
    private int nextPattern;
    /** Remaining one-time phase-change pause, measured in simulation seconds. */
    private double transitionRemaining;
    /** Latched defeat prevents revival or repeated drops on the same encounter. */
    private boolean defeated;
    /** Pending reward; null before death or after its one-time claim. */
    private MaterialDrop drop;

    /**
     * Creates and registers an encounter using shared Enemy health and navigation.
     * @param id non-blank Boss identity
     * @param name non-blank display name
     * @param bounds fully walkable collision/hurtbox rectangle, independent of final visuals
     * @param map owning map
     * @param config immutable encounter definition
     * @param combat shared arena pipeline
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if identity, placement, navigation geometry or combat world binding is invalid
     */
    public Boss(String id, String name, CollisionBox bounds, GameMap map, BossConfig config, CombatSystem combat) {
        this.id = Objects.requireNonNull(id, "Boss ID");
        this.name = Objects.requireNonNull(name, "Boss name");
        this.map = Objects.requireNonNull(map, "map");
        this.config = Objects.requireNonNull(config, "config");
        this.combat = Objects.requireNonNull(combat, "combat");
        if (id.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("Boss identity and name must not be blank");
        }
        enemy = new Enemy(config.maxHp());
        navigation = new EnemyAI(enemy, bounds, map, config.navigation(), combat);
    }

    /**
     * Advances encounter decisions, never combat time or damage directly.
     * Pattern index advances only on an accepted attack; shared source cooldown blocks spam.
     * Captures the pattern-selected origin; combat-core rechecks wall occlusion when each target settles.
     * Transition frames consume time without attacks/movement, discarding any excess frame time.
     * Generic freeze/stagger pauses decisions; navigation still resolves queued collision-safe knockback.
     * @param target eligible canonical hostile target, or null
     * @param delta finite non-negative simulation seconds with representable movement
     * @throws IllegalArgumentException if time or resulting geometry is invalid
     * @throws NullPointerException if a custom pattern returns null geometry
     */
    public void update(Combatant target, double delta) {
        if (!Double.isFinite(delta) || delta < 0 || !Double.isFinite(config.navigation().speed() * delta)) {
            throw new IllegalArgumentException("Boss delta must give finite non-negative movement");
        }
        synchronizeOutcome();
        if (defeated || !registered()) {
            return;
        }
        if (getCombatant().getStatus().blocksActions()) {
            navigation.update(null, delta, false);
            return;
        }
        if (transitionRemaining > 0) {
            transitionRemaining = Math.max(0, transitionRemaining - delta);
            return;
        }
        navigation.update(target, delta, false);
        if (navigation.getState() != EnemyState.ATTACK) {
            return;
        }
        List<BossConfig.Pattern> cycle = phase == 1 ? config.phaseOne() : config.phaseTwo();
        BossConfig.Pattern pattern = cycle.get(nextPattern);
        CollisionBox body = getBounds();
        CollisionBox hurtbox = target.getHurtbox();
        double dx = hurtbox.x() + hurtbox.width() / 2 - (body.x() + body.width() / 2);
        double dy = hurtbox.y() + hurtbox.height() / 2 - (body.y() + body.height() / 2);
        if (dx == 0 && dy == 0) {
            dx = 1;
        }
        CollisionBox hitbox = Objects.requireNonNull(pattern.shape().create(body, pattern.range(), dx, dy),
                "Boss pattern geometry");
        if (combat.tryAttack(getCombatant(), pattern.attack(), hitbox, pattern.shape().origin(body, hitbox)).isPresent()) {
            nextPattern = (nextPattern + 1) % cycle.size();
        }
    }

    /**
     * Observes settled HP after combat, latching defeat/drop or the one-way second phase.
     * Death takes precedence over phase change. Repeated calls never duplicate rewards or restart
     * transition time. Detached old-map encounters do not generate loot. Cancelling attacks preserves
     * remaining cooldowns and never damages another entity.
     */
    public void synchronizeOutcome() {
        if (defeated || !registered()) {
            return;
        }
        if (!enemy.isAlive()) {
            defeated = true;
            transitionRemaining = 0;
            combat.cancelAttacks(getCombatant());
            CollisionBox body = getBounds();
            double size = Math.min(12, Math.min(body.width(), body.height()));
            CollisionBox placement = new CollisionBox(body.x() + (body.width() - size) / 2,
                    body.y() + (body.height() - size) / 2, size, size);
            drop = new MaterialDrop(UUID.randomUUID().toString(), config.materialId(), config.materialQuantity(),
                    map.getId(), placement);
        } else if (phase == 1 && getHealth().getCurrentHp() / (double) getHealth().getMaxHp() <= config.phaseTwoFraction()) {
            phase = 2;
            nextPattern = 0;
            transitionRemaining = config.transitionSeconds();
            navigation.resetAwareness();
            combat.cancelAttacks(getCombatant());
        }
    }

    /**
     * Returns the current decision with immediate health-aware death reporting.
     * @return encounter state
     */
    public BossState getState() {
        if (!isAlive()) {
            return BossState.DEAD;
        }
        if (!registered()) {
            return BossState.IDLE;
        }
        if (getCombatant().getStatus().isFrozen()) {
            return BossState.FROZEN;
        }
        if (getCombatant().getStatus().blocksActions()) {
            return BossState.STAGGERED;
        }
        if (transitionRemaining > 0) {
            return BossState.PHASE_TRANSITION;
        }
        return switch (navigation.getState()) {
            case CHASE -> BossState.CHASE;
            case ATTACK -> BossState.ATTACK;
            case IDLE, DEAD -> BossState.IDLE;
        };
    }

    /**
     * Returns the reusable identity.
     * @return stable Boss identity
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display label.
     * @return debug name independent of assets
     */
    public String getName() {
        return name;
    }

    /**
     * Exposes validated health operations.
     * @return shared health owned by the composed Enemy, not a copy
     */
    public Health getHealth() {
        return enemy.getHealth();
    }

    /**
     * Reports encounter life status.
     * @return true while HP remains and this encounter has not been defeated
     */
    public boolean isAlive() {
        return !defeated && enemy.isAlive();
    }

    /**
     * Exposes current position and size.
     * @return current collision body and canonical hurtbox
     */
    public CollisionBox getBounds() {
        return navigation.getBounds();
    }

    /**
     * Returns the shared damage receiver.
     * @return canonical Enemy adapter registered with combat-core
     */
    public Combatant getCombatant() {
        return navigation.getCombatant();
    }

    /**
     * Reports the entered HP phase.
     * @return one-way phase number, one or two
     */
    public int getPhase() {
        return phase;
    }

    /**
     * Reports remaining transition time.
     * @return non-negative remaining transition seconds
     */
    public double getTransitionRemaining() {
        return transitionRemaining;
    }

    /**
     * Returns the unclaimed reward only while this encounter belongs to its arena.
     * @return pending immutable reward, empty before defeat, after claiming or after detachment
     */
    public Optional<MaterialDrop> getDrop() {
        return registered() ? Optional.ofNullable(drop) : Optional.empty();
    }

    /**
     * Transfers the pending drop at most once to a future inventory/pickup adapter.
     * The caller must check capacity/acceptance before claiming, on the game-loop thread.
     * This foundation does not store items, enforce pickup distance or persist rewards.
     * @return reward transferred to the caller, or empty when none remains
     */
    public Optional<MaterialDrop> claimDrop() {
        Optional<MaterialDrop> pending = getDrop();
        drop = null;
        return pending;
    }

    /**
     * Checks arena membership.
     * @return whether this encounter still belongs to the active combat scope
     */
    private boolean registered() {
        return combat.getCombatants().contains(getCombatant());
    }
}
