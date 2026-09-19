# Stage 2 Architecture

## Current art presentation

The asset-free gameplay boundary is retained, but production rendering now composes `SceneArt` and
`ArtAssets` through the historical `PlaceholderRenderer`. See [Art integration](ART_INTEGRATION.md)
for the current resource inventory, supported animations and remaining content limitations. Older
placeholder-only rendering notes below describe the earlier foundation, not current visuals.
MapCatalog centralizes Hub bounds at 1080 x 400, Memorial Hall at 640 x 360 (one view), and battle/training bounds at 1600 x 1200;
collision and 640 x 360 camera behavior do not derive from images.
GameConfig supplies map-centred encounter bodies for Bosses and training targets, with nearby safety
fallbacks only when collision or player occupancy prevents an exact-centre spawn.
TrainingAppearance chooses independent arena/Boss artwork per visit; the original stationary
TrainingDummy remains the only practice target and R preserves the visit's appearance.
CombatSystem exposes bounded, drain-once HitFeedback after confirmed settlement, with no art references
or damage callbacks into presentation. Fields, attachments, facing and skill state are read-only inputs
to the view. World visuals clear on transition; short immutable result HUD replays cannot retain an
old Boss, delay return, alter Quest state or claim rewards.

This document defines the module boundaries for the gameplay loop:

```text
Hub -> accept quest -> portal -> single-Boss battle -> success/failure -> Hub
```

The existing module boundary keeps Quest state outside gameplay/combat. The requested minimal
NPC quest implementation now lives in `quest.BossQuests`; it consumes EncounterFlow results
without putting quest mutations into Health, CombatSystem or Boss. Future Developer B work
should extend/replace this owner, not introduce a second independent quest progress store.

## Current production encounter boundary

### Background music ownership

`audio.BackgroundMusic` owns up to three lazy LibGDX `Music` streams for the lifetime of GameScreen,
not a map or encounter. After `BossQuests.synchronize()`, the screen supplies the current MapType and
whether all distinct guardian stones are collected. Training/Battle select battle music; Hub/Memorial
select Hub music until completion, then victory music. The ending-dialogue flag is not involved.
An unchanged Track selection does nothing, preserving playback across Hub/Hall portals and repeated
frames. A changed Track stops the old stream before playing the cached/new loop. Pause uses `pause`,
resume uses `play` without a seek, and disposal releases each loaded stream once. Music is streamed
with `newMusic`, not loaded as a large in-memory `Sound`. Portable `audio/*.wav` classpath resources
work through the existing desktop packaging; no build-path override or additional audio engine exists.

### Portal admission and quest progress

The current local entry adapter is `npc.PortalGuide`: Hub contains one initially disabled
portal (`TravelCatalog.QUEST_PORTAL_ID`) and a nearby destination-selection NPC. E opens
the choice, Up/Down selects Training, Battle or Memorial Hall, and E confirms without teleporting.
Walking into the configured portal performs normal travel.
Selecting Battle requires an already accepted EncounterFlow permission and re-applies that exact
route. Portal Guide never constructs acceptance IDs. No task means a locked Battle option.
Selecting Training or Memorial Hall reconfigures the same portal without clearing the accepted quest/permission;
returning and choosing Battle restores its route and retry identity. Reward outbox data is preserved.

`BossChallenge` defines five distinct Boss/map identities. `BossQuests.accept` is the Quest Keeper's
Hub-only adapter: validate/prepare gameplay first, then store the accepted target and unique
acceptance. It rejects completed targets and retains identity for an unfinished repeat selection.
`EncounterFlow.prepare` records this permission while closing all owned portals; accepting,
replacing or re-accepting a quest never opens a gate. `activate` reuses the same validation/preparation
and opens the chosen portal only when Portal Guide confirms Battle. Merely viewing its label/menu
does not activate anything. Invalid preparation preserves previous permission and portal states.
`synchronize` observes the exact accepted BattleResult after settlement: FAILURE retains the task;
SUCCESS adds the distinct target to an immutable-exposed completion set and clears the active task.
Old/unrelated results cannot complete a different task. It never claims material rewards.
`consumeEnding` emits once after all five distinct completions, only in a living Memorial Hall.
GameScreen calls it explicitly when the player interacts with Old Sage, never during frame polling
or the automatic fifth-Boss return. GameScreen then owns a short modal E-advanced ending dialogue;
completed rows use geometric check marks rather
than depending on Unicode glyph availability. NPC dialogue is drawn above the HUD.
`DialogueLayout` separates title, body and controls using the view's actual wrapped GlyphLayout
measurements. Panel height is content-driven, with capped reference-size fonts, two-axis shrinking
and additional fitting for long pages. Quest check marks account for wrapped preceding rows.
GameScreen restores the full framebuffer viewport for UI after drawing the letterboxed world,
so non-16:9 windows do not stretch/compress dialogue geometry.

`MapType.MEMORIAL` is a peaceful room: CombatDemo creates neither Boss nor training dummy there.
`TravelCatalog.createGameplayPortals()` combines the sole Hub task entrance with a separate always-open
`memorial-hub` return in the hall's lower-left corner. EncounterFlow owns only `QUEST_PORTAL_ID`, so quest
acceptance/completion cannot disable the hall exit. Both directions retain TravelSystem arrival protection.
Old Sage is now hall-local, not duplicated in Hub. SceneArt reads `BossQuests.getCompleted()` snapshots
to dim/light the five socket-aligned stone images; no inventory grant or second progress store exists.
The hall shares existing movement, collision, camera, travel and encounter cleanup. It does not permit
Hub-only class/O-mode changes. Its viewport-sized bounds remain fixed when the window is resized.

The five arenas reuse the existing Sentinel HP, AI, patterns and material tuning; only identities,
names and arena routing differ in this first content version. No second damage engine or final
elemental Boss mechanics are introduced. Progress/ending flags are in-memory only, not saved.

`MapType.TRAINING` is separate from `BATTLE`. It contains one `enemy.TrainingDummy`, with
no AI, movement, attacks, Boss reward or battle outcome. Damage is delivered by the existing
CombatSystem; the dummy records actual positive delivered damage and restores its Health
after every hit. CombatDemo discards its knockback requests to keep the target stationary.
GameScreen draws total/last damage and hit count using existing debug text. R clears the
practice combat scope/cooldowns and restores player HP. The Training-only Return to Hub NPC
uses `PortalGuide.returnFromTraining()` after E confirmation, reusing validated travel and
arrival protection without healing or creating BattleResult/rewards. U/1/2/3 debug travel
bindings are removed; the underlying fast-travel API and its contract tests remain supported.
Map transitions create a fresh dummy/score and clear old attacks, fields and ability activity.
Player identity/class/per-class O choices remain unchanged; class/O selection stays Hub-only.

`GameScreen` wires one `EncounterFlow(session, travel, taskPortalIds)` into
`CombatDemo(session, encounterFlow)`. `GameScreen.getEncounterFlow()` is the adapter entry point.
The one-argument CombatDemo constructor remains an unrestricted standalone debug/test harness;
it is not the production Quest route. Automatic Boss creation after arbitrary travel applies
only to that harness. The production route creates exactly one Boss and no ordinary mobs for
each admitted built-in Battle map/spawn; generic Enemy/EnemyAI remain reusable components.

- `ActiveEncounter(id, mapId, spawnId, portalId)` is an external acceptance permission,
  not a Quest. `prepare`/`activate`/`clearActiveEncounter` require a living Hub player. Preparation
  validates a Battle map and full-body spawn before closing old routes; activation additionally opens it.
  Encounter IDs identify acceptances, not maps; a completed acceptance ID cannot be replayed.
