# First-version class skills integration report

> Current integration note (2026-09-25): Save/load now persists class, per-class O modes, Mage element and cooldowns. Production F5 is consumed by SaveMenu for manual saving; choose O mode through Class Master. The F5 mode-selection instructions and no-persistence boundary below describe the original skills integration.
> See [current controls](../README.md), [inventory/equipment](INVENTORY_EQUIPMENT_REPORT.md)
> and [save/load](SAVE_LOAD_REPORT.md) for the current production behavior.

Implemented 2026-09-11 by Daoyan Zhu (u7782042). Behaviour follows the supplied first-version
specification; numerical tuning is provisional. No Git/GitLab operation or formal asset integration.

## Delivered behaviour

| Class | J | K | O A | O B |
| --- | --- | --- | --- | --- |
| Archer | One straight non-piercing arrow | Atomic five-arrow fan; each arrow settles independently | Fixed cast-time Boss-position rain with six waves | Temporary untargetability, not damage immunity |
| Mage | One selected-element bolt | FIRE → WATER → LIGHTNING → ICE → WIND → FIRE; no attack | Fixed cast position/element; interruptible preparation then six field waves | Faster J with two independently randomized elemental bolts |
| Katana | Facing-based single-hit slash | Parry one blockable hit, then collision-safe counter dash; missed parry recovers | One blockable-hit counter stance | Collision-safe thrust; accepted target hit unlocks finite follow-up |

L remains the existing shared collision-safe Dash for every class. Projectiles retain swept
world collision and wall-wins-equal-time ties. All fields, counters and slashes use shared
static-world occlusion and the ordinary damage/element/Health pipeline.

Mage preparation locks movement and J/K/L/O. Effective HP loss interrupts it with no cooldown
refund, including loss followed by healing within the same step. Zero damage does not interrupt.
The field is created only after successful preparation, beginning on the next simulation update.
Mage K works in battle, but rejects dead/unavailable/casting state and an active J launch window.

Untargetability is an AI selection rule on Combatant, not a damage gate. Existing attacks,
ground areas and direct environmental HP loss can still hurt the Player. Boss reuses EnemyAI's
generic eligibility check; there is no Archer test inside Boss or combat.

## Shared mechanisms and frame order

- ClassSkills composes bounded state/timers inside PlayerAbilities. SkillKind and immutable
  ClassDefinition/AbilityLoadout data choose execution without copying Player or projectile code.
- Combatant exposes generic targetability and optional HitInterceptor. AttackSpec.blockable
  defaults to true through existing constructors; scheduled ground/counter/reaction waves use false.
- CombatSystem invokes interception only after valid geometry, hostility and wall checks.
  A block consumes that invocation's ordinary hit ledger, but delivers no damage or attachment.
- Interceptors only latch the triggering source. They never move, change maps or submit attacks
  during settlement. A second incoming hit in the same frame is not automatically blocked.
- Attack.hasLandedHit distinguishes accepted contact from blocked contact for thrust follow-up.
- Health.getDamageVersion records effective positive HP loss without Mage-specific branches.
- The mixed-specification tryProjectiles overload validates and creates the entire volley atomically.
  Archer shares it with Mage; capacity stays 256 projectiles, at most nine per volley.
- The Random-taking PlayerAbilities constructor enables fixed-seed tests. Each rapid bolt consumes
  a separate draw from the five-element pool; equal results are explicitly permitted and tested.

The coordinator calls, on one game-loop thread:

1. PlayerAbilities.update: input, collision-safe movement and scheduled attack submissions.
2. CombatSystem.update: all actors' ordinary hit/damage/element settlement.
3. PlayerAbilities.afterCombat: damage interruption, confirmed thrust and deferred counter decisions.
4. Existing encounter outcome coordination: success/failure, cleanup and Hub return.

A stance response submitted after settlement hits on the next combat update. Calling afterCombat
twice without another ability update is harmless. CombatSystem has no PlayerClass/Archer/Mage/Katana
branches and no second damage formula was introduced.

## Persistent choices and cleanup

Player owns an EnumMap from PlayerClass to UltimateMode, initially A for each class, plus Mage's
selected element (initially FIRE). ClassDefinition supplies the default and alternate O definitions.

