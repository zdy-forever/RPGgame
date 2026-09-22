package au.edu.anu.comp2120.enemy;

import au.edu.anu.comp2120.combat.AttackSpec;
import au.edu.anu.comp2120.combat.CombatSystem;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.screen.GameConfig;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.MapType;
import au.edu.anu.comp2120.world.SpawnPoint;
import au.edu.anu.comp2120.world.TestMaps;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises each campaign guardian's signature trait through the real Boss and shared pipeline.
 * Traits must never damage a health model directly, so every effect is observed after settlement.
 * @author Daoyan Zhu (u7782042)
 */
final class BossTraitTest {
    /** Distance at which the fixture target is outside detection, so only traits can reach it. */
    private static final CollisionBox SAFE_SPOT = new CollisionBox(900, 700, 20, 20);

    /** Creates independent fixtures without graphics or audio. */
    BossTraitTest() { }

    /** Arenas outside the campaign keep the shared template and gain no extra behaviour at all. */
    @Test
    void customArenasKeepTheSharedTemplateUnchanged() {
        GameMap map = TestMaps.rectangle(1000, 800, List.of());
        CombatSystem combat = new CombatSystem();
        Boss boss = new Boss("custom", "Custom", new CollisionBox(200, 200, 64, 64),
                map, GameConfig.SENTINEL, combat);
        Enemy victim = new Enemy(1000);
        Combatant target = new Combatant(victim, Faction.PLAYER, new CollisionBox(290, 220, 20, 20));
        combat.register(target);
        assertSame(BossTrait.NONE, BossTraits.forMap(map.getId()));
        assertSame(GameConfig.SENTINEL, BossTrait.NONE.tune(GameConfig.SENTINEL));
        assertEquals("", boss.getTraitStatus());
        assertEquals(GameConfig.SENTINEL.maxHp(), boss.getHealth().getMaxHp());
        Fixture custom = new Fixture(map, combat, boss, victim, target);
        land(custom);
        int direct = GameConfig.SENTINEL.phaseOne().get(0).attack().damage();
        assertEquals(1000 - direct, victim.getHealth().getCurrentHp(), "Untuned direct damage only");
        assertEquals(0, target.getStatus().getKnockbackX(), "No shove outside a campaign arena");
        assertEquals(0, target.getStatus().getFrozenRemaining(), "No freeze outside a campaign arena");
        CollisionBox settled = boss.getBounds();
        boss.getHealth().takeDamage(BossTraits.BLINK_DAMAGE);
        for (int i = 0; i < 20; i++) {
            boss.update(target, BossTraits.RETRIBUTION_DELAY);
            combat.update(0);
        }
        assertEquals(1000 - direct, victim.getHealth().getCurrentHp(), "No burn or punishment pulses");
        assertEquals(settled, boss.getBounds(), "No blink outside a campaign arena");
    }

    /**
     * Every guardian shares health, reward, phase timing and attack geometry; only feel differs.
     * @param guardian campaign target under test
     */
    @ParameterizedTest
    @EnumSource(BossChallenge.class)
    void guardiansShareTheTemplateExceptMovementAndFireDamage(BossChallenge guardian) {
        BossConfig base = GameConfig.SENTINEL;
        BossConfig tuned = BossTraits.forChallenge(guardian).tune(base);
        assertEquals(base.maxHp(), tuned.maxHp(), "One health change must still reach every guardian");
        assertEquals(base.materialId(), tuned.materialId());
        assertEquals(base.materialQuantity(), tuned.materialQuantity());
        assertEquals(base.phaseTwoFraction(), tuned.phaseTwoFraction());
        assertEquals(base.transitionSeconds(), tuned.transitionSeconds());
        assertEquals(base.phaseOne().size(), tuned.phaseOne().size());
        assertEquals(base.navigation().detectionRange(), tuned.navigation().detectionRange());
        assertEquals(base.navigation().loseRange(), tuned.navigation().loseRange());
        assertEquals(base.navigation().attackRange(), tuned.navigation().attackRange());
        assertNotEquals(base.navigation().speed(), tuned.navigation().speed(), "Movement feel must differ");
        for (int i = 0; i < base.phaseOne().size(); i++) {
            BossConfig.Pattern original = base.phaseOne().get(i);
            BossConfig.Pattern current = tuned.phaseOne().get(i);
            assertEquals(original.range(), current.range());
            assertSame(original.shape(), current.shape());
            assertEquals(original.attack().cooldownSeconds(), current.attack().cooldownSeconds());
            assertEquals(original.attack().activeSeconds(), current.attack().activeSeconds());
            int expected = guardian == BossChallenge.FIRE
                    ? (int) Math.round(original.attack().damage() * BossTraits.BURN_DIRECT_DAMAGE_SHARE)
                    : original.attack().damage();
            assertEquals(expected, current.attack().damage(),
                    "Only the burning guardian trades direct damage for its trait");
        }
    }

