# Element reaction completion report

Implemented 2026-09-11 by Daoyan Zhu (u7782042). This replaces the two production prototype
rules with the supplied first-version reaction behaviours. Numbers remain provisional balance.
No Git/GitLab writes, formal assets, renderer/VFX work, Quest/Inventory implementation or second
damage engine were introduced.

## 1. Final ordered reaction table

The left side is the existing attachment; the right side is this confirmed hit's element.

| Attached → triggering | Reaction | Current result |
| --- | --- | --- |
| FIRE → WATER | Vaporize | Triggering base damage × 2 |
| WATER → FIRE | Vaporize | Triggering base damage × 1.5 |
| ICE → FIRE | Melt | Triggering base damage × 2 |
| FIRE → ICE | Melt | Triggering base damage × 1.5 |
| FIRE ↔ LIGHTNING | Overload | FIRE area damage, strong poise loss and collision-resolved knockback |
| WATER ↔ LIGHTNING | Electro-Charged | One finite target-local LIGHTNING tick schedule, no chains |
| WATER ↔ ICE | Freeze | Short freeze: no movement or new Boss attacks |
| ICE ↔ LIGHTNING | Superconduct | ICE area damage and finite physical resistance reduction |
| FIRE → WIND | Swirl Fire | Fixed FIRE ground field |
| WATER → WIND | Swirl Water | Fixed WATER ground field with actual damage |
| ICE → WIND | Swirl Ice | Fixed ICE ground field |
| LIGHTNING → WIND | Swirl Lightning | Fixed LIGHTNING ground field |

The four symmetric behaviours have two explicit ordered entries each. Swirl is specifically
WIND triggering an existing other element, as requested; a WIND attachment followed by another
element does not swirl and uses the ordinary replacement policy.

## 2. Attachment and confirmed-hit boundary

ElementRules.Reaction now records attachedElement and triggeringElement, plus multiplier,
optional bonus/child data and a small Kind enum. Duplicate ordered pairs are rejected; reverse
entries are legal and may share the same reaction label. Labels are diagnostic only, not dispatch
keys. ElementRules.DEFAULT is the production table; CombatSystem(customRules) remains supported.

A confirmed hit first passes geometry, hostility, alive/self/repeat filtering, wall occlusion
and generic defensive interception. Only then does the existing CombatSystem damage boundary
look up the ordered rule, calculate damage, call Damageable and record the hit.

- No existing attachment: attach the incoming non-NONE element.
- Same element: refresh its duration.
- Different compatible element: react and consume the attachment; do not leave the trigger attached.
- Different incompatible element: replace with the incoming element.
- NONE: preserve without refreshing; no reaction.
- Derived hits: neither attach, refresh nor react.
- Missed, blocked, friendly, dead or duplicate hits do not create reactions.
- Existing valid zero-damage direct elemental hits can still attach/react, preserving the prior contract.

ElementStatus remains the immutable attachment observation owned by CombatSystem. Its existing
end-of-step aging semantics are retained. New reaction timers begin advancing on the next
combat update, rather than losing their entire lifetime in the creation step.

## 3. ElementField and bounded periodic damage

ElementField.Definition is immutable and validates element, damage, square half-extent, duration
and tick interval. Each runtime field captures its fixed world geometry, original canonical
source and direct parent Attack ID. It retains no Boss reference and cannot follow Boss movement.

ReactionTimer is shared by fields and charged status:

- First tick after one interval, not immediately or on each frame.
- Explicit finite count, at most 64 scheduled ticks per configured instance.
- Catch-up emits only newly due bounded ticks.
- Repeated zero-time updates cannot create damage.
- Final boundary tick is allowed; the field expires at its configured duration.
- Field pulses sample once through ordinary CombatSystem hit detection/occlusion and are retired
  in the same update, including a very short final frame. No post-expiry hitbox remains.

Electro-Charged uses one schedule on the afflicted CombatStatus. It tracks that receiver, but
settles only that receiver through the same damage delivery method; nearby actors do not receive
chain damage. Reapplication refreshes/replaces the schedule rather than stacking timer objects.

One-shot Overload/Superconduct children remain queued for the next combat update. Field pulses
enter the ordinary Attack list with explicit derived provenance. Charged pulses also use an
Attack with direct provenance and the shared canHit/deliverDamage methods, then retire immediately.
All reaction output is ELEMENTAL and non-blockable, and cannot create further reaction generations.

Existing capacity remains 64 live derived attacks. Fields have a separate 64-instance bound.
Capacity-rejected scheduled ticks are consumed, not retried. Invalid optional area geometry
does not undo already settled triggering damage or retry the consumed reaction.

