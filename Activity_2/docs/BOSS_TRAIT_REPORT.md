# Guardian traits, status overlay and Boss visibility

Validated 22 September 2026. Scope: Developer A's per-guardian combat behaviour, the shared
control/immunity applications it needs, and the readable status presentation for both sides.
No Git/GitLab operations, new art or audio assets, quest data, save format or NPC logic were added.

## Problem

The five campaign guardians ran the same `Boss` class over the single `GameConfig.SENTINEL`
definition. Only the pursuit bolt's element differed, so health, speed, reach, patterns, damage,
phase threshold and reward were identical and the five fights felt the same. Two supporting gaps
also existed: queued reaction displacement and `blocksActions()` were consumed by `EnemyAI` but by
nobody on the player's side, so freeze and knockback had no effect on the player at all; and the
minimap drew obstacles, portals, NPCs and the player but never the Boss.

## Design

`BossTrait` is a strategy interface with default no-op methods, composed into the shared `Boss`
rather than subclassed from it. `Boss` resolves its trait from the arena's `BossChallenge` exactly
as `tryElementBolt` already resolved its element, so no call site had to change and every arena
outside the campaign (including every test map) receives `BossTrait.NONE` and behaves as before.

Health, damage settlement, collision, navigation, phases, death, rewards, admission and travel stay
in `Boss`, `CombatSystem` and `EncounterFlow`. A trait only observes events and adds its own effect:

| Hook | When it runs | Used by |
| --- | --- | --- |
| `tune(BossConfig)` | Once at construction, before rematch scaling | All five, for movement feel |
| `onLandedHit(boss, target, source)` | One decision step after the pipeline settled the hit | Water, fire, ice |
| `onDamageTaken(boss, target, damage)` | When settled health fell since the previous step | Lightning, wind |
| `update(boss, target, delta)` | Every decision step, including while suppressed | Fire, lightning, wind |
| `status()` / `afflictionLabel()` | Read-only, for diagnostics and the status overlay | Fire, and all for diagnostics |

Two observation choices keep the core untouched. Impacts are read from the existing public
`Attack.hasLandedHit`, the same deferred pattern `ClassSkills` already uses, so no trait runs inside
damage settlement. The damage signal is derived from settled `Health`, which needs no callback and
also covers reaction pulses, ground ticks and explicit debug damage.

Traits never touch a health model. `CombatSystem.tryAffliction` submits a bounded pulse that is
self-provenanced, therefore derived: it cannot attach an element or create reactions and children,
so an affliction cannot chain into another one. It keeps the shared faction, life, hit-ledger,
overlap and occlusion rules and ignores its specification's cooldown, so a burning or punishing
guardian never disturbs its own melee or ranged pacing.

## The five guardians

| Guardian | Signature mechanic | Tuning |
| --- | --- | --- |
| Fire | Every connected attack adds a burning stack that keeps dealing damage | 2 s per tick, 5 ticks, 5 stacks max, 2 damage per stack per tick; speed 78 |
| Water | Every connected attack shoves the target outward, resolved by its own collision | 120 world units, no action lock; speed 62 |
| Lightning | Taking no damage for ten seconds calls down a strike and a short action lock | 10 s, 10 damage, 0.6 s stagger; speed 92 |
| Ice | The third connected attack freezes the target outright | 3 hits, 2 s freeze; speed 54 |
| Wind | Burst damage makes it blink to a random bearing and distance, arriving untouchable | 45 damage in 1.5 s, 4 s cooldown, 120-320 units, 2 s immunity; speed 104 |

`tune` deliberately changes only navigation speed and the blocked-path replan interval, plus the
fire guardian's direct damage below. Health, detection and loss radii, attack stopping distance,
pattern geometry, pacing, elements, phase threshold, transition duration and reward stay at the
template's values, so one balance change still reaches all five and no existing expectation about
fresh-encounter HP, `IDLE` arrival state or the inclusive phase threshold is disturbed.