    /** Water shoves the struck target directly outward for its own collision resolver to apply. */
    @Test
    void waterPushesTheStruckTargetOutward() {
        Fixture f = fixture(BossChallenge.WATER);
        land(f);
        double pushX = f.target.getStatus().getKnockbackX();
        double pushY = f.target.getStatus().getKnockbackY();
        assertTrue(pushX > 0, "Push must move the target away from the guardian, not toward it");
        assertEquals(BossTraits.PUSH_DISTANCE, Math.hypot(pushX, pushY), 1e-9);
        assertTrue(f.target.getStatus().getFrozenRemaining() == 0
                && f.target.getStatus().getStaggerRemaining() == 0, "A shove must not also lock actions");
        assertTrue(f.boss.getTraitStatus().contains("Push x1"));
    }

    /** Burning stacks per landed hit, ticks on the configured interval and expires after five ticks. */
    @Test
    void fireBurnStacksTickOnIntervalAndExpire() {
        Fixture f = fixture(BossChallenge.FIRE);
        land(f);
        int afterHit = f.victim.getHealth().getCurrentHp();
        assertTrue(f.boss.getTraitStatus().contains("Burn x1"));
        f.target.setHurtbox(SAFE_SPOT);
        f.boss.update(f.target, BossTraits.BURN_INTERVAL / 2);
        f.combat.update(0);
        assertEquals(afterHit, f.victim.getHealth().getCurrentHp(), "No tick before the interval elapses");
        for (int tick = 1; tick <= BossTraits.BURN_TICKS; tick++) {
            f.boss.update(f.target, BossTraits.BURN_INTERVAL / 2);
            f.combat.update(0);
            assertEquals(afterHit - tick * BossTraits.BURN_DAMAGE_PER_STACK,
                    f.victim.getHealth().getCurrentHp(), "Tick " + tick + " must deal one stack of damage");
            f.boss.update(f.target, BossTraits.BURN_INTERVAL / 2);
            f.combat.update(0);
        }
        int expired = f.victim.getHealth().getCurrentHp();
        for (int i = 0; i < 10; i++) {
            f.boss.update(f.target, BossTraits.BURN_INTERVAL);
            f.combat.update(0);
        }
        assertEquals(expired, f.victim.getHealth().getCurrentHp(), "Burning must stop after its ticks");
        assertEquals("", f.boss.getTraitStatus());
    }

    /** Stacks multiply one tick's damage and never exceed the configured cap. */
    @Test
    void fireBurnStacksAreCappedAndMultiplyOneTick() {
        Fixture f = fixture(BossChallenge.FIRE);
        for (int hit = 0; hit < BossTraits.MAX_BURN_STACKS + 2; hit++) {
            land(f);
            f.combat.update(2);
        }
        assertTrue(f.boss.getTraitStatus().contains("Burn x" + BossTraits.MAX_BURN_STACKS));
        f.target.setHurtbox(SAFE_SPOT);
        int before = f.victim.getHealth().getCurrentHp();
        f.boss.update(f.target, BossTraits.BURN_INTERVAL);
        f.combat.update(0);
        assertEquals(before - BossTraits.MAX_BURN_STACKS * BossTraits.BURN_DAMAGE_PER_STACK,
                f.victim.getHealth().getCurrentHp());
    }