- Production `TravelCatalog.createQuestPortals()` supplies one initially closed Hub route.
  No ordinary battle return portal is generated. Normal return is driven by settlement.
- `TravelSystem.arrivedThrough` checks the exact transition stamp and portal ID. Fast travel
  retains its independent unlock/routing rules but cannot create a task Boss, even on the
  active task's map. Direct session transitions likewise provide no task admission.
- `CombatDemo.returnToHubIfBattleEnded` runs before input/travel and after shared combat.
  Player death wins ties; FAILURE retains permission for retry. SUCCESS synchronizes Boss
  death, observes its immutable MaterialDrop, completes the return and consumes the old Boss's
  one-time claim before normal cleanup. No damage callback changes maps.
- `EncounterFlow.complete` is the **gameplay coordinator** endpoint, not an adapter command
  to award rewards. SUCCESS stages its immutable payload in the outbox before calling the
  existing GameSession.switchMap, then closes permission and publishes the result. An invalid
  Hub transition removes the staged payload without publishing a result, closing permission
  or consuming Boss loot. Normal completion is followed immediately by CombatDemo's existing
  scope clear/rebind and Health.restoreFull. All steps run on the game-loop thread; there are
  no external adapter callbacks inside transition or damage delivery.
- SUCCESS clears permission/closes task portals. FAILURE leaves the same portal retryable.
  Both retain Player/Health/abilities/class, per-class O choices and Mage's selected element. Production loadout selection
  requires Hub (including same-class O changes); R cannot reset a production Battle encounter.
- `BattleResult(encounter, encounterVersion, outcome)` is the latest immutable snapshot.
  Polling does not execute anything. Developer B observes it on the game-loop thread and owns
  all Quest state changes. Distinct transition stamps distinguish retries of one acceptance.
- An in-memory `getPendingRewards()` outbox maps success results to immutable MaterialDrops.
  It survives all subsequent map changes and acceptances. `claimReward(result)` removes and
  transfers a payload at most once. Developer C's adapter must be ready to own it before
  claiming; no inventory add/capacity/pickup/save implementation is included.
- Rewards and completed acceptance IDs live only for this process/session. Pending rewards
  accumulate intentionally until consumed; persistence and transactional adapter recovery
  remain Developer B/C integration work. Only the latest BattleResult is retained; poll it
  in normal game-loop coordination rather than treating it as a durable event history.

See [Quest portal flow report](QUEST_PORTAL_FLOW_REPORT.md) for calls, tests and limitations.

### Core integration baseline

The headless integration suite uses the production EncounterFlow/CombatDemo wiring, real
TravelSystem and shared CombatSystem. It covers both built-in Battle spawns and both Training spawns,
exact Player/Boss membership, actual ability-driven victories, failure/retry/replacement,
reward-once delivery and Hub-only selection. BossQuestFlowTest additionally covers the actual
Quest Keeper acceptance, locked guide, all five distinct Boss routes, failed retries, training
detours, task replacement, success checks and one-time ending. Historical multi-Battle
contract tests use explicit fixture maps rather than treating the real Training Ground as a Boss arena.

Cross-scope tests leave 15 concrete skill phases through FAILURE, SUCCESS and normal fast
travel, while reaction fields/statuses and active attacks exist. Retained old references are
checked after return, then retry must create a fresh full-HP, phase-one, idle Boss. Ordinary
fast travel is not a task outcome: abandoning a living encounter clears its runtime scope
without inventing SUCCESS/FAILURE, restoring HP or consuming its permission.

These are JUnit integration fixtures, not the separate gameplay smoke-tester feature. Domain tests
require no graphics context. ArtIntegrationTest separately validates PNG files/metadata without a GPU;
desktop art playback/resize remain separate graphics checks.

## 1. Current package structure

```text
core/src/main/java/au/edu/anu/comp2120/
├── MainGame.java
├── GameSession.java
├── TravelSystem.java
├── ActiveEncounter.java
├── BattleResult.java
├── EncounterFlow.java
├── combat/
│   ├── Attack.java
│   ├── AttackSpec.java
│   ├── Combatant.java
│   ├── CombatSystem.java
│   ├── CombatStatus.java
│   ├── Damageable.java
│   ├── DamageType.java
│   ├── ElementType.java
│   ├── ElementStatus.java
│   ├── ElementRules.java
│   ├── ElementField.java
│   ├── Faction.java
│   ├── Health.java
│   ├── HitInterceptor.java
│   ├── Projectile.java
│   ├── ProjectileSpec.java
│   ├── ReactionTimer.java
│   └── ReactionTuning.java
├── enemy/
│   ├── Boss.java
│   ├── BossConfig.java
│   ├── BossState.java
│   ├── Enemy.java
│   ├── EnemyAI.java
│   ├── EnemyConfig.java
│   └── EnemyState.java
├── inventory/
│   ├── Inventory.java
│   └── MaterialDrop.java
├── npc/NPC.java
├── player/
│   ├── AbilityLoadout.java
│   ├── AbilitySlot.java
│   ├── AbilityState.java
│   ├── AttackShape.java
│   ├── ClassDefinition.java
│   ├── ClassSkills.java
│   ├── Player.java
│   ├── PlayerAbilities.java
│   ├── PlayerClass.java
│   ├── PlayerClasses.java
│   ├── PlayerCommand.java
│   ├── PlayerInput.java
│   ├── SkillKind.java
│   ├── SkillTuning.java
│   └── UltimateMode.java
├── screen/
│   ├── CameraFollow.java
│   ├── CombatDemo.java
│   ├── GameConfig.java
│   ├── GameScreen.java
│   └── PlaceholderRenderer.java
└── world/
    ├── AStarPathfinder.java
    ├── CollisionBox.java
    ├── CollisionSweep.java
    ├── FastTravelDestination.java
    ├── GameMap.java
    ├── MapCatalog.java
    ├── MapType.java
    ├── Portal.java
    ├── SpawnPoint.java
    ├── TravelCatalog.java
    └── WalkableArea.java

lwjgl3/src/main/java/au/edu/anu/comp2120/lwjgl3/
└── Lwjgl3Launcher.java
```

Dialogue remains part of `npc`, and Equipment remains part of `inventory` until
either area contains enough behaviour to justify another package. Quest and UI
are documented boundaries but do not have empty packages yet.

## 2. Module boundaries

### Application and screen

- Responsible for LibGDX lifecycle, update/render order, screen transitions, and
  coordination of feature modules.
- Does not implement movement, combat, AI, quests, dialogue, or inventory rules.
- May call feature modules, but feature modules must not depend on `GameScreen`.
- Public surface: LibGDX lifecycle methods on `MainGame` and `GameScreen`.
- `GameSession` coordinates registered maps, the current map, and the same player
  instance across transitions. The screen reads its state and invokes its public
  transition methods. Model classes do not require a graphics context.
- A transition validates the target ID, spawn ID, and full-body occupancy before
  changing player state or current map. Failure preserves the previous state.
- Fast-travel/debug-unlock keys and window-title feedback belong to `GameScreen`;
  geometric rendering and role colours belong to `PlaceholderRenderer`.
- `CameraFollow` calculates a visible rectangle using only subject/map bounds and
  viewport world dimensions. `PlaceholderRenderer` applies its centre before
  drawing; the player, map, and travel modules have no camera dependencies.