## 4. Damage category and physical resistance

AttackSpec adds DamageType.PHYSICAL / ELEMENTAL independently of its element metadata.
Older constructor signatures remain available: NONE infers PHYSICAL, a non-NONE tag infers
ELEMENTAL. The explicit constructor permits independent metadata and rejects null category.

Actual profession definitions/execution explicitly preserve the intended category:

- Archer J/K arrows and O rain: PHYSICAL.
- Katana J/K/O slash/counter/thrust/follow-up damage: PHYSICAL.
- Mage J, independently randomized rapid bolts and cast-element-locked circle: ELEMENTAL.
- All reaction children, charged ticks and fields: ELEMENTAL, including custom untagged children.

ClassDefinition.withElement preserves category and existing geometry/timing data. The pre-existing
F4 debug tag route is retained, not expanded into a weapon-enchantment system.

Superconduct uses baseline physical resistance 0, temporarily reduced by 0.25:

physical damage multiplier = 1 - (-0.25) = 1.25

The single calculator applies reaction amplification and then applicable physical vulnerability,
floors once to integer HP damage and saturates at Integer.MAX_VALUE. Elemental damage does not
receive this physical bonus. Expiry restores the multiplier to 1; no second armour/damage engine.

## 5. Generic status, Boss and knockback

Combatant composes a small CombatStatus, separate from Health and attachment snapshots:

- finite frozen and stagger timers;
- finite physical vulnerability;
- configurable current/max poise;
- one charged schedule with source/direct provenance;
- pending displacement components.

Overload consumes poise. A break briefly staggers and refills poise to the configured capacity.
This is a simple threshold/reset model, not complex limb/animation/poise regeneration mechanics.
A second pending knockback replaces the pending vector rather than accumulating unsafe distance.

Damage settlement only latches status/displacement. EnemyAI consumes displacement on its next
update using the existing WalkableArea.move, synchronizes its canonical hurtbox and invalidates
cached navigation. Thus the actual Boss cannot be moved into a wall or outside the map.
This also works while the target is absent; movement is not performed in a damage callback.

EnemyAI pauses movement/new decisions while status blocks actions. Boss reuses that common
capability and exposes FROZEN/STAGGERED in BossState. No Boss/Archer/Mage/Katana class checks
were added to CombatSystem. Freeze does not delete already-active attacks. Existing phase-change
and death attack cancellation still take precedence where applicable.

Control state works for EnemyAI-owned actors, including Boss. Other future movement owners must
consume the generic control/displacement capability rather than assuming Health moves entities.
No new player freeze/knockback controller was added outside the requested Boss scope.

## 6. Lifecycle and cleanup

- Death/getStatus/normal combat cleanup clears the dead receiver's reaction status.
- Unregister clears that receiver's status and source-owned attacks/fields/charged schedules.
- Source death/cancelAttacks/unregister clears its continuing fields and charged damage.
- Source cancellation does not erase other targets' already-applied freeze/vulnerability; those
  expire independently, consistent with ordinary attachment cancellation semantics.
- A living source's fixed ground field may remain after the original target dies or detaches:
  it is a location effect, not a target-following status.
- Existing encounter/map clear cancels all fields, pending hits, attachments, controls, charged
  timers and registrations. Retained field references report inactive.
- SUCCESS and FAILURE quest return paths were both tested with actual fields/charged state.
  No map, Quest, reward or Player instance management was duplicated.

## 7. Files and tests

New production files, under core/src/main/java/au/edu/anu/comp2120/combat:

- DamageType.java — explicit calculation category.
- ReactionTuning.java — centralized provisional balance and bounds.
- ReactionTimer.java — finite periodic schedule.
- ElementField.java — immutable definition and fixed-position runtime.
- CombatStatus.java — small control/vulnerability/poise/charged capability.

Modified production files:

- combat/ElementRules.java, AttackSpec.java, Combatant.java, CombatSystem.java
- enemy/EnemyAI.java, Boss.java, BossState.java
- player/PlayerClasses.java, ClassDefinition.java, PlayerAbilities.java, ClassSkills.java

Player, Health, ElementType, ElementStatus and Attack's existing provenance/hit-ledger machinery
were reused without replacement.

Tests:

- New combat/ReactionCompletionTest.java: 39 cases across ordered multipliers, attachment policy,
  Overload poise/knockback/wall clamps, charged tick bounds/refresh/expiry, actual Boss freeze and
  recovery, physical categories, actual class J and stateful K/O vulnerability, four swirl fields,
  fixed positions, wall/tangent checks, lifecycle cleanup, timer/capacity/invalid data and tiny
  final-frame expiry.