### Why the fire guardian trades direct damage

Adding burning on top of unchanged melee broke `CoreGameplayIntegrationTest`'s scripted victory for
a never-moving Mage. Isolation runs showed the cause was the burn, not the speed change: with burn
damage set to zero the fight passed, and the original victory margin was only **11 HP**. Slowing the
guardian to 44 recovered just 3 HP, so no burn value fits inside that margin. The fire guardian
therefore keeps **60%** of the template's direct pattern damage (sweep 15 to 9, slam 20 to 12,
heavy sweep 20 to 12, heavy slam 30 to 18) and returns the difference over time. Total output stays
comparable, the margin improved to **15 HP**, and the fight changes character: breaking contact so
the burn expires now matters more than surviving one hit.

### Why the wind blink is bounded

A first implementation drew the destination uniformly from the whole 1600 x 1200 arena, which is
literally random but let the guardian kite a stationary caster indefinitely: a scripted Mage fight
ran **300 simulated seconds** with the Boss still at 350/400 HP, because each blink was followed by
a long walk back. Bearing and distance are both still random, but the distance is bounded to
120-320 units: far enough to break contact, near enough to stay in the same fight. The same
scenario then finished in 12.9 seconds.

## Shared additions

- `CombatStatus` gains three external control applications, all validated and refreshing rather
  than stacking: `applyPush(dx, dy, distance)`, `applyFreeze(seconds)` and `applyStagger(seconds)`.
  It also gains a total immunity window, `applyInvulnerability(seconds)` with `isInvulnerable()` and
  `getInvulnerableRemaining()`, aged by the same `advance` call and cleared by the same `clear`.
- `CombatSystem.deliverDamage` negates every confirmed hit on an invulnerable receiver exactly as a
  defensive interception does: the attack's ledger is consumed so it cannot land again later in its
  own active window, no damage, element, reaction or landed-hit follow-up occurs, and presentation
  still observes a blocked hit. Unlike `setTargetable(false)`, which the contract limits to AI target
  selection, this does stop existing attack geometry, including non-blockable reaction pulses and
  ground ticks that a `HitInterceptor` cannot see.
- `EnemyAI.teleportTo` relocates a whole body to a fully walkable destination, discarding the cached
  route and queued displacement. A blocked, unrepresentable or non-finite destination is refused and
  leaves the enemy in place. It grants no immunity and changes no cooldown or existing geometry.
- `CombatDemo` now owns the player's side of these effects, as `EnemyAI` owns the enemy's: it
  resolves queued displacement through `Player.moveBy`, the same swept collision as ordinary
  movement and outside any damage callback, then re-syncs the hurtbox; and it substitutes
  `PlayerCommand.NONE` while the player's status blocks actions. This also activates the existing
  overload knockback and reaction freeze on the player, which previously had no gameplay effect.

## Status presentation

`StatusLabels` is a graphics-free mapping from a `CombatStatus`, plus an optional attacker-owned
label, to ordered uppercase labels and colours. `PlaceholderRenderer.drawStatusTags` stacks them
above every actor through the existing world-space name-tag batch, font and camera projection, so
labels follow their subject without hand-projected coordinates.

| Label | Source | Colour |
| --- | --- | --- |
| `FROZEN` | `CombatStatus.isFrozen()` | Ice blue |
| `STAGGERED` | `getStaggerRemaining()` | Orange |
| `BURNING xN` | Fire guardian's `afflictionLabel()` | Orange-red |
| `CHARGED` | `getChargedRemaining()` | Yellow |
| `VULNERABLE` | `getResistanceRemaining()` | Purple |
| `INVULNERABLE` | `isInvulnerable()` | Pale cyan |

Control effects come first because they decide what the player may do next. The immunity window is
included so that negated hits read as a mechanic rather than a bug. Shared statuses are read per
actor, so a guardian frozen by the player's own reaction is labelled too; burning needs the trait
channel because only the trait owns its stack count. Anchors match the scene's visual bodies rather
than hitboxes, and the player's labels clear its existing name tag. No new asset is loaded: the
existing `frozen.png` overlay and the built-in bitmap font are reused.

