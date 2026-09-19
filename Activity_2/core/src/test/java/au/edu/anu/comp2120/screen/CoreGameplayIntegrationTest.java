package au.edu.anu.comp2120.screen;

import static org.junit.jupiter.api.Assertions.*;

import au.edu.anu.comp2120.ActiveEncounter;
import au.edu.anu.comp2120.BattleResult;
import au.edu.anu.comp2120.EncounterFlow;
import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.TravelSystem;
import au.edu.anu.comp2120.combat.*;
import au.edu.anu.comp2120.enemy.Boss;
import au.edu.anu.comp2120.enemy.BossState;
import au.edu.anu.comp2120.npc.PortalGuide;
import au.edu.anu.comp2120.player.*;
import au.edu.anu.comp2120.world.*;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Production encounter integration across actual travel, class skills, elemental state and outcomes.
 * Uses the same graphics-free coordinators as GameScreen, not the unrestricted demo constructor.
 * These are bounded JUnit fixtures, not a gameplay smoke-tester feature or a Quest implementation.
 * @author Daoyan Zhu (u7782042)
 */
final class CoreGameplayIntegrationTest {
    /** Persistent real player/map session. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    /** Production task routes, initially closed. */
    private final List<Portal> portals = TravelCatalog.createQuestPortals();
    /** Actual travel implementation, including arrival provenance. */
    private final TravelSystem travel = new TravelSystem(session, portals, TravelCatalog.createDestinations());
    /** Existing external permission/result/reward boundary. */
    private final EncounterFlow flow = new EncounterFlow(session, travel, portals.stream().map(Portal::id).toList());
    /** Single real combat engine retained for explicit state arrangement and cleanup assertions. */
    private final CombatSystem combat = new CombatSystem();
    /** Production coordination with exactly the same admission policy as GameScreen. */
    private final CombatDemo demo = new CombatDemo(session, combat, flow);

    /** Creates an independent production fixture for each parameterized case. */
    CoreGameplayIntegrationTest() { }

    /**
     * Built-in practice/battle spawns are reached by Hub movement and create the correct single target.
     * @param map target map ID
     * @param spawn target named spawn
     */
    @ParameterizedTest
    @CsvSource({"battle-training,entry", "battle-training,far-side", "battle-crossroads,entry", "battle-crossroads,east"})
    void admissionByMovementUsesCurrentMapCollisionAndOneTarget(String map, String spawn) {
        assertTrue(portals.stream().noneMatch(p -> travel.getPortal(p.id()).enabled()));
        frame(0.1, new PlayerCommand(1, 0, false, false, false, false));
        assertTrue(demo.getBoss().isEmpty());
        boolean practice = session.getMap(map).getType() == MapType.TRAINING;
        if (practice) {
            new PortalGuide(session, travel, flow).select(PortalGuide.Destination.TRAINING);
            travel.configurePortalDestination(TravelCatalog.QUEST_PORTAL_ID, map, spawn);
            travel.setPortalEnabled(TravelCatalog.QUEST_PORTAL_ID, true);
        } else {
            flow.activate(new ActiveEncounter("walk-in", map, spawn, TravelCatalog.QUEST_PORTAL_ID));
        }
        for (int i = 0; i < 10 && session.getCurrentMap().getType() == MapType.HUB; i++) {
            frame(0.1, new PlayerCommand(1, 0, false, false, false, false));
        }
        assertEquals(map, session.getCurrentMap().getId());
        SpawnPoint entry = session.getCurrentMap().getSpawnPoint(spawn);
        assertEquals(new CollisionBox(entry.x(), entry.y(), 28, 28), session.getPlayer().getBounds());
        assertSame(session.getCurrentMap(), session.getPlayer().getCollisionArea());
        if (practice) {
            assertTrue(demo.getBoss().isEmpty());
            assertTrue(demo.getTrainingDummy().isPresent());
            assertEquals(2, demo.getCombatants().size());
        } else {
            assertSingleBoss();
        }
        for (int i = 0; i < 20; i++) { frame(0, PlayerCommand.NONE); }
        assertEquals(map, session.getCurrentMap().getId(), "Arrival protection must not return immediately");
        assertEquals(!practice, flow.isBattleActive());
        assertCameraAndWorld();
    }