    /** Lightning punishes a challenger who stops trading, and any damage restarts the countdown. */
    @Test
    void lightningStrikesAfterIdleSecondsAndDamageResetsIt() {
        Fixture f = fixture(BossChallenge.LIGHTNING);
        f.target.setHurtbox(SAFE_SPOT);
        f.boss.update(f.target, BossTraits.RETRIBUTION_DELAY - 1);
        f.combat.update(0);
        assertEquals(1000, f.victim.getHealth().getCurrentHp(), "No strike before the delay elapses");
        f.boss.getHealth().takeDamage(20);
        f.boss.update(f.target, 0.9);
        f.combat.update(0);
        assertEquals(1000, f.victim.getHealth().getCurrentHp(), "Damage must restart the countdown");
        f.boss.update(f.target, BossTraits.RETRIBUTION_DELAY);
        f.combat.update(0);
        assertEquals(1000 - BossTraits.RETRIBUTION_DAMAGE, f.victim.getHealth().getCurrentHp());
        assertEquals(BossTraits.RETRIBUTION_STAGGER, f.target.getStatus().getStaggerRemaining(), 1e-9);
        assertTrue(f.target.getStatus().blocksActions());
        assertTrue(f.boss.getTraitStatus().contains("(1)"));
    }

    /** Ice freezes on every third landed hit and starts counting again afterwards. */
    @Test
    void iceFreezesOnEveryThirdLandedHit() {
        Fixture f = fixture(BossChallenge.ICE);
        for (int hit = 1; hit < BossTraits.FROSTBITE_HITS; hit++) {
            land(f);
            assertEquals(0, f.target.getStatus().getFrozenRemaining(), "Only the third hit may freeze");
            assertTrue(f.boss.getTraitStatus().contains(hit + "/" + BossTraits.FROSTBITE_HITS));
            f.combat.update(2);
        }
        land(f);
        assertEquals(BossTraits.FROSTBITE_FREEZE, f.target.getStatus().getFrozenRemaining(), 1e-9);
        assertTrue(f.target.getStatus().isFrozen());
        assertTrue(f.boss.getTraitStatus().contains("0/" + BossTraits.FROSTBITE_HITS));
        assertTrue(f.boss.getTraitStatus().contains("(1)"));
        assertFalse(f.boss.getCombatant().getStatus().blocksActions(),
                "Freezing must not also suppress the guardian itself");
    }

    /** The burning guardian names its affliction for display above the target it burns. */
    @Test
    void burningGuardianReportsItsAfflictionLabel() {
        Fixture f = fixture(BossChallenge.FIRE);
        assertEquals("", f.boss.getAfflictionLabel(), "Nothing is shown before the first hit");
        land(f);
        assertEquals("BURNING x1", f.boss.getAfflictionLabel());
        f.combat.update(2);
        land(f);
        assertEquals("BURNING x2", f.boss.getAfflictionLabel());
        f.boss.getHealth().takeDamage(f.boss.getHealth().getMaxHp());
        f.boss.update(f.target, 0);
        assertEquals("", f.boss.getAfflictionLabel(), "A defeated guardian holds no affliction");
    }

    /**
     * Guardians without an owned affliction report none, leaving only shared status labels.
     * @param guardian campaign target under test
     */
    @ParameterizedTest
    @EnumSource(value = BossChallenge.class, names = {"WATER", "LIGHTNING", "ICE", "WIND"})
    void otherGuardiansReportNoOwnedAffliction(BossChallenge guardian) {
        Fixture f = fixture(guardian);
        land(f);
        assertEquals("", f.boss.getAfflictionLabel());
    }

    /** Wind vanishes on burst damage, keeps a legal body and will not blink again immediately. */
    @Test
    void windBlinksOnBurstDamageThenCoolsDown() {
        Fixture f = fixture(BossChallenge.WIND);
        CollisionBox start = f.boss.getBounds();
        f.boss.getHealth().takeDamage(BossTraits.BLINK_DAMAGE - 1);
        f.boss.update(f.target, 0);
        assertEquals(start, f.boss.getBounds(), "Chip damage must not trigger the escape");
        f.boss.getHealth().takeDamage(1);
        f.boss.update(f.target, 0);
        CollisionBox escaped = f.boss.getBounds();
        assertNotEquals(start, escaped);
        assertTrue(f.map.isWalkable(escaped), "A blink must land on a fully walkable body");
        assertEquals(escaped, f.boss.getCombatant().getHurtbox());
        assertTrue(f.boss.getMeleeWarning().isEmpty(), "A blink must drop its prepared strike");
        assertTrue(f.boss.getTraitStatus().contains("(1)"));
        f.boss.getHealth().takeDamage(BossTraits.BLINK_DAMAGE);
        f.boss.update(f.target, 0);
        assertEquals(escaped, f.boss.getBounds(), "Cooldown must prevent an immediate second blink");
        f.boss.update(f.target, BossTraits.BLINK_COOLDOWN);
        f.boss.getHealth().takeDamage(BossTraits.BLINK_DAMAGE);
        f.boss.update(f.target, 0);
        assertNotEquals(escaped, f.boss.getBounds());
    }

