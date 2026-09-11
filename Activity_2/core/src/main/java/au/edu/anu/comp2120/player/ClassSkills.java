package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.Attack;
import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.world.CollisionBox;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Bounded class-skill state composed by PlayerAbilities, not another damage or movement engine.
 * update submits geometry; afterCombat observes accepted hits/damage and handles deferred responses.
 * All damage, including each field wave and counter segment, is delivered by CombatSystem.
 * @author Daoyan Zhu (u7782042)
 */
final class ClassSkills {
    /** Ordered Mage cycle and random pool, excluding NONE. */
    private static final List<ElementType> ELEMENTS = List.of(ElementType.FIRE, ElementType.WATER,
            ElementType.LIGHTNING, ElementType.ICE, ElementType.WIND);
    /** Persistent owner. */
    private final Player player;
    /** One shared damage pipeline. */
    private final CombatSystem combat;
    /** Injected deterministic-capable random source, used once per rapid projectile. */
    private final Random random;
    /** Current-scope canonical adapter. */
    private Combatant source;
    /** Current finite execution phase. */
    private Phase phase = Phase.NONE;
    /** Slot owning the current state. */
    private AbilitySlot slot = AbilitySlot.ULTIMATE;
    /** Captured ability tuning; selection changes cannot rewrite an active skill. */
    private AbilityLoadout.AreaAttack definition;
    /** Captured elemental tag, independent of subsequent selection. */
    private ElementType element = ElementType.NONE;
    /** Fixed field/follow-up geometry or last emitted movement position. */
    private CollisionBox anchor;
    /** Target identity for confirmed thrust follow-up, never a Boss type check. */
    private Combatant target;
    /** Last thrust probe observed only after combat settlement. */
    private Attack probe;
    /** Deferred source of the first intercepted attack; never executes inside interception. */
    private Combatant interceptedSource;
    /** Elapsed time in the current phase. */
    private double elapsed;
    /** Current simulation step, consumed once after combat. */
    private double frameDelta;
    /** Whether one submitted frame still needs post-combat processing. */
    private boolean pendingFrame;
    /** Number of scheduled damage waves already emitted. */
    private int emitted;
    /** Captured normalized movement direction x. */
    private double directionX;
    /** Captured normalized movement direction y. */
    private double directionY;
    /** Health damage stamp captured at cast start. */
    private long castDamageVersion;

    /**
     * Small explicit phase vocabulary; no event bus or per-state class hierarchy.
     * @author Daoyan Zhu (u7782042)
     */
    private enum Phase {
        /** No stateful skill. */ NONE,
        /** Fixed finite damage waves. */ FIELD,
        /** Interruptible preparation. */ CAST,
        /** Temporary targetability or projectile buff. */ BUFF,
        /** One K interception opportunity. */ PARRY,
        /** Locked missed-parry recovery. */ RECOVERY,
        /** Collision-constrained finite counter segments. */ COUNTER_DASH,
        /** One O interception opportunity. */ STANCE,
        /** Movement awaiting an accepted combat hit. */ THRUST,
        /** Bounded follow-up waves after a landed thrust. */ FINISHER
    }

    /**
     * Composes existing models and an injected random source.
     * @param player persistent owner
     * @param combat shared pipeline
     * @param random validated per-controller random source
     */
    ClassSkills(Player player, CombatSystem combat, Random random) {
        this.player = player;
        this.combat = combat;
        this.random = random;
    }

    /**
     * Binds defensive capabilities to the new canonical adapter, cancelling old activity.
     * @param next validated current-scope player adapter
     */
    void bind(Combatant next) {
        clear();
        source = next;
        source.setHitInterceptor(this::intercept);
    }

    /** Clears temporary states and adapter capabilities, never persistent element/mode choices. */
    void clear() {
        if (source != null) {
            source.setTargetable(true);
            source.setHitInterceptor(null);
        }
        phase = Phase.NONE;
        definition = null;
        target = null;
        probe = null;
        anchor = null;
        interceptedSource = null;
        elapsed = 0;
        emitted = 0;
        pendingFrame = false;
    }

