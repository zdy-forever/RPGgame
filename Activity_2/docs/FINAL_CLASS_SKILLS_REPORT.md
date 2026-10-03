# Class skills

Reviewed against `7fea86a` on 2026-10-02. Core class-skill implementation: Daoyan Zhu
(`u7782042`). Sources: [PlayerClasses](../core/src/main/java/au/edu/anu/comp2120/player/PlayerClasses.java),
[SkillTuning](../core/src/main/java/au/edu/anu/comp2120/player/SkillTuning.java),
[ClassSkills](../core/src/main/java/au/edu/anu/comp2120/player/ClassSkills.java).

## Selection and controls

The initial class is Mage. Class Master in Hub selects Archer/Mage/Katana and O mode A/B.
Each class remembers its own ultimate choice, initially A; Mage independently remembers its
element, initially Fire. F1/F2/F3 provide Hub class shortcuts. F5 belongs to manual saving;
it is not the production O-mode control.

Class/loadout selection requires an available living Hub player without active skill, dash,
launch or attack activity. It retains HP, position and remaining cooldowns. Choices, Mage
element and cooldowns survive travel and are included in safe-map saves.

| Class | J | K | O A | O B |
| --- | --- | --- | --- | --- |
| Archer | One straight non-piercing arrow | Atomic five-arrow fan; each arrow hits independently | Fixed target-position Arrow Rain, six waves | Temporary AI untargetability |
| Mage | One selected-element bolt | Fire → Water → Lightning → Ice → Wind → Fire; no attack | Interruptible preparation, then a fixed six-wave Magic Circle | Faster J with two independently randomized elemental bolts |
| Katana | Facing single-hit slash | Parry one blockable hit, then collision-safe counter dash | One blockable-hit Counter Stance | Collision-safe Dragon Thrust; confirmed target hit unlocks finite follow-up |

L is each class's collision-safe dash. Abilities trigger once per press, not repeatedly while
held. Dash has input priority, followed by a ready O/K/J; requests are not buffered. P uses the
shared potion rules without changing class, equipment or cooldowns.

## Current base tuning

Damage is before equipment, reactions and vulnerability. Numerical tuning is adjustable in
the source catalogues; values are not a claim of final balance.

| Skill | Damage / timing |
| --- | --- |
| Archer J | 20; cooldown 0.45 s; launch 0.15 s; projectile speed 600, range 480 |
| Archer K | Five × 20; 40° total fan; cooldown 2.5 s; launch 0.25 s; speed 800, range 640 |
| Archer O A | Six × 18 at 0.35 s intervals; target hurtbox expanded by 100; cooldown 8 s |
| Archer O B | Untargetable for 3 s; cooldown 8 s |
| Mage J | 25; cooldown 0.5 s; launch 0.25 s; speed 400, range 360 |
| Mage K | No damage; selection cooldown 0.05 s |
| Mage O A | Preparation 1.5 s, six × 25 at 0.4 s intervals; target hurtbox expanded by 160; cooldown 10 s |
| Mage O B | Buff 4 s; J cooldown 0.16 s/launch 0.08 s; two bolts separated by 6°; O cooldown 10 s |
| Katana J | 30; reach 48; active 0.15 s; cooldown 0.35 s |
| Katana K | Parry 0.25 s; missed recovery 0.8 s; counter speed 700 for 0.36 s, at most four × 12; cooldown 2 s |
| Katana O A | Stance 1.2 s; one 100-damage response; area expands player hurtbox by 140; cooldown 7 s |
| Katana O B | Thrust speed 600 for at most 0.4 s; initial hit 10; at most six × 18 every 0.12 s after confirmed hit; cooldown 7 s |
| Archer L | Speed 650 for 0.2 s; cooldown 1.2 s |
| Mage L | Speed 600 for 0.2 s; cooldown 1 s |
| Katana L | Speed 750 for 0.16 s; cooldown 0.9 s |

Gloves reduce K/L/O cooldowns; boots change ordinary movement, not these dash speeds.
Archer arrows/rain and Katana attacks are Physical. Mage bolts/circle are Elemental.
Damage category is independent of an attack's element tag; F4's debug tag does not create
an equipment enchantment system.

## Behavior boundaries

Arrow Rain and Magic Circle capture target geometry at activation; they do not follow a
moving Boss. Each wave uses ordinary shared hit detection and fixed-origin wall visibility.
Mage Circle also locks its element. Preparation blocks movement and J/K/L/O. Positive HP loss
interrupts it without refunding cooldown, even if a potion heals within that step; zero damage
does not interrupt. Successful preparation starts the field on the next simulation update.
Mage K is usable in Battle but rejects unavailable/casting state and an active J launch window.

Archer untargetability suppresses AI selection, not incoming damage. Existing projectiles,
areas and other delivered damage can still hit. Random rapid Mage bolts draw independently
from five elements; both may select the same element. Tests can inject a seeded Random.

Katana interception occurs only after valid contact, hostility and wall checks. It blocks
one eligible invocation, not every hit in the same frame. Non-blockable reaction/field/counter
waves bypass parry. Interceptors record the source; they never move or submit counters from
inside damage settlement. Missed parry has recovery. Thrust follow-up requires a landed hit,
not merely overlap or blocked contact.

## Integration and cleanup

PlayerAbilities processes commands and scheduled submissions; CombatSystem settles damage;
`afterCombat()` then handles interruption, confirmed thrust and deferred counters. Boss decisions
are inserted before settlement by CombatDemo. A deferred counter submits for the next combat
update. Repeating afterCombat without another ability update does not duplicate responses.

Travel/recovery cancels casts, rain/circle, buffs, parry/recovery, dash/thrust, captured targets
and defensive callbacks. It restores targetability and clears old combat effects, while keeping
Player identity, class/O/element and remaining cooldowns. No save restores an active skill.
No-CD clears timers but leaves action/launch guards intact; see [debug tools](DEBUG_TRAINING_UI_REPORT.md).

## Verification

From Activity_2:

```sh
./gradlew core:test --tests '*FinalClassSkillsTest' --tests '*PlayerAbilitiesTest' \
  --tests '*ArcherAbilitiesTest' --tests '*MageAbilitiesTest' --tests '*KatanaAbilitiesTest' \
  --tests '*ElementClassIntegrationTest' --tests '*CoreGameplayIntegrationTest'
```

Tests exercise all classes/modes, real attack settlement, timing, interruption, interception,
fixed geometry, bounded follow-up, selection guards and cleanup. The production integration
suite also completes admitted fights through ordinary commands for all six class/mode pairs.
See [verified CI and native gaps](TEST_COVERAGE_REVIEW.md). Animation readability, input feel,
Boss matchups and full campaign balance still need human playtesting.