    /**
     * Every random destination is legal wherever it lands: inside the arena, fully walkable,
     * clear of the target and never the spot the guardian already occupied.
     */
    @Test
    void windBlinkDestinationsAreRandomButAlwaysLegal() {
        Fixture f = fixture(BossChallenge.WIND);
        java.util.Set<CollisionBox> seen = new java.util.HashSet<>();
        CollisionBox previous = f.boss.getBounds();
        CollisionBox arena = f.map.getBounds();
        for (int blink = 0; blink < 40; blink++) {
            f.boss.getHealth().restoreFull();
            f.boss.update(f.target, BossTraits.BLINK_COOLDOWN);
            f.combat.update(BossTraits.BLINK_INVULNERABILITY);
            f.boss.getHealth().takeDamage(BossTraits.BLINK_DAMAGE);
            f.boss.update(f.target, 0);
            CollisionBox landed = f.boss.getBounds();
            assertNotEquals(previous, landed, "A blink must always relocate the guardian");
            assertTrue(f.map.isWalkable(landed), "Random destinations must still be walkable");
            assertTrue(landed.x() >= arena.x() && landed.y() >= arena.y()
                            && landed.right() <= arena.right() && landed.top() <= arena.top(),
                    "Random destinations must stay inside the arena");
            assertFalse(landed.overlaps(f.target.getHurtbox()), "A blink must not land on the target");
            assertEquals(landed, f.boss.getCombatant().getHurtbox());
            seen.add(landed);
            previous = landed;
        }
        assertTrue(seen.size() > 1, "Destinations must not be a single fixed spot");
    }

    /** The arrival is briefly untouchable, which ends the burst instead of only relocating it. */
    @Test
    void windIsUntouchableRightAfterBlinking() {
        Fixture f = fixture(BossChallenge.WIND);
        f.boss.getHealth().takeDamage(BossTraits.BLINK_DAMAGE);
        f.boss.update(f.target, 0);
        var status = f.boss.getCombatant().getStatus();
        assertEquals(BossTraits.BLINK_INVULNERABILITY, status.getInvulnerableRemaining(), 1e-9);
        assertTrue(status.isInvulnerable());
        int guarded = f.boss.getHealth().getCurrentHp();
        f.target.setHurtbox(f.boss.getBounds());
        for (int i = 0; i < 10; i++) {
            f.combat.tryAttack(f.target, new AttackSpec("immunity-test", 50, 0.1, 0, ElementType.FIRE),
                    f.boss.getBounds());
            f.combat.update(0.1);
        }
        assertEquals(guarded, f.boss.getHealth().getCurrentHp(), "Immunity must negate every hit");
        assertEquals(ElementType.NONE, f.combat.getElementStatus(f.boss.getCombatant()).element(),
                "A negated hit must not attach an element either");
        f.combat.update(BossTraits.BLINK_INVULNERABILITY);
        assertFalse(status.isInvulnerable());
        f.combat.tryAttack(f.target, new AttackSpec("immunity-test", 50, 0.1, 0, ElementType.FIRE),
                f.boss.getBounds());
        f.combat.update(0);
        assertTrue(f.boss.getHealth().getCurrentHp() < guarded, "Damage must resume once immunity ends");
    }

    /** Spread-out damage below the window threshold never accumulates into an escape. */
    @Test
    void windIgnoresDamageSpreadBeyondItsWindow() {
        Fixture f = fixture(BossChallenge.WIND);
        CollisionBox start = f.boss.getBounds();
        for (int i = 0; i < 6; i++) {
            f.boss.getHealth().takeDamage(BossTraits.BLINK_DAMAGE - 5);
            f.boss.update(f.target, BossTraits.BLINK_WINDOW + 0.1);
        }
        assertEquals(start, f.boss.getBounds());
        assertTrue(f.boss.isAlive());
    }