### Camera / Scrolling

- The current camera follows the player rectangle centre directly, independently
  on both axes. This provides continuous horizontal and vertical scrolling for
  the current open rectangular maps; room snapping and cinematic effects are absent.
- `GameConfig` defines a fixed 640 x 360 world-unit view, independent of map size.
  Maps are no longer scaled to fit the whole map into the window on transition.
- On a large map axis, clamp the view inside the map. On an equal/smaller axis,
  centre the map with equal solid-background margins instead of using an inverted
  clamp interval. Map/player geometry is never modified for camera purposes.
- The existing `FitViewport` maintains world extent and aspect ratio on window
  resize. Its resize call does not recenter the camera; minimized/zero-size
  windows leave the previous valid screen viewport in place.
- Follow is stateless: every draw uses current map/player bounds, so portal and
  fast-travel arrivals snap to their valid view without stale bounds or lag.
- Pure mathematical tests cover targets, all map edges/corners, small/equal/mixed
  axes, alternate viewport dimensions, and actual map/spawn transitions. Graphics
  lifecycle and visual resize checks remain desktop verification concerns.

### Player

- Owns health, position, collision dimensions, speed, normalized eight-direction movement,
  the selected immutable ClassDefinition, per-class UltimateMode choices and Mage element.
  First-version class skills execute through PlayerAbilities/ClassSkills, not Player subclasses.
  Inventory/equipment remain outside this module.
- Does not load maps, store dialogue, implement AI, or calculate shared damage.
- Depends on world contracts and the shared combat health model; inventory remains future work.
- Public surface: constructor, `updateMovement`, read-only position/geometry/speed,
  collision-constrained `moveBy`, and validated `enterArea`. `GameSession` calls `enterArea` for coordinated map
  changes. Movement continues to use `WalkableArea` rather than map internals.
- Implements `Damageable` by delegating to its own `Health`. `getHealth()` exposes
  queries and validated healing/restoration; the optional constructor capacity
  overrides `Player.DEFAULT_MAX_HP`. Movement/transitions do not reset health.

### Common player abilities

- PlayerInput samples held WASD plus edge-triggered J/K/L/O into PlayerCommand;
  it never selects class-specific damage rules. PlayerAbilities is the single
  controller per player, using injected immutable AbilityLoadout tuning.
- Attack-producing J/K/O executions submit rectangular areas or projectile volleys through the same canonical
  Combatant and CombatSystem pipeline. Independent slot cooldowns are owned by
  PlayerAbilities, so these invocations use zero source-level cooldown. Mage K is element selection,
  and buff/parry/cast entry points may create no immediate attack. Enemy
  source cooldowns and all shared damage/lifetime/hit filtering remain unchanged.
- READY/ACTIVE/COOLDOWN are observational states; update returns an accepted slot
  as the triggered event. Dead or unregistered owners report UNAVAILABLE and cannot
  move or trigger. No new state classes, factories or class hierarchy are required.
- L captures the normalized current/last direction (right initially), has fixed
  duration/speed and cooldown, and uses Player.moveBy -> WalkableArea.move. It
  cannot tunnel through walls; diagonal sliding follows existing axis sweeps.
  Only the remaining frame time after dash is used for normal movement. There is
  no invulnerability, damage, animation or cooldown refund when blocked.
- An active/new dash blocks new attacks. Otherwise O > K > J chooses the first
  ready requested attack; one new ability per step and no queued requests. Slot
  timers advance after input arbitration, matching combat-core's timing convention.
- CombatDemo coordinates player abilities, the single Boss, exactly one shared combat update,
  PlayerAbilities.afterCombat and settled outcomes. GameScreen checks outcomes before fast
  travel, and checks portal overlap only if coordination did not already change maps.
  Transition cleanup cancels attacks/dash/skills and rebinds the player adapter, preserving
  slot cooldowns. R is Hub-only in production; unrestricted fixtures retain explicit arena
  reset. Pause freezes simulation.
- Rendering reads existing attack rectangles; native window-title text shows slot
  lifecycle. No ability/controller/model loads art or depends on graphics APIs.
- All agreed first-version skills are implemented on these common slots, AttackShape strategies,
  projectiles and finite ClassSkills states, as detailed below. Balance remains provisional.
  Ultimate energy, swept portal triggers and combo buffering are not part of this baseline.

### Player defeat, success and Hub recovery

- A settled zero-HP player in a BATTLE map means challenge FAILURE. CombatDemo checks
  before accepting new commands and immediately after the shared CombatSystem update.
  No Health/Damageable callback switches maps; all hits in that settlement finish first.
  Player failure is handled before the post-combat Boss reward/phase synchronization.
- `returnToHubIfBattleEnded()` coordinates both outcomes, using EncounterFlow.complete for
  admitted task battles. The narrower returnToHubIfDefeated API remains available to existing
  callers. All returns use GameSession.switchMap(MapCatalog.HUB) and its default spawn;
  the destination must be HUB type and fit the full player.
  It then reuses `syncMap`: CombatSystem.clear removes every registration, source
  cooldown, attachment/status, ElementField and active attack, including projectiles and derived
  attacks. PlayerAbilities.bindAfterTransition clears dash, launch windows, invocation references,
  rain/circle schedules, casting/buff/parry/counter states, and old defensive/targetability capabilities.
  The old Boss is detached and its owned Enemy/AI/path/phase/pattern/drop state is no
  longer referenced or updated by the scene. External stale references cannot act or claim loot.
- Only after arrival and scope cleanup does `Health.restoreFull()` restore the player.
  Player, Health, PlayerAbilities and the complete ClassDefinition remain the same instances;
  remaining slot cooldowns are preserved under the existing transition policy. A fresh
  canonical Combatant is registered in Hub. The arrival frame has no further movement,
  ability execution or portal check; normal live input resumes on the next frame.
- GameScreen also checks settled death before processing travel shortcuts, preventing
  travel/reset/selection from bypassing failure. TravelSystem needs no new switching API:
  its existing transition-stamp guard handles the external Hub arrival and portal retry.
- `getLastDefeat()` exposes an optional immutable `(mapId, encounterVersion)` snapshot.
  Repeated polling cannot return/heal twice because successful recovery is alive in Hub.
  It retains only the latest completed failure, not a queue or a mutable Quest status.
  External Quest ownership remains unchanged; callers may leave their task ACTIVE.
- Every admitted built-in Battle Map creates one fresh sentinel and no ordinary mobs. Retry
  never reuses the old Boss: Health, AI, phase, pattern index, cooldown and pending loot
  start fresh. Custom maps need a walkable configured Boss placement; no invalid body is forced.
- Player stores one UltimateMode per PlayerClass. PlayerAbilities.selectUltimateMode and F5
  require Hub; recovery preserves these choices and Mage element while cancelling active skills.
  ClassDefinition supplies both ultimate definitions without duplicating Player or combat.
- Invalid Hub configuration fails explicitly without healing in Battle or losing Boss loot.
  Hub death itself is not a battle failure. SUCCESS retains its one-time material payload
  before scope removal, closes task admission and also restores the same player in Hub.
  Quest state, durable loot persistence, death effects and respawn UI remain external work.
  Production R cannot reset a battle; dead battle players return instead of reviving in place.

### Class composition and selection

- PlayerClass is an enum identity with stable explicit IDs, not a Player superclass
  or strategy interface. ClassDefinition composes that identity with AbilityLoadout.
  PlayerClasses provides all three immutable built-in configurations; Mage is the
  default identity, using elemental J, element-selection K and two stateful O modes. GameConfig aliases
  that default instead of duplicating class tuning or introducing a screen dependency.