PlayerAbilities.selectUltimateMode(mode) returns false outside a living, available Hub context,
or during active skill/dash/launch/attack activity. Null is rejected explicitly. It requires Hub
even in unrestricted test/demo controllers. PlayerInput samples F5 on a key edge; GameScreen and
CombatDemo route it through this API. F1/F2/F3 class selection remains Hub-only in production.

Class switching remembers each class's choice separately. Neither selection nor travel refreshes
slot cooldowns. Existing defeat/success paths retain the same Player, class and O mode. Scope
rebinding clears casts, fields, buffs, parry/recovery, dash/thrust/follow-up activity, captured
targets and defensive callbacks; restores targetability; and uses existing combat cleanup for
attacks, projectiles and elemental/derived state. No Quest or Inventory state is modified here.

## Provisional tuning

Damage/cooldown/range definitions live in PlayerClasses; additional behaviour constants live in
SkillTuning. Numbers below are base damage before existing elemental reactions.

| Skill | Current values |
| --- | --- |
| Archer J | 20 damage; 0.45 s cooldown; 0.15 s launch; speed 600; range 480; size 6; lifetime 1.2 s |
| Archer K | 5 arrows, 40-degree total fan; 20 each; 2.5 s cooldown; 0.25 s launch; speed 800; range 640; size 8; lifetime 1.2 s |
| Archer O A | 6 × 18 damage; 0.35 s spacing; 2.1 s field lifetime; target hurtbox expanded 100; 8 s cooldown |
| Archer O B | Untargetable 3 s; 8 s cooldown |
| Mage J | 25 damage; 0.5 s cooldown; 0.25 s launch; speed 400; range 360; size 10; lifetime 1.5 s |
| Mage K | No damage; 0.05 s selection cooldown |
| Mage O A | 1.5 s cast; 6 × 25 damage; 0.4 s spacing; 2.4 s field lifetime; target hurtbox expanded 160; 10 s cooldown |
| Mage O B | Buff 4 s; J cooldown 0.16 s and launch 0.08 s; 2 bolts with 6-degree separation; unchanged J damage/flight; 10 s O cooldown |
| Katana J | 30 damage; reach 48; 0.15 s active; 0.35 s cooldown |
| Katana K | Parry 0.25 s; missed recovery 0.8 s; cooldown 2 s; counter speed 700 for 0.36 s; at most 4 × 12 damage; contact margin 12 |
| Katana O A | Stance 1.2 s; one 100-damage counter; player hurtbox expanded 140; 7 s cooldown |
| Katana O B | Thrust speed 600 for at most 0.4 s; initial hit 10; then at most 6 × 18 every 0.12 s; captured target hurtbox expanded 80; 7 s cooldown |
| Shared wave hitbox | 0.05 s active per invocation; one hit per target per invocation |
| Archer / Mage / Katana L | Existing speed-duration-cooldown: 650–0.2–1.2 / 600–0.2–1 / 750–0.16–0.9 |

## Changed files

Paths below are relative to core/src/main/java/au/edu/anu/comp2120 unless stated otherwise.

New production files:

- combat/HitInterceptor.java
- player/UltimateMode.java
- player/SkillKind.java
- player/SkillTuning.java
- player/ClassSkills.java

Modified production files:

- combat/Attack.java, AttackSpec.java, Combatant.java, CombatSystem.java, Health.java
- enemy/EnemyAI.java
- player/AbilityLoadout.java, ClassDefinition.java, Player.java, PlayerAbilities.java,
  PlayerClasses.java, PlayerInput.java
- screen/CombatDemo.java
- GameScreen.java

New tests (under core/src/test/java/au/edu/anu/comp2120):

- player/FinalClassSkillsTest.java: 18 cases covering final skills, timing, walls, RNG and modes.
- combat/CombatCapabilitiesTest.java: 5 cases covering generic interception, damage stamps,
  targetability/cleanup and atomic mixed-element delivery.

Updated tests:

- screen/QuestPortalFlowTest.java: 12 additional class × mode × outcome cases through real
  task admission, success/failure return and retry; Battle F5 cannot change mode.
- player/ArcherAbilitiesTest.java, MageAbilitiesTest.java, KatanaAbilitiesTest.java,
  PlayerClassTest.java, PlayerInputTest.java