    /**
     * Death and detachment stop every trait effect, leaving no pulses behind.
     * @param guardian campaign target under test
     */
    @ParameterizedTest
    @EnumSource(BossChallenge.class)
    void deathAndDetachmentStopTraitEffects(BossChallenge guardian) {
        Fixture dead = fixture(guardian);
        land(dead);
        int survived = dead.victim.getHealth().getCurrentHp();
        dead.boss.getHealth().takeDamage(dead.boss.getHealth().getMaxHp());
        for (int i = 0; i < 10; i++) {
            dead.boss.update(dead.target, BossTraits.RETRIBUTION_DELAY);
            dead.combat.update(0);
        }
        assertEquals(survived, dead.victim.getHealth().getCurrentHp());
        Fixture detached = fixture(guardian);
        detached.combat.clear();
        for (int i = 0; i < 10; i++) {
            detached.boss.update(detached.target, BossTraits.RETRIBUTION_DELAY);
        }
        assertTrue(detached.combat.getActiveAttacks().isEmpty());
        assertEquals(1000, detached.victim.getHealth().getCurrentHp());
    }

    /** A frozen guardian keeps delivering burning but may not start its own punishing actions. */
    @Test
    void controlSuppressionStopsAgencyButNotDeliveredAfflictions() {
        Fixture burning = fixture(BossChallenge.FIRE);
        land(burning);
        burning.target.setHurtbox(SAFE_SPOT);
        burning.boss.getCombatant().getStatus().applyFreeze(BossTraits.BURN_INTERVAL * 2);
        int before = burning.victim.getHealth().getCurrentHp();
        burning.boss.update(burning.target, BossTraits.BURN_INTERVAL);
        burning.combat.update(0);
        assertEquals(before - BossTraits.BURN_DAMAGE_PER_STACK, burning.victim.getHealth().getCurrentHp(),
                "An affliction already applied keeps running while its owner is frozen");
        Fixture storm = fixture(BossChallenge.LIGHTNING);
        storm.target.setHurtbox(SAFE_SPOT);
        storm.boss.getCombatant().getStatus().applyFreeze(BossTraits.RETRIBUTION_DELAY * 2);
        assertFalse(storm.boss.canAct());
        storm.boss.update(storm.target, BossTraits.RETRIBUTION_DELAY);
        storm.combat.update(0);
        assertEquals(1000, storm.victim.getHealth().getCurrentHp(), "A frozen guardian cannot punish");
    }

    /**
     * Lands exactly one melee pattern on the fixture target, then lets the trait observe it.
     * @param f fixture whose target starts inside melee reach
     */
    private void land(Fixture f) {
        f.target.setHurtbox(new CollisionBox(290, 220, 20, 20));
        f.boss.update(f.target, 0);
        f.boss.update(f.target, BossConfig.MELEE_WINDUP);
        f.combat.update(0);
        f.boss.update(f.target, 0);
    }

    /**
     * Creates a real campaign arena, Boss and registered target with no graphics.
     * @param guardian campaign element whose arena selects the trait
     * @return isolated fixture sharing one combat scope
     */
    private Fixture fixture(BossChallenge guardian) {
        GameMap map = new GameMap(guardian.mapId(), "Trait test", MapType.BATTLE, 1000, 800,
                List.of(), Map.of("entry", new SpawnPoint(0, 0)), "entry");
        CombatSystem combat = new CombatSystem();
        Boss boss = new Boss(guardian.id(), guardian.displayName(), new CollisionBox(200, 200, 64, 64),
                map, GameConfig.SENTINEL, combat);
        Enemy victim = new Enemy(1000);
        Combatant target = new Combatant(victim, Faction.PLAYER, new CollisionBox(290, 220, 20, 20));
        combat.register(target);
        return new Fixture(map, combat, boss, victim, target);
    }

    /**
     * Owns one isolated combat scope.
     * @param map campaign arena selecting the trait
     * @param combat shared settlement engine
     * @param boss tested encounter
     * @param victim health-owning target
     * @param target registered adapter
     */
    private record Fixture(GameMap map, CombatSystem combat, Boss boss, Enemy victim, Combatant target) { }
}