- Every class uses the same final Player, Health, movement/collision, Combatant,
  PlayerAbilities, shared J/K/L/O slots, cooldowns and map/portal interactions.
  No subclass duplicates HP, movement, timing or damage formulas.
- AttackShape is a pure geometry Strategy returning an
  immutable CollisionBox from source hurtbox, reach and facing. AREA and FORWARD
  supply two actual behaviours. The existing AreaAttack record gains this strategy
  component; its two-argument constructor still means AREA for compatibility.
  No code in the common executor dispatches on Archer/Mage/Katana identities.
- Per-class J/K/O definitions choose hitbox strategy, range, damage, active window
  and cooldown. Dash varies speed/duration/cooldown through existing validated data.
  Archer J/K and Mage J select shared projectile delivery rather than AttackShape.
  Future spells/combos must keep the same damage pipeline rather than calling Health
  separately; there is no universal speculative skill execution framework.
- Player owns the selected definition. Its selection setter is package-private;
  running selection goes through PlayerAbilities.selectClass. The three-argument
  ability constructor respects the initial class; the legacy explicit-loadout
  constructor overrides initial tuning while retaining that identity.
- Selection rejects null, dead/stale, identical-definition or active player
  attack/dash/stateful-skill cases, including projectile launch windows and flight (null throws,
  the others return false). Successful selection
  preserves the player/health/adapter, map, position, speed, facing and remaining
  slot cooldown seconds. No registration or health reset is involved.
- PlayerInput samples F1/F2/F3 separately from PlayerCommand, with fixed F1/F2/F3
  priority and key-edge semantics. CombatDemo consumes same-frame new movement and
  abilities on selection attempts but still advances existing activity/AI/combat.
  Travel and R reset have precedence. Class persists across both map changes and R.
- F4 is another independent key-edge request, handled after class selection. It cycles
  NONE/WATER/FIRE/LIGHTNING/ICE/WIND through `ClassDefinition.withElement`, retaining
  all slot geometry/projectile/dash tuning. It uses the same guarded selection path and
  preserves cooldowns/HP/position. Rejected attempts consume new commands without queuing.
  F1/F2/F3 preserve the selected light-slot tag on the new class's J/K/O; F4 deliberately
  tags the definitions uniformly. Mage J and its field instead use the separate K-selected
  element; rapid bolts draw independently. Catalog definitions are not mutated.
- Rendering continues to consume generic attack rectangles, without class-specific
  assets or renderer branches. The native title displays the selected identity.

### Combat

- Owns shared hit-point validation, lifetimes, cooldowns, hit detection/occlusion, generic
  hit interception, ordered reaction calculation, physical vulnerability and final delivery.
  Equipment/armour integration and broader balance modifiers remain separate future work.
- Does not handle input, rendering, movement collision, loot storage, or AI.
- Depends on stable combat contracts rather than concrete entity types.
- Public surface: `Damageable.takeDamage(int)` and `Damageable.isAlive()`.
- `Health` implements that contract and provides `getMaxHp()`, `getCurrentHp()`,
  `isDead()`, `heal(int)`, and `restoreFull()`. Capacity is a fixed positive integer;
  current HP stays within `[0, maxHp]`. Every new instance starts full and alive.
- Negative damage/healing throws before mutation, zero is harmless, and positive
  amounts saturate without overflow. Healing returns the actual HP restored.
  Death is derived from zero HP. Healing does not revive; only explicit
  `restoreFull()` restores a dead entity to full health.
- Health has no graphics, input, callbacks, or entity-specific rules. PlayerAbilities
  blocks dead-player movement/abilities; GameScreen processes battle failure before travel.
  CombatDemo owns recovery, never Health. Use mutations on the game-loop thread.
- `Combatant` adapts `Damageable` with a stable faction and mutable reference to
  immutable world hurtbox data. It is not a new entity hierarchy or health model.
  Visual/collision sizes do not determine combat geometry; owners update hurtboxes
  after movement. Player and Enemy retain their existing health implementations.
- `AttackSpec` is reusable type/damage/duration/cooldown/element/category/blockability data;
  its legacy constructor uses NONE and physical damage. `Attack` is one
  system-created invocation: unique scope-local ID, source, fixed or projectile hitbox, active
  time, and lifetime receiver-identity ledger. Only the system mutates invocations.
- `CombatSystem` registers one adapter per receiver and owns per-source cooldowns.
  It filters self, same faction, dead, and already-hit targets before positive-area
  overlap and world occlusion, applies generic interception, calculates ordered reaction
  amplification and physical vulnerability centrally, then calls `Damageable.takeDamage`.
  Border contact alone is a miss. It never dispatches on Player/Enemy/Boss classes.
- Update samples at the start of each step then ages attacks/cooldowns. Expired
  invocations cannot settle later. Fixed areas sample current geometry; projectiles use
  swept wall/target detection. Finite periodic effects catch up bounded scheduled ticks,
  not historical target motion. Attack order then registration order determines settlement; dead sources
  cannot retaliate later in the step. Invalid time is rejected before any mutation.
- Register/attack requests reject nulls and invalid membership. Duplicate receiver
  aliases are rejected. Unregister cancels that source's attacks; target hit ledgers
  survive re-registration, while attachments/statuses and source-owned periodic effects are removed.
  Clear cancels all attacks/fields/registrations/cooldowns/attachments/statuses without
  changing HP, and invocation identifiers are not reused after clear.
- `screen/CombatDemo` is the asset-free encounter coordinator despite its historical name,
  not the damage engine. Its production constructor requires EncounterFlow permission;
  its standalone constructor retains unrestricted test/debug admission. It places one Boss
  in each admitted built-in battle map and clears scopes using GameSession's transition
  stamp. J/K/L/O delegate to PlayerAbilities; Boss attacks use its composed EnemyAI and patterns.
  R restores HP and resets state only where the constructor's selection/reset policy allows it.
  GameScreen routes requests. PlaceholderRenderer draws hurtbox/hitbox rectangles;
  HP, AI and ability states use native window-title text. All AI updates precede exactly one
  shared combat update per simulation step.
- No equipment armour, animation-driven combos or visual damage effects are implemented.
  First-version skills and reactions already use this pipeline. Future class/AI code supplies tuning and
  geometry to this pipeline instead of applying Health damage separately. Receivers
  must not re-enter the pipeline during damage delivery; simulation is single-threaded.

### Unified static-world attack occlusion

- WalkableArea.hasLineOfSight queries a segment against the same immutable GameMap obstacle
  rectangles used by movement and projectiles. CollisionSweep.segmentCrossesInterior uses
  slab intervals, without sampling, epsilon-expanded walls or a physics dependency. It does
  not alter the existing projectile rectangle sweep or wall-wins-equal-time policy.
- Fixed attacks require both positive hitbox/hurtbox overlap and clear line of sight before
  CombatSystem delivers damage, records the hit, applies an element or creates a reaction.
  Occluded candidates may become eligible later during the same active window; accepted
  targets still receive at most one hit per invocation. There is no class/Boss type dispatch.
- Attack captures an immutable origin anchor. The legacy tryAttack overload uses the cast-time
  source hurtbox centre; its explicit-origin overload supports effects. AttackShape.FORWARD
  uses source centre, while AREA/custom fixed shapes default to fixed hitbox centre. Player
  slots and Boss patterns pass this geometry policy to the common pipeline. Future custom
  melee shapes should override origin rather than inheriting fixed-area semantics.