- combat/CombatOcclusionTest.java
- screen/CombatDemoTest.java, PlayerDefeatTest.java, BossDemoTest.java, GameplayIntegrationTest.java

Old provisional expectations were updated for the specified behaviours. Historical direct-area
geometry regressions use explicit custom DIRECT loadouts; they do not mislabel Mage K or Katana
defence as immediate attacks. No tests were deleted or disabled to achieve passing results.

Documentation: README.md, docs/ARCHITECTURE.md, this report, and a current-mode note in
docs/QUEST_PORTAL_FLOW_REPORT.md. The latter's illustrative API fragments are marked text rather
than an invalid standalone Java compilation unit, addressing the earlier Markdown inspection issue.

## Actual validation

Final clean run: **775 tests across 42 suites; zero failures, errors or skipped tests**.
Compilation, packaging and strict JavaDoc audit passed. Net addition: 35 cases over the initial
740-test baseline; pre-existing regression cases also run with the new definitions.

Commands executed during implementation included:

```bash
./gradlew test --tests '*FinalClassSkillsTest' javadocAudit
./gradlew test --tests '*ArcherAbilitiesTest' --tests '*MageAbilitiesTest' --tests '*KatanaAbilitiesTest'
./gradlew test javadocAudit
./gradlew test build javadocAudit
./gradlew clean test build javadocAudit
./gradlew lwjgl3:run
jcmd -l
jcmd 82829 Thread.print
```

Additional targeted class/combat/screen test runs were used while correcting old provisional
expectations. Test totals were read from Gradle's TEST-*.xml with Ruby/REXML, not inferred from
the number of test methods. Earlier diagnostic failures were resolved before the final clean run.

Desktop startup smoke check: the launcher ran without startup exceptions and its main thread
entered Lwjgl3Application.loop. The launched process was then stopped with Ctrl-C. This checks
startup/main-loop health, not a manual keyboard playthrough of every skill.

JavaDoc audit includes production and test Java, private members, Xdoclint:all and Werror.
New/modified Java declarations have descriptions and applicable parameter/return/exception tags;
new authors use the user-provided Daoyan Zhu (u7782042), not an inferred identity. A successful
audit is not a claim that every IntelliJ style inspection is warning-free.

## Manual verification and remaining limits

1. Launch with lwjgl3:run. Use F1/F2/F3 and F5 in Hub; verify class, O mode, Mage element and skill
   phase in the native title. On media-key keyboards use Fn with the F keys if needed.
2. Developer B's existing adapter must activate an encounter before its task portal can admit
   a Boss. Fresh production startup intentionally has no fake acceptance key or Quest UI.
3. Enter the enabled portal. Try F5: mode must remain unchanged. Test J/K/L/O for the chosen
   class, including fixed-position fields, cast interruption and timed defensive windows.
4. Lose or win: return to Hub with the same class/mode and no old active state. Retry a failure
   against a fresh Boss; switch mode only while safely in Hub.

- Only one stateful K/O execution runs at once. Field/buff phases permit normal actions;
  Mage casting and Katana defence/recovery/counter/thrust/follow-up phases lock ordinary inputs.
  This is explicit first-version arbitration, not a full cancellation/combo system.
- Cast targeting automatically chooses the nearest eligible hostile; production has one Boss.
  No cursor aiming, target selector UI or new casting-distance restriction is introduced.
- Fields are rectangles expanded from the captured target hurtbox, not true circles. Counter
  paths use axis-aligned swept envelopes and the existing centre-based wall gate, not blade arcs.
- Finite catch-up waves sample current target geometry; they do not reconstruct historical target
  motion across a long frame. Existing production delta clamping limits ordinary large-frame steps.
- Existing Boss attacks default to blockable. Derived/ground skill waves are explicitly non-blockable;
  future environment attacks should choose their generic blockable flag deliberately.
- Random selection is deterministic for the same seeded input sequence; a capacity-rejected rapid
  attempt may still consume random draws, though it creates no partial volley and spends no cooldown.
- No mana/equipment/balance pass, save/load, NPC/Quest/Inventory implementation, formal UI or art.
  Persistent choices here mean across runtime class/map/encounter transitions, not disk persistence.

## Suggested handoff

Commit: feat(player): implement first-version class skills and Hub-only ultimate modes

MR title: Implement Archer, Mage and Katana skills with persistent Hub-only ultimate modes