The minimap and the expanded `[M]` map also draw a red square for a living Boss, after the NPC
markers and before the green player marker, using the same world-to-minimap conversion as the
surrounding markers. A wind guardian's blink is reflected immediately because the marker reads
`getBounds()` directly. That marker shipped in commit `af7fa88` and had not been documented.

## Changed files

Paths relative to Activity_2:

- `core/src/main/java/au/edu/anu/comp2120/enemy/BossTrait.java`: new strategy interface with
  all-default hooks and the shared `NONE` behaviour.
- `core/src/main/java/au/edu/anu/comp2120/enemy/BossTraits.java`: the five guardian behaviours as
  private nested classes, the arena factory, and their tuning constants.
- `core/src/main/java/au/edu/anu/comp2120/enemy/Boss.java`: trait ownership, bounded retention of
  its own attacks for deferred impact observation, the settled-health damage signal, and the
  package-private `afflict`, `blinkTo` and `canAct` helpers traits use.
- `core/src/main/java/au/edu/anu/comp2120/enemy/BossChallenge.java`: documentation only.
- `core/src/main/java/au/edu/anu/comp2120/enemy/EnemyAI.java`: `teleportTo`.
- `core/src/main/java/au/edu/anu/comp2120/combat/CombatStatus.java`: push, freeze, stagger and
  invulnerability applications with shared duration validation.
- `core/src/main/java/au/edu/anu/comp2120/combat/CombatSystem.java`: `tryAffliction` and the
  invulnerability guard in the single damage boundary.
- `core/src/main/java/au/edu/anu/comp2120/screen/CombatDemo.java`: player displacement resolution,
  control-lock input suppression and trait diagnostics in the existing title text.
- `core/src/main/java/au/edu/anu/comp2120/screen/StatusLabels.java`: new graphics-free label mapping.
- `core/src/main/java/au/edu/anu/comp2120/screen/PlaceholderRenderer.java`: `drawStatusTags` and its
  layout constants.
- `core/src/test/java/au/edu/anu/comp2120/enemy/BossTraitTest.java`: 26 new cases.
- `core/src/test/java/au/edu/anu/comp2120/screen/StatusLabelsTest.java`: 6 new cases.
- `core/src/test/java/au/edu/anu/comp2120/screen/PlayerControlEffectsTest.java`: 3 new cases.
- `core/src/test/java/au/edu/anu/comp2120/enemy/BossAttackTest.java` and
  `core/src/test/java/au/edu/anu/comp2120/screen/CombatDemoTest.java`: two assertions read the
  submitted pattern's own damage instead of a hard-coded 15, because the fire guardian tunes it.
  Both tests keep their original subject, windup settlement and attack direction respectively.
- `README.md`, `docs/ARCHITECTURE.md`, `docs/ART_INTEGRATION.md`, `docs/BOSS_TRAIT_REPORT.md`:
  current behaviour and handoff.

Existing `Player`, `Health`, `Enemy`, `GameSession`, `EncounterFlow`, `TravelSystem`, `BossConfig`,
`BossScaling` and `ElementRules` implementations were reused without modification. No test was
weakened or disabled to obtain green results.

## Verification

Executed from Activity_2:

```bash
./gradlew :core:test
./gradlew test
./gradlew clean build javadocAudit --no-daemon
./gradlew :lwjgl3:run
```

- Final full suite: **1130 tests, zero failures, zero errors**.
- `./gradlew clean build javadocAudit --no-daemon`, the exact command CI runs: **BUILD SUCCESSFUL**.
  The audit rejected five undocumented nested-class constructors and one missing `@param` during
  development; both were fixed rather than suppressed.
- `./gradlew :lwjgl3:run` reached the real game loop with zero exceptions in the log and was then
  stopped. This is a boot and runtime-exception check, not manual gameplay acceptance.