    /**
     * Reports whether normal input/movement must be suppressed this frame.
     * @return true during cast, defence, recovery, thrust or follow-up execution
     */
    boolean locksInput() {
        return phase != Phase.NONE && phase != Phase.FIELD && phase != Phase.BUFF;
    }

    /**
     * Reports state ownership to the common slot lifecycle.
     * @param candidate slot to inspect
     * @return true while a stateful invocation owns this slot
     */
    boolean active(AbilitySlot candidate) {
        return phase != Phase.NONE && slot == candidate;
    }

    /**
     * Reports whether a stateful skill is still present, preventing definition/mode changes.
     * @return true for any active state or field
     */
    boolean busy() {
        return phase != Phase.NONE;
    }

    /**
     * Identifies active rapid-fire delivery without a class check in combat.
     * @return true during the rapid buff
     */
    boolean rapid() {
        return phase == Phase.BUFF && definition.kind() == SkillKind.RAPID_BARRAGE;
    }

    /**
     * Samples one independent random element; two calls may return the same value.
     * @return one of the five non-NONE elements
     */
    ElementType randomElement() {
        return ELEMENTS.get(random.nextInt(ELEMENTS.size()));
    }

    /**
     * Exposes state text without loading fonts or effects.
     * @return current phase name
     */
    String status() {
        return phase.name();
    }

    /**
     * Starts a stateful slot after common cooldown arbitration; no damage is settled here.
     * @param requested owning slot
     * @param skill immutable skill definition
     * @return true if accepted, false when another state owns execution or a required target is absent
     * @throws IllegalArgumentException if the definition requests direct rather than stateful delivery
     */
    boolean start(AbilitySlot requested, AbilityLoadout.AreaAttack skill) {
        if (skill.kind() == SkillKind.ELEMENT_CYCLE) {
            if (locksInput()) {
                return false;
            }
            player.applyMageElement(ELEMENTS.get((ELEMENTS.indexOf(player.getMageElement()) + 1) % ELEMENTS.size()));
            return true;
        }
        if (busy()) {
            return false;
        }
        Combatant selected = chooseTarget().orElse(null);
        if ((skill.kind() == SkillKind.ARROW_RAIN || skill.kind() == SkillKind.MAGIC_CIRCLE
                || skill.kind() == SkillKind.DRAGON_THRUST) && selected == null) {
            return false;
        }
        definition = skill;
        slot = requested;
        target = selected;
        element = skill.kind() == SkillKind.MAGIC_CIRCLE ? player.getMageElement() : skill.attack().element();
        elapsed = 0;
        emitted = 0;
        source.setHitInterceptor(this::intercept);
        switch (skill.kind()) {
            case ARROW_RAIN -> {
                anchor = expanded(selected.getHurtbox(), skill.range());
                phase = Phase.FIELD;
            }
            case MAGIC_CIRCLE -> {
                anchor = expanded(selected.getHurtbox(), skill.range());
                castDamageVersion = player.getHealth().getDamageVersion();
                phase = Phase.CAST;
            }
            case UNTARGETABLE -> {
                source.setTargetable(false);
                phase = Phase.BUFF;
            }
            case RAPID_BARRAGE -> phase = Phase.BUFF;
            case PARRY -> phase = Phase.PARRY;
            case COUNTER_STANCE -> phase = Phase.STANCE;
            case DRAGON_THRUST -> {
                aimAt(selected);
                phase = Phase.THRUST;
            }
            default -> throw new IllegalArgumentException("Not a stateful skill: " + skill.kind());
        }
        return true;
    }