- Derived reaction areas capture their own effect centre and use the same occlusion gate.
  Moving the original caster cannot redirect a fixed effect or its visibility ray. Every
  target uses its current hurtbox centre; partial visibility is intentionally not sampled.
- Solid wall interior crossings block; pure edge/corner tangency and endpoint-only boundary
  contact are clear. An origin/target centre inside a wall or outside world bounds is blocked.
  Non-finite coordinates throw explicitly. Placeholder attack outlines are not visually clipped.
- CombatSystem.bindWorld associates one WalkableArea with a scope. CombatDemo binds on map
  sync/reset, PlayerAbilities on binding, EnemyAI on creation, and projectile launches bind
  their existing collision provider. Rebinding the same instance is harmless; a different
  map requires clear first. Clear removes the world along with all old combat state.
  Standalone map-free combat calculations remain supported; gameplay adapters must bind a world.
- Archer Arrow Rain, Mage's fixed circle, reaction areas and ElementField ticks all use this
  fixed-centre gate. Katana melee/counters use their declared attack origins; Archer/Mage
  projectiles retain the original sweep and wall-wins-tie rule. Integration retains both
  unobstructed-hit and wall-miss tests. Geometry is not clipped to partial visibility.

### Ordered elemental reactions and one-generation delivery

The agreed first-version reactions use the same CombatSystem confirmed-hit calculation and
delivery boundary. ElementRules.DEFAULT matches **attachedElement → triggeringElement**;
the reverse order is a separate rule, not an unordered pair.

| Attached → triggering | Reaction | Result |
| --- | --- | --- |
| FIRE → WATER | Vaporize | Triggering damage × 2 |
| WATER → FIRE | Vaporize | Triggering damage × 1.5 |
| ICE → FIRE | Melt | Triggering damage × 2 |
| FIRE → ICE | Melt | Triggering damage × 1.5 |
| FIRE ↔ LIGHTNING | Overload | FIRE area damage, poise loss, collision-safe queued knockback |
| WATER ↔ LIGHTNING | Electro-Charged | Four target-local LIGHTNING ticks; no chain lightning |
| WATER ↔ ICE | Freeze | Finite movement/new-attack suppression |
| ICE ↔ LIGHTNING | Superconduct | ICE area damage and temporary physical resistance reduction |
| FIRE/WATER/ICE/LIGHTNING → WIND | Swirl | Fixed ground field carrying the consumed attachment element |

Reactions consume attachment without applying the incoming element again. Ordinary same-element
hits refresh; NONE preserves without refreshing. Non-reacting pairs replace with the incoming
element. WIND attached followed by another element does not swirl; the specified trigger is WIND.

DamageType is independent of AttackSpec.element. Archer arrows/rain and Katana slashes/counters
are PHYSICAL; Mage bolts/circle and all reaction children/ticks are ELEMENTAL. Baseline physical
resistance is zero; Superconduct temporarily reduces it by 0.25, giving physical damage × 1.25.
Damage is amplified in the shared calculator, floored once and saturated at Integer.MAX_VALUE.
Legacy constructors infer a category; explicit category is preserved when tagging/copying definitions.

CombatStatus is a small composition on Combatant: finite freeze/stagger/vulnerability, configurable
poise and queued displacement. EnemyAI applies displacement through WalkableArea.move outside hit
callbacks and pauses new decisions while controlled; Boss reuses that capability and reports
FROZEN/STAGGERED without a Boss-specific branch in CombatSystem. Existing active attacks are not
cancelled merely by freezing. Existing HP phase-transition cancellation remains unchanged.

ElementField has immutable Definition data, fixed square geometry, source/direct parent ID and
a finite ReactionTimer. Ground ticks use the existing overlap/centre-occlusion and damage pipeline;
WATER fields cause real damage. Charged status uses the same timer but settles only its afflicted
target. Neither periodic effect emits ticks during zero-time spam. The first tick occurs after one
interval; the final scheduled boundary tick is allowed, but no post-expiry hitbox survives.

All reaction-generated attacks have nonzero direct parent provenance, ELEMENTAL category and
non-blockable ground-damage semantics. Derived hits cannot attach, refresh or react. One-shot
children settle next update; periodic clocks start advancing on the update after application.
Ordinary attachment aging retains the existing end-of-step semantics. Fixed fields never follow
Boss movement, and charged damage never jumps to another actor.

Limits: 64 live derived attacks, 64 fields, at most 64 ticks per configured periodic instance.
Capacity rejection consumes that scheduled opportunity without retry; it never repeats the original
reaction. Death/unregister clears target statuses. Source death/cancellation/unregister cancels its
fields and charged effects; existing map/encounter clear cancels everything, including retained
references. A target dying does not relocate or erase another living source's fixed field; normal
Boss outcome return clears that entire encounter.

No assets, VFX, audio, renderer changes or weapon-enchantment system were introduced. Mage still
uses K-selected elements, independent rapid-shot RNG and cast-locked circle element. Existing F4
is only the previously present debug tag path, not a new weapon-enchantment mechanic.

See [Element reaction completion report](ELEMENT_REACTION_COMPLETION_REPORT.md) for tuning,
files, validation and limitations.

### First-version class skills and shared execution

- PlayerClasses defines J/K/L and both O modes. AbilityLoadout.AreaAttack carries SkillKind;
  direct/custom AttackShape and ProjectileSpec constructors remain compatible.
  ClassSkills is a small stateful component of PlayerAbilities, not another combat engine.
  SkillTuning centralizes new durations, wave counts, movement speeds and cast/buff parameters.
- Archer J is one non-piercing arrow; K is five arrows across 40 degrees. Each projectile has
  its own Attack and hit ledger. O A captures the nearest eligible hostile's position and emits
  six fixed-area rain waves. O B temporarily sets the canonical Combatant untargetable.
- Mage J uses Player's selected element; K cycles FIRE/WATER/LIGHTNING/ICE/WIND without attacking.
  O A captures position and element, locks inputs for a damage-interruptible cast, then emits six
  field waves. O B temporarily lowers J cooldown and emits two independently random-element
  projectiles. The PlayerAbilities constructor accepts Random for deterministic tests.
- Katana J uses the existing facing-based slash. K opens a parry window, then either a
  collision-safe four-wave counter dash or missed-parry recovery. O A blocks one hit and
  submits one strong counter. O B advances through collision-safe thrust and creates six
  follow-up waves only after the probe records an accepted hit on the selected target.
- Combatant exposes generic targetability and an optional HitInterceptor. EnemyAI (also used
  by Boss) refuses new targeting when isTargetable is false. Damage delivery does NOT use
  targetability as immunity: already-active attacks still hit.
- CombatSystem checks blockable/interception after valid hit geometry and world occlusion,
  before damage/elements. Blocked hits consume the ordinary hit ledger but not landedHits.
  Interceptors only latch state. Attack.hasLandedHit distinguishes accepted thrust contact
  from blocked contact. No class identity checks exist in CombatSystem.
- Game-loop order is PlayerAbilities.update, Boss.update, CombatSystem.update, PlayerAbilities.afterCombat,
  then encounter outcome coordination. afterCombat handles deferred counters and cast interruption.
  Health exposes a monotonically changing effective-damage stamp, with no class-specific code;
  damage followed by healing still interrupts a cast, while zero/dead-target damage does not.