- Trait tests use real `Boss`, real `CombatSystem` and real campaign arenas, with no graphics.
  They cover each mechanic, burning's stack cap and expiry, lightning's countdown reset, ice's
  counter reset, the wind blink's legality over 40 consecutive random destinations, full negation
  and element suppression during immunity, template invariants, death and detachment stopping every
  trait, control suppression stopping agency but not an already delivered affliction, and a
  regression guard proving a non-campaign arena is byte-for-byte unchanged in behaviour.
- The wind destination tests assert invariants that hold for every random outcome rather than a
  distribution, keeping the suite free of probabilistic assertions.

## Measured balance

A scripted fight was run per arena. `ARCHER` and `KATANA` move toward the guardian; `MAGE` never
moves, dashes or dodges, so it is a deliberate worst case rather than representative play.

| Arena | ARCHER | KATANA | MAGE, stationary |
| --- | --- | --- | --- |
| Fire (Crossroads) | Win | Win | Win, 15 HP left (enforced by CoreGameplayIntegrationTest) |
| Water | — | — | Win, 40 HP left |
| Lightning | — | — | Win, 11 HP left |
| Ice | Win, 4.7 s | Win, 4.7 s | **Loss**, Boss at 25/400 |
| Wind | Win, 7.7 s | Win, 7.9 s | **Loss**, Boss at 150/400 |

Two findings are recorded rather than silently balanced:

- The ice guardian's two-second freeze is lethal to a stationary caster. An earlier revision made
  each freeze also cost the guardian a second of its own attack time, which was removed on request.
  Single-constant levers remain: `FROSTBITE_FREEZE` or the ice guardian's speed.
- The stationary Mage also loses to the wind guardian, but an isolation run with the immunity
  disabled entirely produced the same loss with the Boss at 350/400 HP, and lengthening the blink
  cooldown made it worse. The cause is the wind guardian's speed of 104, the fastest of the five,
  not the random blink or the immunity. The immunity's measured cost is about three extra seconds
  for the two mobile classes. `BLINK_INVULNERABILITY` and the wind speed are the levers.

## Manual check

1. Start with `./gradlew lwjgl3:run` and accept a Boss quest from the Quest Keeper.
2. In the Crossroads arena, take a hit and watch `BURNING xN` appear above the player and the stack
   count rise in the window title; break contact and confirm it expires after five ticks.
3. In the ice arena, take three hits and confirm `FROZEN` above the player, no movement or new
   abilities for two seconds, and the existing frozen overlay.
4. In the wind arena, land a burst and confirm the guardian vanishes to an unpredictable nearby
   spot with `INVULNERABLE` above it, and that hits during that window deal nothing.
5. In the lightning arena, stop attacking for ten seconds and expect a strike and a brief lock.
6. In the water arena, confirm hits shove the player outward and that walls stop the shove.
7. Freeze any guardian with a water-plus-ice reaction and confirm `FROZEN` appears above it too.
8. Open the minimap and the `[M]` expanded map and confirm the red Boss marker tracks the guardian.

## Limitations / separate work

- The status overlay uses the built-in bitmap font scaled in world units, matching the existing name
  tags; edges are slightly soft at large window sizes. A crisper overlay needs a font asset.
  Up to five labels can stack above one actor, which is rare but tall.
- Rendering is not unit-tested, because it needs a GL context and the suite is graphically headless.
  The tested layers are the label mapping and the data the renderer reads.
- The lightning strike has no range limit, which is intentional for punishing disengagement but is a
  single-condition change if the team disagrees.
- Trait state is encounter-scoped and not saved. A fresh encounter creates a fresh trait, so burning
  stacks, counters, countdowns and blink cooldowns never survive travel, defeat or a debug reset.
- Final balance across all five arenas, per-guardian art or audio cues, and any status-icon artwork
  for burning remain separate work. The measured table above is scripted data, not playtesting.