    /**
     * Every class/remembered mode can complete a real admitted fight with normal ability commands.
     * Only starting position is arranged; neither actor's HP is changed by the fixture.
     * @param type chosen class
     * @param mode selected ultimate
     */
    @ParameterizedTest
    @CsvSource({"ARCHER,A", "ARCHER,B", "MAGE,A", "MAGE,B", "KATANA,A", "KATANA,B"})
    void actualCombatCompletesOneRewardAndClosesPermission(PlayerClass type, UltimateMode mode) {
        select(type, mode);
        admit("win");
        var persistent = session.getPlayer();
        Boss oldBoss = boss();
        session.getPlayer().enterArea(session.getCurrentMap(), type == PlayerClass.MAGE ? 80 : 320, 100);
        for (int i = 0; i < 300 && session.getCurrentMap().getType() == MapType.BATTLE; i++) {
            CollisionBox body = persistent.getBounds();
            CollisionBox target = oldBoss.getBounds();
            double dx = target.x() + target.width() / 2 - body.x() - body.width() / 2;
            double dy = target.y() + target.height() / 2 - body.y() - body.height() / 2;
            boolean special = type == PlayerClass.ARCHER && i % 25 == 0
                    || type == PlayerClass.MAGE && mode == UltimateMode.B && i % 5 == 0;
            // Random-buff execution is covered below and seeded unit tests; this victory script avoids RNG.
            boolean ultimate = type != PlayerClass.MAGE || mode == UltimateMode.A && i == 0;
            frame(0.1, new PlayerCommand(type == PlayerClass.MAGE || Math.abs(dx) < 8 ? 0 : dx > 0 ? 1 : -1,
                    type == PlayerClass.MAGE || Math.abs(dy) < 8 ? 0 : dy > 0 ? 1 : -1,
                    true, special, false, ultimate));
        }
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId(), "Bounded fight must end");
        BattleResult result = flow.getLastResult().orElseThrow();
        assertEquals(BattleResult.Outcome.SUCCESS, result.outcome(), demo.getStatus());
        assertSame(persistent, session.getPlayer());
        assertEquals(type, persistent.getPlayerClass());
        assertEquals(mode, persistent.getUltimateMode());
        assertEquals(100, persistent.getHealth().getCurrentHp());
        assertEquals(BossState.DEAD, oldBoss.getState());
        assertEquals(1, flow.getPendingRewards().size());
        var payload = flow.getPendingRewards().get(result);
        assertNotNull(payload);
        assertEquals("sentinel-core", payload.materialId());
        assertTrue(oldBoss.claimDrop().isEmpty());
        assertEquals(payload, flow.claimReward(result).orElseThrow());
        assertTrue(flow.claimReward(result).isEmpty());
        assertTrue(flow.getPendingRewards().isEmpty());
        assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertTrue(flow.getActiveEncounter().isEmpty());
        long returned = session.getTransitionVersion();
        for (int i = 0; i < 20; i++) { frame(0.1, PlayerCommand.NONE); }
        assertEquals(returned, session.getTransitionVersion());
        assertEquals(result, flow.getLastResult().orElseThrow());
        assertTrue(demo.getBoss().isEmpty());
        assertThrows(IllegalStateException.class,
                () -> flow.activate(new ActiveEncounter("win", MapCatalog.CROSSROADS, "entry", TravelCatalog.QUEST_PORTAL_ID)));
        assertCameraAndWorld();
    }

    /** Real Boss AI damage, without fixture HP changes, ends the challenge and allows a fresh retry. */
    @Test
    void actualBossCombatFailureReturnsAndRetries() {
        select(PlayerClass.MAGE, UltimateMode.B);
        admit("boss-defeat");
        var persistent = session.getPlayer();
        Boss oldBoss = boss();
        for (int i = 0; i < 300 && session.getCurrentMap().getType() == MapType.BATTLE; i++) {
            frame(0.1, PlayerCommand.NONE);
        }
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        BattleResult result = flow.getLastResult().orElseThrow();
        assertEquals(BattleResult.Outcome.FAILURE, result.outcome());
        assertSame(persistent, session.getPlayer());
        assertEquals(100, persistent.getHealth().getCurrentHp());
        assertEquals(PlayerClass.MAGE, persistent.getPlayerClass());
        assertEquals(UltimateMode.B, persistent.getUltimateMode());
        assertTrue(flow.getPendingRewards().isEmpty());
        assertTrue(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        assertEquals(400, oldBoss.getHealth().getCurrentHp());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertTrue(combat.getElementFields().isEmpty());
        enterQuestPortal();
        assertSingleBoss();
        assertNotSame(oldBoss, boss());
        assertEquals(400, boss().getHealth().getCurrentHp());
        assertEquals(1, boss().getPhase());
        assertEquals(BossState.IDLE, boss().getState());
        assertEquals(result.encounter(), flow.getActiveEncounter().orElseThrow());
    }

    /**
     * Crosses active skill phases with outcomes and programmatic travel, not removed debug keys.
     * @return scenario and exit-path combinations
     */
    private static Stream<Arguments> lifecycleCases() {
        return Stream.of("arrow", "volley", "rain", "untargetable", "cast", "magic-field", "rapid",
                        "slash", "parry", "recovery", "counter-dash", "stance", "thrust", "finisher", "dash")
                .flatMap(skill -> Stream.of("failure", "success", "travel").map(exit -> Arguments.of(skill, exit)));
    }

    /**
     * No active weapon/class/reaction object may update or damage the arrival scope.
     * @param skill exact active phase to leave
     * @param exit failure, success or an explicit low-level travel API call
     */
    @ParameterizedTest
    @MethodSource("lifecycleCases")
    void allActivePhasesAreScopeLocal(String skill, String exit) {
        prepare(skill);
        var persistent = session.getPlayer();
        var definition = persistent.getClassDefinition();
        UltimateMode mode = persistent.getUltimateMode();
        ElementType mageElement = persistent.getMageElement();
        Boss oldBoss = boss();
        Combatant oldSource = source();
        // Prime finite controls, periodic damage, one-shot children and a fixed ground field together.
        // WIND consumes any existing FIRE attachment from a completed circle's first wave.
        for (ElementType element : List.of(ElementType.WIND, ElementType.ICE, ElementType.LIGHTNING,
                ElementType.WATER, ElementType.LIGHTNING, ElementType.FIRE, ElementType.WIND,
                ElementType.WATER, ElementType.ICE, ElementType.FIRE, ElementType.LIGHTNING, ElementType.FIRE)) {
            combat.tryAttack(oldSource, new AttackSpec("cleanup-prime", 0, 10, 0, element),
                    oldBoss.getBounds(), oldBoss.getBounds()).orElseThrow();
            combat.update(0);
        }
        assertTrue(oldBoss.getCombatant().getStatus().isFrozen());
        assertTrue(oldBoss.getCombatant().getStatus().getChargedRemaining() > 0);
        assertTrue(oldBoss.getCombatant().getStatus().getResistanceRemaining() > 0);
        assertFalse(combat.getElementFields().isEmpty());
        if (combat.getActiveAttacks().stream().noneMatch(a -> a.getSource() == oldBoss.getCombatant())) {
            combat.tryAttack(oldBoss.getCombatant(), new AttackSpec("old-boss-area", 0, 10, 0),
                    oldSource.getHurtbox(), oldSource.getHurtbox()).orElseThrow();
        }
        List<Attack> oldAttacks = combat.getActiveAttacks();
        var oldFields = combat.getElementFields();
        List<Combatant> oldActors = demo.getCombatants();
        double cooldown = demo.getAbilities().getCooldownRemaining(AbilitySlot.ULTIMATE);
        int hpBeforeTravel = persistent.getHealth().getCurrentHp();
        switch (exit) {
            case "failure" -> persistent.takeDamage(persistent.getHealth().getCurrentHp());
            case "success" -> oldBoss.getHealth().takeDamage(oldBoss.getHealth().getCurrentHp());
            case "travel" -> travel.fastTravel(TravelCatalog.HUB_ENTRY);
            default -> throw new AssertionError(exit);
        }
        // Settled outcomes/transition sync take priority over spam, reset and configuration requests.
        demo.update(0.1, new PlayerCommand(1, 1, true, true, true, true), true,
                PlayerClass.MAGE, true, true);
        assertEquals(MapCatalog.HUB, session.getCurrentMap().getId());
        assertSame(persistent, session.getPlayer());
        assertSame(definition, persistent.getClassDefinition());
        assertEquals(mode, persistent.getUltimateMode());
        assertEquals(mageElement, persistent.getMageElement());
        assertEquals(exit.equals("travel") ? hpBeforeTravel : 100, persistent.getHealth().getCurrentHp());
        assertEquals(new CollisionBox(64, 64, 28, 28), persistent.getBounds());
        assertEquals(cooldown, demo.getAbilities().getCooldownRemaining(AbilitySlot.ULTIMATE));
        assertEquals("NONE", demo.getAbilities().getSkillStatus());
        assertTrue(source().isTargetable());
        assertTrue(oldSource.isTargetable(), "Old untargetable capability must be released");
        assertTrue(oldAttacks.stream().noneMatch(Attack::isActive));
        assertTrue(oldFields.stream().noneMatch(ElementField::isActive));
        assertTrue(oldActors.stream().noneMatch(demo.getCombatants()::contains));
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertTrue(combat.getElementFields().isEmpty());
        assertEquals(0, oldBoss.getCombatant().getStatus().getChargedRemaining());
        assertFalse(oldBoss.getCombatant().getStatus().blocksActions());
        assertEquals(1, oldBoss.getCombatant().getStatus().physicalMultiplier());
        assertThrows(IllegalArgumentException.class, () -> combat.getElementStatus(oldSource));
        assertTrue(oldBoss.claimDrop().isEmpty());
        assertTrue(demo.getBoss().isEmpty());
        assertTrue(demo.getEnemies().isEmpty());
        assertEquals(1, demo.getCombatants().size());
        long version = session.getTransitionVersion();
        CollisionBox abandonedBody = oldBoss.getBounds();
        int arrivalHp = persistent.getHealth().getCurrentHp();
        for (int i = 0; i < 100; i++) {
            frame(0.1, PlayerCommand.NONE);
            oldBoss.update(source(), 0.1);
            oldBoss.synchronizeOutcome();
        }
        assertEquals(version, session.getTransitionVersion());
        assertEquals(abandonedBody, oldBoss.getBounds());
        assertEquals(arrivalHp, persistent.getHealth().getCurrentHp());
        assertTrue(combat.getActiveAttacks().isEmpty());
        assertTrue(combat.getElementFields().isEmpty());
        if (exit.equals("success")) {
            assertEquals(BattleResult.Outcome.SUCCESS, flow.getLastResult().orElseThrow().outcome());
            assertEquals(1, flow.getPendingRewards().size());
            assertFalse(travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).enabled());
        } else {
            assertTrue(flow.getPendingRewards().isEmpty());
            assertTrue(flow.getActiveEncounter().isPresent());
            if (exit.equals("failure")) {
                assertEquals(BattleResult.Outcome.FAILURE, flow.getLastResult().orElseThrow().outcome());
            } else {
                assertTrue(flow.getLastResult().isEmpty(), "Ordinary travel must not invent a task outcome");
            }
            enterQuestPortal();
            assertSingleBoss();
            assertNotSame(oldBoss, boss());
            assertEquals(400, boss().getHealth().getCurrentHp());
            assertEquals(1, boss().getPhase());
            assertEquals(BossState.IDLE, boss().getState());
            assertEquals(0, combat.getCooldownRemaining(boss().getCombatant()));
            assertFalse(boss().getCombatant().getStatus().blocksActions());
            assertEquals(mode, persistent.getUltimateMode());
        }
    }

    /**
     * Initializes a concrete class phase using the actual coordinator and shared pipeline.
     * @param skill named lifecycle fixture
     */
    private void prepare(String skill) {
        PlayerClass type = switch (skill) {
            case "arrow", "volley", "rain", "untargetable" -> PlayerClass.ARCHER;
            case "cast", "magic-field", "rapid" -> PlayerClass.MAGE;
            default -> PlayerClass.KATANA;
        };
        UltimateMode mode = List.of("untargetable", "rapid", "thrust", "finisher").contains(skill)
                ? UltimateMode.B : UltimateMode.A;
        select(type, mode);
        admit("lifecycle");
        PlayerClass other = type == PlayerClass.MAGE ? PlayerClass.ARCHER : PlayerClass.MAGE;
        assertFalse(demo.getAbilities().selectClass(PlayerClasses.get(other)));
        assertFalse(demo.getAbilities().selectUltimateMode(mode == UltimateMode.A ? UltimateMode.B : UltimateMode.A));
        demo.update(0, PlayerCommand.NONE, false, null, false, true);
        assertEquals(mode, session.getPlayer().getUltimateMode());
        if (skill.equals("counter-dash") || skill.equals("finisher")) {
            session.getPlayer().enterArea(session.getCurrentMap(), 320, 100);
        }
        boolean light = skill.equals("arrow") || skill.equals("slash");
        boolean special = List.of("volley", "parry", "recovery", "counter-dash").contains(skill);
        boolean dash = skill.equals("dash");
        demo.update(skill.equals("finisher") ? 0.1 : 0,
                new PlayerCommand(0, 0, light, special, dash, !light && !special && !dash), false);
        switch (skill) {
            case "magic-field" -> {
                for (int i = 0; i < 16; i++) { demo.update(0.1, PlayerCommand.NONE, false); }
            }
            case "recovery" -> demo.update(SkillTuning.PARRY_SECONDS, PlayerCommand.NONE, false);
            case "rapid" -> {
                demo.update(0, new PlayerCommand(0, 0, true, false, false, false), false);
                assertEquals(2, combat.getActiveAttacks().stream().filter(a -> a.getProjectile() != null).count());
            }
            default -> { /* Other phases are ready immediately after the initial command. */ }
        }
        String expected = switch (skill) {
            case "rain", "magic-field" -> "FIELD";
            case "untargetable", "rapid" -> "BUFF";
            case "cast" -> "CAST";
            case "parry" -> "PARRY";
            case "recovery" -> "RECOVERY";
            case "counter-dash" -> "COUNTER_DASH";
            case "stance" -> "STANCE";
            case "thrust" -> "THRUST";
            case "finisher" -> "FINISHER";
            default -> "NONE";
        };
        assertEquals(expected, demo.getAbilities().getSkillStatus(), skill);
        if (skill.equals("arrow") || skill.equals("volley")) {
            assertEquals(skill.equals("arrow") ? 1 : 5,
                    combat.getActiveAttacks().stream().filter(a -> a.getProjectile() != null).count());
        }
        if (dash) { assertEquals(AbilityState.ACTIVE, demo.getAbilities().getState(AbilitySlot.DASH)); }
        if (skill.equals("untargetable")) { assertFalse(source().isTargetable()); }
        assertSingleBoss();
    }

    /**
     * Selects class and O through production Hub guards.
     * @param type profession
     * @param mode remembered mode
     */
    private void select(PlayerClass type, UltimateMode mode) {
        demo.update(0, PlayerCommand.NONE, false, type);
        if (mode == UltimateMode.B) {
            demo.update(0, PlayerCommand.NONE, false, null, false, true);
        }
        assertEquals(type, session.getPlayer().getPlayerClass());
        assertEquals(mode, session.getPlayer().getUltimateMode());
    }

    /**
     * Emulates only external acceptance, then uses the real portal boundary.
     * @param id acceptance identity
     */
    private void admit(String id) {
        flow.activate(new ActiveEncounter(id, MapCatalog.CROSSROADS, "entry", TravelCatalog.QUEST_PORTAL_ID));
        enterQuestPortal();
    }

    /**
     * Enters this fixture's Crossroads task portal after observing departure, without bypassing admission.
     */
    private void enterQuestPortal() {
        travel.updatePortals();
        CollisionBox trigger = travel.getPortal(TravelCatalog.QUEST_PORTAL_ID).trigger();
        session.getPlayer().enterArea(session.getCurrentMap(), trigger.x(), trigger.y());
        assertTrue(travel.updatePortals());
        demo.update(0, PlayerCommand.NONE, false);
    }

    /**
     * Uses the screen's coordinator-before-portal order without a graphics context.
     * @param delta simulation seconds
     * @param command sampled player request
     */
    private void frame(double delta, PlayerCommand command) {
        long version = session.getTransitionVersion();
        demo.update(delta, command, false);
        if (version != session.getTransitionVersion()) { return; }
        if (travel.updatePortals()) { demo.update(0, PlayerCommand.NONE, false); }
    }

    /**
     * Exposes the admitted Boss to assertions.
     * @return unique current encounter
     */
    private Boss boss() { return demo.getBoss().orElseThrow(); }

    /**
     * Gets the canonical player adapter, not a fabricated alias.
     * @return registered player adapter
     */
    private Combatant source() {
        return demo.getCombatants().stream().filter(a -> a.getEntity() == session.getPlayer()).findFirst().orElseThrow();
    }

    /** Checks exact production membership, not merely a nonempty enemy collection. */
    private void assertSingleBoss() {
        assertEquals(2, demo.getCombatants().size());
        assertTrue(demo.getEnemies().isEmpty());
        assertEquals(List.of(source(), boss().getCombatant()), demo.getCombatants());
    }

    /** Validates current-map player collision and the renderer's stateless camera calculation. */
    private void assertCameraAndWorld() {
        CollisionBox map = session.getCurrentMap().getBounds();
        CollisionBox view = CameraFollow.viewFor(session.getPlayer().getBounds(), map,
                GameConfig.CAMERA_VIEW_WIDTH, GameConfig.CAMERA_VIEW_HEIGHT);
        assertTrue(view.x() >= map.x() && view.y() >= map.y());
        assertTrue(view.right() <= map.right() && view.top() <= map.top());
        assertTrue(session.getCurrentMap().isWalkable(session.getPlayer().getBounds()));
    }
}