    /**
     * Submits bounded geometry/movement for the upcoming shared combat step.
     * No timer advances twice on zero-time input; at most the configured total waves can exist.
     * @param delta validated non-negative frame time
     */
    void update(double delta) {
        frameDelta = delta;
        pendingFrame = true;
        if (phase == Phase.CAST && player.getHealth().getDamageVersion() != castDamageVersion) {
            end();
        }
        if (phase == Phase.FIELD || phase == Phase.FINISHER) {
            int count = phase == Phase.FINISHER ? SkillTuning.DRAGON_TICKS
                    : definition.kind() == SkillKind.ARROW_RAIN ? SkillTuning.RAIN_TICKS : SkillTuning.FIELD_TICKS;
            double interval = phase == Phase.FINISHER ? SkillTuning.DRAGON_INTERVAL
                    : definition.kind() == SkillKind.ARROW_RAIN ? SkillTuning.RAIN_INTERVAL : SkillTuning.FIELD_INTERVAL;
            while (emitted < count && emitted * interval <= elapsed + delta) {
                emit(anchor, anchor, definition.attack().damage());
                emitted++;
            }
        } else if (phase == Phase.COUNTER_DASH) {
            double until = Math.min(SkillTuning.COUNTER_SECONDS, elapsed + delta);
            double interval = SkillTuning.COUNTER_SECONDS / SkillTuning.COUNTER_TICKS;
            double cursor = elapsed;
            while (emitted < SkillTuning.COUNTER_TICKS && (emitted + 1) * interval <= until) {
                double boundary = (emitted + 1) * interval;
                move(SkillTuning.COUNTER_SPEED * Math.max(0, boundary - cursor));
                emit(swept(anchor, player.getBounds(), definition.range()), anchor, definition.attack().damage());
                anchor = player.getBounds();
                cursor = boundary;
                emitted++;
            }
            move(SkillTuning.COUNTER_SPEED * Math.max(0, until - cursor));
        } else if (phase == Phase.THRUST && delta > 0) {
            CollisionBox before = player.getBounds();
            move(SkillTuning.THRUST_SPEED * Math.min(delta, Math.max(0, SkillTuning.THRUST_SECONDS - elapsed)));
            probe = emit(swept(before, player.getBounds(), 0), before, SkillTuning.THRUST_DAMAGE).orElse(null);
        }
    }

    /** Observes effective damage/accepted hits, then ages the one submitted frame exactly once. */
    void afterCombat() {
        if (!pendingFrame) {
            return;
        }
        pendingFrame = false;
        if (!player.isAlive() || !combat.getCombatants().contains(source)) {
            clear();
            return;
        }
        if (phase == Phase.CAST && player.getHealth().getDamageVersion() != castDamageVersion) {
            end();
            return;
        }
        if (interceptedSource != null) {
            if (phase == Phase.PARRY) {
                aimAt(interceptedSource);
                anchor = player.getBounds();
                phase = Phase.COUNTER_DASH;
                elapsed = 0;
                emitted = 0;
            } else if (phase == Phase.STANCE) {
                emit(expanded(source.getHurtbox(), definition.range()), source.getHurtbox(), definition.attack().damage());
                end();
            }
            interceptedSource = null;
            return;
        }
        if (phase == Phase.THRUST && probe != null && target != null && probe.hasLandedHit(target.getEntity())) {
            phase = Phase.FINISHER;
            anchor = expanded(target.getHurtbox(), definition.range());
            probe = null;
            elapsed = 0;
            emitted = 0;
            return;
        }
        elapsed += frameDelta;
        if (phase == Phase.CAST && elapsed >= SkillTuning.CAST_SECONDS) {
            phase = Phase.FIELD;
            elapsed = 0;
        } else if (phase == Phase.PARRY && elapsed >= SkillTuning.PARRY_SECONDS) {
            phase = Phase.RECOVERY;
            elapsed = 0;
        } else if (phase == Phase.RECOVERY && elapsed >= SkillTuning.RECOVERY_SECONDS
                || phase == Phase.STANCE && elapsed >= SkillTuning.STANCE_SECONDS
                || phase == Phase.COUNTER_DASH && elapsed >= SkillTuning.COUNTER_SECONDS
                || phase == Phase.THRUST && elapsed >= SkillTuning.THRUST_SECONDS
                || phase == Phase.FINISHER && elapsed >= SkillTuning.DRAGON_TICKS * SkillTuning.DRAGON_INTERVAL
                || phase == Phase.FIELD && elapsed >= fieldDuration()
                || phase == Phase.BUFF && elapsed >= (definition.kind() == SkillKind.UNTARGETABLE
                        ? SkillTuning.UNTARGETABLE_SECONDS : SkillTuning.RAPID_SECONDS)) {
            end();
        }
    }