- Updated combat/ElementRulesTest.java for the formal ordered matrix.
- combat/ElementCombatTest.java retains the existing generic custom-rule regression contracts
  using an explicit test-only bonus/child table, not a hidden production prototype fallback.
- Updated combat/CombatCapabilitiesTest.java, CombatOcclusionTest.java and
  player/ElementClassIntegrationTest.java for formal multiplier/area behaviour.
- Updated screen/GameplayIntegrationTest.java, PlayerDefeatTest.java and QuestPortalFlowTest.java
  for formal reaction children and real SUCCESS/FAILURE field/status cleanup.
- No test was deleted or disabled. Existing final-class skill, seeded RNG, map, combat, projectile,
  wall tie, health and gameplay tests remain in the full run.

Documentation: README.md, docs/ARCHITECTURE.md and this report.

Final clean validation: **814 tests in 43 suites; zero failures, errors or skipped tests**.
This is 39 additional cases over the initial 775-test baseline.

Commands actually executed included:

```bash
./gradlew compileJava javadocAudit
./gradlew test --tests '*ReactionCompletionTest' javadocAudit
./gradlew test javadocAudit
./gradlew clean test build javadocAudit
./gradlew lwjgl3:run
jcmd -l
jcmd 84824 Thread.print
```

Early diagnostic runs exposed old prototype assertions and one test fixture's still-active priming
hitbox; these were corrected before the successful final clean run. Test counts were read from
Gradle TEST-*.xml using Ruby/REXML. Compilation, packaging and strict JavaDoc audit passed.

Desktop smoke check: launcher remained running and its main thread entered
Lwjgl3Application.loop with no startup exception. The task-launched process was then stopped
with Ctrl-C. This is not a claim of a full manual keyboard playthrough; fresh production task
battles still require the external Quest adapter's encounter activation.

JavaDoc audit includes production/test sources and private members with Xdoclint:all and Werror.
New/modified elements carry descriptions and applicable parameter/return/exception documentation.
The author is the user-provided Daoyan Zhu (u7782042). IntelliJ style inspections are separate.

## 8. Provisional balance and limitations

| Setting | Current value |
| --- | --- |
| Attachment | 3 seconds |
| Overload | 20 FIRE area damage; square radius 80; 100 poise damage; knockback 60 world units |
| Default poise / stagger | 100 capacity; refill on break; 0.5-second action lock |
| Charged | 8 LIGHTNING damage every 0.5 seconds for 2 seconds: 4 ticks |
| Freeze | 0.8 seconds, including Boss |
| Superconduct | 12 ICE area damage; square radius 70; physical resistance -0.25 for 4 seconds |
| Each swirl field | 6 corresponding-element damage every 0.5 seconds for 3 seconds: 6 ticks; square radius 80 |
| One-shot child | 0.05-second hitbox, first sampled next update |
| Bounds | 64 live derived attacks; 64 fields; at most 64 ticks per configured periodic instance |

Behaviour is agreed; these balance values remain adjustable in ReactionTuning and ElementRules.
configurePoise permits a per-combatant capacity without any Boss class check.

Remaining limitations:

- Fixed fields/area attacks use squares with centre-based wall occlusion, not precise circles or
  partial-cover sampling. Charged damage originates at the afflicted target, so it does not need
  continuous line of sight back to the original caster.
- Long-frame catch-up samples current geometry, not historical Boss movement. Work stays bounded;
  normal production already clamps frame delta.
- Freeze/stagger/reapplication can refresh status through new valid direct reactions; no diminishing
  returns or CC-immunity economy was requested. A single application always expires.
- Knockback is a queued instantaneous collision-resolved displacement, not a simulated physical force.
  There is no smooth knockback animation or complex poise recovery.
- Multiple separately triggered fields may coexist up to the cap. Rejected capacity slots are lost;
  fairness/stacking balance across hypothetical crowds is outside the single-Boss gameplay scope.
- No dedicated persistent field/status visual was added. Model snapshots expose the data; Boss
  FROZEN/STAGGERED is available in existing debug state presentation. No VFX/UI/assets/audio.
- No chain lightning, weapon enchantment, resistance roster, save/load or other members' modules.

## 9. Suggested handoff

Commit: feat(combat): complete ordered elemental reactions and bounded status effects

MR title: Complete first-version elemental reactions, ground fields and Boss status effects