- Fixed field/counter/reaction waves are non-blockable AttackSpec submissions with explicit
  bounded counts. Ordinary existing attacks default to blockable for constructor compatibility.
  They still obey friendly/dead-target filtering, Health, elements and world occlusion.
- Shared projectile delivery retains swept wall/target collision, range/lifetime expiry,
  non-piercing hits, wall-wins-equal-time ties and cleanup. A mixed-AttackSpec volley overload
  validates all shots and reserves capacity atomically; no partial Archer fan or Mage double shot.
  Capacity remains 256 live projectiles and at most nine per volley.
- Player owns persistent per-class UltimateMode selections (A initially) and Mage element.
  selectUltimateMode always validates living Hub state and inactive execution, even in a
  debug harness. F5 calls this API. Mode/class switches preserve remaining cooldowns.
- Transition/reset unregisters defensive capabilities, restores targetability and clears
  captured references, pending counters, fields, casts, buffs and movement activity.
  Existing encounter cleanup cancels attacks/projectiles/elements; choices survive.
- Geometry remains axis-aligned placeholders and centre-based occlusion. Finite catch-up
  waves sample current target geometry, not historical movement. No assets or global event bus.

See [the current skill report](FINAL_CLASS_SKILLS_REPORT.md) for tuning and tests.

### Boss encounters and material drops

- Boss is a small encounter coordinator in `enemy`, composed of an existing Enemy
  health owner and EnemyAI navigation controller. The canonical Combatant wraps that
  Enemy; Boss does not introduce a second Damageable/Health identity. Collision body
  and hurtbox come from EnemyAI and are independent of final visual dimensions.
- EnemyAI's three-argument update optionally disables basic attack submission while
  preserving detection/loss hysteresis, A*, collision and attack readiness. Its original
  update delegates with submission enabled, preserving ordinary enemy behavior.
- BossConfig holds max HP, EnemyConfig navigation, threshold, transition duration,
  immutable ordered per-phase Pattern lists and material metadata. Patterns compose
  existing AttackSpec/AttackShape, with cooldown >= active duration and reach >= stopping
  distance. Boss advances the cycle only after CombatSystem accepts a cast.
- Phase 1 changes to phase 2 at the inclusive HP threshold, never reverses on healing,
  and pauses new movement/attacks once. CombatSystem.cancelAttacks cancels this source's
  fixed/projectile invocations and source-owned periodic effects without resetting registrations or cooldowns. It is also
  reused by unregister; other sources and damage ledgers remain unchanged.
- Boss.update runs before the arena's single combat update; synchronizeOutcome runs
  afterwards to process settled HP. Outcome handling is never called inside a damage
  receiver callback. Death wins over phase change and latches the encounter as defeated.
- MaterialDrop is immutable data in `inventory`: unique reward ID, material type,
  quantity, map ID and collision rectangle. Boss creates it once on defeat at its last
  valid location. getDrop/claimDrop provide a minimal read/one-time-transfer boundary;
  In formal task flow CombatDemo transfers this payload into EncounterFlow's outbox before
  scope cleanup. External adapters use getPendingRewards/claimReward, not the detached Boss's
  claimDrop. Inventory remains untouched; its adapter must be ready to accept the payload
  before the one-time claim on the simulation thread. No pickup-distance UI is required here.
- Production CombatDemo owns a single sentinel on each admitted built-in battle map, without ordinary enemies.
  Safe placement candidates and provisional pattern tuning are in GameConfig. Map changes
  clear registrations/attacks; reset replaces the Boss rather than reviving the rewarded
  instance. Formal SUCCESS rewards survive later travel/encounters in the in-memory outbox;
  they are not save-persisted. Unrestricted demo loot remains scope-local. Old Bosses cannot
  expose/claim rewards after detachment. Generic Boss supports shared-engine use without
  changing production's one-Boss membership rule.
- PlaceholderRenderer draws the existing large enemy hurtbox and gold MaterialDrop
  rectangles. The native title displays phase/HP/state/material text, with no asset load.
  There is no Boss UI, cutscene, animation, pickup UI or inventory rewrite.
- Limitations: two static rectangular pattern cycles, conservative navigation corridors,
  no actor-body collision, no visual hitbox clipping, no save/defeat persistence. An
  attack requires an initially clear target corridor; all targets also pass post-cast
  world occlusion at settlement. A defeated encounter stays stopped even if its Health is restored;
  a deliberate new encounter/reset is needed to fight and reward it again.

### Enemy

- Owns enemy health, per-enemy decision state, and collision-constrained pursuit.
- Does not define separate damage formulas, load maps, or handle player input.
- Depends on `combat` and `world` contracts, never on screens or graphics APIs.
- Constructors create full, independently owned `Health`, using `DEFAULT_MAX_HP`
  or an explicit capacity. Implements `Damageable` by delegation and exposes
  `getHealth()` with the same semantics as Player. Health/damage implementation is unchanged.
- `EnemyAI` composes Enemy, an independent CollisionBox, WalkableArea, EnemyConfig,
  and a registered Combatant. A normal collection supports multiple controllers;
  no ECS, entity inheritance hierarchy, or singleton enemy is introduced.
- `EnemyState` is an enum-based finite-state model: IDLE acquires within detection
  radius; CHASE approaches through WalkableArea.move; ATTACK stops and requests
  attacks via CombatSystem; DEAD disables movement/new requests. Loss beyond a
  separately configured larger radius returns to IDLE without teleporting home.
- Ranges use centre distance. Attack comparison has a four-coordinate-ULP allowance
  to absorb normalized-movement rounding at the stop boundary. Map collision stays
  unchanged. Large steps stop at attack range instead of overshooting the target.
- Clear pursuit stays direct; blocked pursuit uses `world/AStarPathfinder`. The
  bounded four-neighbour grid samples full-body placements and validates complete
  edge envelopes, including off-grid connectors and thin-wall/corner clearance.
  Queue priority is travelled distance plus an admissible Euclidean stand-off
  heuristic. Grid membership is static; search arrays are local to each query.
- EnemyAI caches a route, waypoint index, target snapshot, and replan timer. All
  waypoint motion shares one speed-times-delta budget and uses WalkableArea.move.
  Moving-target and failed-path retries are throttled; clear direct approaches
  bypass A*. Death, lost targets, resets, and map changes discard route state.
  AI also checks a conservative clear corridor before initiating an attack.
- AI owns no damage formula or cooldown clock. The arena updates all AI, then one
  CombatSystem update settles hits, health/death, and timing. A health-aware state
  getter reports death immediately after settlement. Invalid/dead/friendly/absent
  targets and controllers removed from the arena stop pursuing.
- EnemyConfig validates positive finite speed/ranges, attack <= detection <= loss,
  and positive autonomous cooldown. GameConfig supplies demo values; generic AI
  accepts per-enemy tuning through its constructor.
- Navigation defaults are a 16-unit grid and a 0.5-second blocked-path replan
  interval, configurable through EnemyConfig with a backward-compatible constructor.
  WalkableArea exposes outer bounds for finite navigation. Grids over 20,000 nodes
  fail explicitly; grid resolution can miss passages narrower than its sampling.
- EnemyAI remains reusable and tested independently; the gameplay scene only creates
  Boss-owned navigation. Placement checks use map/player geometry. New battle scopes
  create a fresh Boss; this is not a persistent spawn/save system.