    /**
     * Latches at most one eligible defensive response without executing it in the combat callback.
     * @param incoming confirmed blockable hit
     * @return true only for the first positive-damage hostile hit during an active defence window
     */
    private boolean intercept(Attack incoming) {
        if ((phase != Phase.PARRY && phase != Phase.STANCE) || interceptedSource != null
                || incoming.getSpecification().damage() <= 0 || !player.isAlive()
                || incoming.getSource().getFaction() == source.getFaction()) {
            return false;
        }
        interceptedSource = incoming.getSource();
        return true;
    }

    /** Ends execution without refunding slot cooldown or changing persistent choices. */
    private void end() {
        phase = Phase.NONE;
        source.setTargetable(true);
        target = null;
        probe = null;
        interceptedSource = null;
    }

    /**
     * Resolves the finite field duration from centralized tuning.
     * @return field lifetime in seconds
     */
    private double fieldDuration() {
        return definition.kind() == SkillKind.ARROW_RAIN
                ? SkillTuning.RAIN_TICKS * SkillTuning.RAIN_INTERVAL
                : SkillTuning.FIELD_TICKS * SkillTuning.FIELD_INTERVAL;
    }

    /**
     * Chooses the nearest eligible hostile adapter; production has one Boss, but no type is hard-coded.
     * @return live target, if present
     */
    private Optional<Combatant> chooseTarget() {
        return combat.getCombatants().stream().filter(candidate -> candidate != source && candidate.isTargetable()
                && candidate.getFaction() != source.getFaction())
                .min(Comparator.comparingDouble(candidate -> Math.hypot(
                        centreX(candidate.getHurtbox()) - centreX(player.getBounds()),
                        centreY(candidate.getHurtbox()) - centreY(player.getBounds()))));
    }

    /**
     * Captures a direction toward the selected hostile, using right for coincident centres.
     * @param receiver target at activation/interception time
     */
    private void aimAt(Combatant receiver) {
        double dx = centreX(receiver.getHurtbox()) - centreX(player.getBounds());
        double dy = centreY(receiver.getHurtbox()) - centreY(player.getBounds());
        double length = Math.hypot(dx, dy);
        directionX = length == 0 ? 1 : dx / length;
        directionY = length == 0 ? 0 : dy / length;
    }

    /**
     * Moves through the existing world collision resolver; no teleport or immunity is implied.
     * @param distance non-negative finite travel budget
     */
    private void move(double distance) {
        player.moveBy(directionX * distance, directionY * distance);
    }

    /**
     * Submits one non-blockable finite effect to the shared occlusion/damage/element pipeline.
     * @param box geometry for this wave
     * @param origin fixed effect or movement origin
     * @param damage configured damage, never applied directly
     * @return accepted invocation
     */
    private Optional<Attack> emit(CollisionBox box, CollisionBox origin, int damage) {
        return combat.tryAttack(source, new AttackSpec(definition.attack().type(), damage,
                SkillTuning.TICK_ACTIVE, 0, element, false), box, origin);
    }

    /**
     * Expands geometry without consulting visual assets.
     * @param box base rectangle
     * @param range expansion on each side
     * @return fixed effect rectangle
     */
    private static CollisionBox expanded(CollisionBox box, double range) {
        return new CollisionBox(box.x() - range, box.y() - range, box.width() + 2 * range, box.height() + 2 * range);
    }

    /**
     * Covers a resolved movement segment; the shared occlusion gate still rejects walls.
     * @param before start body
     * @param after resolved end body
     * @param expansion contact margin
     * @return rectangular swept placeholder
     */
    private static CollisionBox swept(CollisionBox before, CollisionBox after, double expansion) {
        double x = Math.min(before.x(), after.x());
        double y = Math.min(before.y(), after.y());
        return expanded(new CollisionBox(x, y, Math.max(before.right(), after.right()) - x,
                Math.max(before.top(), after.top()) - y), expansion);
    }

    /**
     * Computes horizontal rectangle centre.
     * @param box geometry
     * @return centre x
     */
    private static double centreX(CollisionBox box) {
        return box.x() + box.width() / 2;
    }

    /**
     * Computes vertical rectangle centre.
     * @param box geometry
     * @return centre y
     */
    private static double centreY(CollisionBox box) {
        return box.y() + box.height() / 2;
    }
}