- No behaviour trees, patrol/home return, crowd avoidance, or dynamic navigation
  graph updates are implemented. Sealed areas return no route; thin/off-grid
  passages may need finer sampling. Static-map collision occupancy is cached.
  AI attack-initiation clearance remains separate from the shared combat occlusion gate,
  which checks player attacks and collateral overlap hits when damage settles.

### World / Map

- Owns immutable map identity/name/type, dimensions, static collision rectangles,
  named spawn points, and default-spawn selection. File loading remains future work.
- `Portal`, `FastTravelDestination`, and `TravelCatalog` add immutable travel data
  without referencing players, input, rendering, or the screen.
- Does not implement attacks, dialogue, inventory, or quest rewards.
- Does not depend on concrete Player, Enemy, NPC, or UI classes.
- `WalkableArea` exposes point/full-body occupancy and swept X-then-Y movement.
  The previous collision algorithm is reused unchanged.
- `GameMap` validates metadata, obstacles, and spawn anchors on construction.
  Spawn entry separately checks the entering body's complete dimensions.
- `MapCatalog` supplies one Hub and two Battle Maps as Java data definitions.
  Additional maps use the same class; there are no per-map subclasses.
- World units have a bottom-left origin (x right, y up), independent of pixels.
  Walkable space is the outer rectangle minus obstacles; edge contact is allowed.

### Portal / Fast Travel

- `TravelSystem` is an application-level coordinator alongside `GameSession`.
  It owns trigger eligibility, arrival protection, and in-memory unlock/enabled
  state. Every actual transition calls the existing `GameSession.switchMap`.
- Portal registration validates source map/region and target map/spawn references.
  Full-player target occupancy is validated by GameSession when execution occurs.
- Automatic activation uses positive body/trigger overlap after movement. Only
  enabled current-map portals can trigger; registration order resolves ties.
- Arrival suppresses automatic triggering until an update observes the player
  outside every portal region, including disabled regions. The guard handles
  startup inside portals, portal arrivals, fast travel, and external/same-map
  transitions via a session transition stamp. Failed portal attempts also wait
  for exit before retrying, without changing map/player state.
- Fast travel requires a known unlocked destination; portal activation does not
  depend on fast-travel unlock state. Unlocks remain explicit and in-memory.
- Rendering receives immutable portal snapshots and draws only coloured
  rectangles. No visual assets, particles, or audio are part of travel logic.

### NPC / Dialogue

- Owns NPC identity, interaction, dialogue data, and dialogue flow.
- Does not implement movement, map collision, combat, or enemy AI.
- Depends only on read-only player information and the future Quest boundary.
- Public surface is currently the constructor.

### Quest

- Will own quest acceptance, progress, completion, and reward requests.
- Will not render UI or implement inventory, combat, or map loading.
- Will depend on stable World/NPC/Combat events and Inventory reward operations.
- No public surface or package exists until objective/reward data is agreed.

### Inventory / Equipment

- Owns item storage, add/remove operations, equipped items, and equipment values.
- Does not apply final combat damage, render UI, or track quests.
- Has no module dependency initially; consumers use public methods.
- Public surface is currently the constructor. Equipment remains here.

### UI

- Will own HUD, menus, dialogue presentation, and inventory display.
- Will not mutate feature internals or implement gameplay rules.
- Will read public APIs from Player, Quest, Inventory, and NPC.
- No separate package exists until there are reusable UI classes.

### Save / Game State

- Will own save-file creation, loading, validation, and versioning.
- Will coordinate immutable snapshots supplied by gameplay modules rather than
  reaching into their private fields.
- Will not implement player, world, quest, achievement, or inventory rules.
- No package or save format exists until the first required state snapshot is
  agreed by all module owners.

### Achievement

- Will own achievement definitions, progress, and unlock state.
- Will consume stable gameplay events and expose read-only progress to UI.
- Will not duplicate quest progress or combat calculations.
- No package exists until the initial achievement list is agreed.

## 3. Dependency direction

```text
GameScreen -> feature modules (coordination only)
GameScreen -> GameSession, TravelSystem, PlayerInput, CombatDemo, PlaceholderRenderer
PlaceholderRenderer -> CameraFollow, observational player/map/portal/combat geometry
CombatDemo -> GameSession, EncounterFlow, PlayerAbilities, Boss/BossConfig, CombatSystem, Combatant
EncounterFlow -> GameSession, TravelSystem, ActiveEncounter, BattleResult, immutable MaterialDrop
PlayerInput -> PlayerCommand, LibGDX Input (mapping only)
PlayerAbilities -> Player, ClassSkills, ClassDefinition, AbilityLoadout/Slot/State, AttackShape, PlayerCommand, CombatSystem, Combatant, AttackSpec
ClassSkills -> Player, CombatSystem, Combatant capabilities, finite skill tuning (no damage engine)
PlayerClasses -> ClassDefinition, PlayerClass, AbilityLoadout, AttackShape, AttackSpec (immutable Java data)
AttackShape -> CollisionBox (pure geometry strategy)
EnemyAI    -> Enemy, EnemyState, EnemyConfig, WalkableArea, AStarPathfinder, CombatSystem, Combatant
AStarPathfinder -> WalkableArea, CollisionBox (no entity, input, or graphics dependencies)
CombatSystem -> Combatant, Attack, AttackSpec, Projectile/ProjectileSpec, Damageable, CollisionBox/CollisionSweep, WalkableArea
CombatSystem -> ElementRules, CombatStatus, ElementField, ReactionTimer, DamageType (shared settlement)
Projectile -> CollisionBox, WalkableArea (no input, rendering or damage application)
CameraFollow -> CollisionBox (pure mathematics)
TravelSystem -> GameSession, Portal/FastTravelDestination data
GameSession -> GameMap/SpawnPoint, Player (transition coordination)
Player     -> WalkableArea, CollisionBox, Health, Damageable, ClassDefinition/PlayerClass/PlayerClasses
Enemy      -> Health, Damageable
Health     -> Damageable (no graphics, world, or entity dependencies)
NPC        -> read-only Player API, Quest boundary
Quest      -> World/NPC/Combat events, Inventory reward API
UI         -> read-only feature APIs
Combat     -> Damageable
World      -> no concrete Player, Enemy, NPC, or UI classes
Achievement -> stable gameplay events
Save       -> snapshot contracts supplied by feature modules
```

Feature modules do not depend on `GameScreen`, and Combat does not contain
type-specific branches. This prevents circular dependencies.

## 4. Necessary public interfaces

- `Damageable` provides one damage entry point for players, enemies, and bosses.
- `Health` is the shared implementation of bounded HP; Player and Enemy delegate
  instead of maintaining separate damage, healing, or death rules.
- `CombatSystem.register`, `tryAttack`, `update`, `unregister`, and `clear` manage
  arena membership and attack settlement. `Combatant.setHurtbox` synchronizes only
  vulnerable geometry; getters expose debug data without rendering dependencies.
- `WalkableArea` exposes collision queries and resolved movement without exposing
  obstacle storage or future map-loader details.
- `GameSession.switchMap(mapId[, spawnId])` remains the transition entry point.
  It preserves player identity, size, speed, and health and increments its transition
  stamp only on success, including same-map re-entry.
- `TravelSystem.updatePortals()`, `fastTravel(destinationId)`,
  `unlockDestination(destinationId)`, and `setPortalEnabled(portalId, enabled)`
  expose travel behaviour without UI or graphics dependencies.
- `CameraFollow.viewFor(subjectBounds, mapBounds, viewWidth, viewHeight)` returns
  the visible world rectangle without reading input or mutating gameplay state.
- `GameScreen.getEncounterFlow()` exposes the boundary for an external Quest adapter.
  `EncounterFlow.activate(ActiveEncounter)` accepts/replaces permission in Hub;
  `clearActiveEncounter()` removes it without modifying any Quest state.
  `getActiveEncounter()` and `isBattleActive()` expose permission/current admission.
- `EncounterFlow.getLastResult()` is observational. FAILURE leaves the permission retryable;
  SUCCESS clears it. `getPendingRewards()` retains unclaimed success payloads across maps;
  `claimReward(BattleResult)` transfers each once. External adapters must not call complete
  to manufacture a result or mutate gameplay while damage is being delivered.
- `PlayerAbilities.selectClass(ClassDefinition)` and `selectUltimateMode(UltimateMode)`
  are the production Hub-only selection boundary. Player getters expose persistent choices;
  update/afterCombat execute and settle skill state without graphics or UI ownership.
- `CombatSystem.getElementStatus(Combatant)`, `getElementFields()` and Combatant.getStatus()
  expose attachment/field/control observations. HitInterceptor and targetability are generic
  capabilities; elemental and physical damage still enter the same shared engine.

PlayerClass is now a stable enum, and AttackShape is a small geometry Strategy.
ClassDefinition/AbilityLoadout composition supplies class differences without
Player subclasses. No Observer, Factory, repository or universal skill hierarchy
has been introduced; future execution abstractions need a concrete skill use case.

## 5. Three-developer boundaries

| Owner | Primary boundary | Integration boundary |
| --- | --- | --- |
| Developer A — Daoyan Zhu (`u7782042`) | `player`, `combat`, `world`, `enemy`, portals | Inventory/equipment values, quest combat events, save snapshots, and screen integration |
| Developer B — name to confirm (`u7998420`) | `npc`, dialogue, `quest`, achievements, save/load | Read-only gameplay events, inventory reward operations, and module state snapshots |
| Developer C — name to confirm (`u7851266`) | `inventory`, equipment, crafting, UI, settings/debug UI | Read-only gameplay APIs, world data for the minimap, and combat equipment values |

Developer A owns the core gameplay path from player movement, maps, collision,
and portals through enemy behaviour and combat resolution. Developer B owns
conversations, quest flow, achievements, and persistence coordination.
Developer C owns items, equipment, crafting, and presentation/configuration of
gameplay state. Every gameplay owner must expose a small snapshot API needed by
Developer B; the save module must not access another module's private fields.

`u8350466` is not included in the active development allocation.

Each developer should change files inside their boundary. Changes to another
boundary or Shared Core require agreement before implementation.

## 6. Feature branches

Use one focused branch per feature instead of one long-lived branch per person.
Priority 0 is the minimum playable loop, Priority 1 completes the planned game,
and Priority 2 is optional polish if time remains.

### Developer A — Daoyan Zhu (`u7782042`)

The table below is the original branch-planning breakdown, not a list of missing features.
The current placeholder baseline includes these core systems, finalized first-version skills
and reactions, wall occlusion, death recovery and the task-portal/reward boundary described above.
This integration stage adds regression coverage and hardens lifecycle contracts, not new gameplay.

| Priority | Feature | Suggested branch |
| --- | --- | --- |
| P0 | Player position, WASD movement, and collision checks | `feature/player-movement` |
| P0 | Hub/combat maps, boundaries, and map switching | `feature/map-system` |
| P0 | Portals and fast travel between maps | `feature/portal-fast-travel` |
| P0 | Health, shared damage rules, and attack resolution | `feature/combat-system` |
| P0 | Enemy state, movement/following, and basic AI | `feature/enemy-ai` |
| P1 | Camera following and horizontal/room scrolling | `feature/world-camera` |
| P1 | J/K/L/O actions: light attack, special, dash, and ultimate | `feature/player-actions` |
| P1 | Archer, mage, and katana class behaviour | `feature/player-classes` |
| P1 | Boss combat and boss-material drop events | `feature/boss-combat` |
| P2 | Derived attacks and elemental reactions | `feature/advanced-attacks` |

### Developer B — `u7998420`

| Priority | Feature | Suggested branch |
| --- | --- | --- |
| P0 | NPC identity, interaction range, and interaction flow | `feature/npc-interaction` |
| P0 | Dialogue data and dialogue progression | `feature/npc-dialogue` |
| P0 | Quest acceptance, progress, completion, and rewards | `feature/quest-system` |
| P1 | Quest manager, priest, and beginner-guide hub flows | `feature/hub-npcs` |
| P1 | NPC movement and player-following behaviour | `feature/npc-following` |
| P1 | Save/load of player, map, quest, inventory, and achievement state, including the agreed special-save rule | `feature/save-load` |
| P1 | Achievement definitions, progress, and unlock events | `feature/achievements` |
| P2 | Guided tutorial/onboarding sequence | `feature/tutorial-flow` |

### Developer C — `u7851266`

| Priority | Feature | Suggested branch |
| --- | --- | --- |
| P0 | Item model and inventory add/remove/use operations | `feature/inventory-items` |
| P0 | Weapon/armour equipment and stat values | `feature/equipment-system` |
| P0 | HUD, menus, dialogue view, and inventory view | `feature/game-ui` |
| P1 | Four armour slots and two-/four-piece set bonuses | `feature/armour-set-bonuses` |
| P1 | Boss-material crafting, weapon forging, and enchanting | `feature/crafting-enchanting` |
| P1 | Minimap presentation using read-only world data | `feature/minimap-ui` |
| P1 | Health-bar and damage-number display settings | `feature/combat-display-settings` |
| P1 | Automated gameplay smoke tester for the main loop | `feature/gameplay-smoke-tester` |
| P2 | Debug-mode controls and diagnostic overlay | `feature/debug-mode-ui` |

### Shared integration

- Assign only one developer to each `GameScreen` integration change.
- Integrate completed P0 domain features before beginning P1 features.
- Use stable events/read-only interfaces for quest, achievement, save, minimap,
  and UI integration instead of depending on private fields.

## 7. Shared Core

- `core/src/main/java/au/edu/anu/comp2120/MainGame.java`
- `core/src/main/java/au/edu/anu/comp2120/screen/GameScreen.java`
- root and module `build.gradle` files
- `settings.gradle`
- `gradle.properties`

Changes to these files must be minimal and state the integration reason in the
commit message.

## 8. Git collaboration rules

1. Branch from the same up-to-date integration point and keep commits focused.
2. Do not mix package moves or unrelated refactors into a feature commit.
3. Agree on changes to public interfaces or Shared Core before implementation.
4. Only one branch performs a given `GameScreen` integration change.
5. Merge feature code before its small Shared Core integration change.
6. Never commit `.gradle/`, `build/`, IDE files, or local environment files.

## Decisions still requiring the team

- Confirm the names for Developers B and C.
- Current maps use Java rectangle data and named bottom-left spawn anchors in
  world units. Agree on any future external file format and portal triggers when
  those features are implemented.
- Agree on quest objective/reward data and the Inventory reward API before
  creating the `quest` package.
- First-version class behaviours and reaction rules are now fixed and implemented. Agree on
  balance refinements and future art/presentation mapping separately; those must preserve the
  shared damage, world-collision, Hub-only selection and one-generation reaction contracts.
- Agree on the first save-file version and snapshot fields before implementing
  save/load; do not serialize complete LibGDX objects directly.
- Decide which P2 features will be dropped if the P0/P1 schedule slips.
