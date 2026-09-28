# Stage 2 Architecture

## Current state (2026-09-26)

This document matches `main` at `ab76422` (!42). Since the 2026-09-25 update, Hao Liu's
pause menu, save-slot management, merchant services, potion top-up and bounded gear upgrades
(`npc.MerchantServices`, `inventory.PotionSupply`, `inventory.GearUpgrades`) and adjustable
health capacity (`Health.setMaxHp`, driven by equipped headgear) are merged. Sections 5 and 6
remain the original pre-implementation plan; Section 6 notes what was actually delivered.

## Inventory and persistence integration (2026-09-25)

The current production loop includes Hao Liu's inventory/equipment and save/load work.
[Inventory/equipment](INVENTORY_EQUIPMENT_REPORT.md) defines item interactions and combat
bonuses; [Save/load](SAVE_LOAD_REPORT.md) defines slot policy, validation and restoration.
Earlier implementation/test reports describe their original checkout, not missing features
in the current game. Section 7 retains the older contribution snapshot with a current update.

The game-loop order after outcome settlement is: collect inventory rewards, reconcile worn
slots, synchronize BossQuests, then observe the settled result for autosave. A success snapshot
therefore includes Hub recovery, earned rewards and quest progress. Failure never writes an
automatic checkpoint. No damage callback performs disk IO or triggers a scene transition.

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
acceptance. Cleared guardians can be accepted again with a fresh ID; reselecting the same active
task retains its retry identity. Quest rows show per-guardian win counts beside the retained clear check.
`EncounterFlow.prepare` records this permission while closing all owned portals; accepting,
replacing or re-accepting a quest never opens a gate. `activate` reuses the same validation/preparation
and opens the chosen portal only when Portal Guide confirms Battle. Merely viewing its label/menu
does not activate anything. Invalid preparation preserves previous permission and portal states.
`synchronize` observes the exact accepted BattleResult after settlement: FAILURE retains the task;
SUCCESS adds the distinct target to an immutable-exposed completion set and clears the active task.
Old/unrelated results cannot complete a different task. It never claims material rewards.
`EncounterFlow` separately counts successful completed returns by arena ID (one Boss per campaign
arena). The count advances only after a valid success return, not on failure, reward claim, polling,
acceptance or practice. A completed acceptance ID remains non-replayable; a rematch uses a new ID.
`CombatDemo` passes the arena's previous count to a fresh Boss. `BossScaling` derives from original
tuning, using `HP multiplier = min(4, 1 + log10(wins + 1))` (HP rounded to an integer) and
`attack rate = min(2, 1 + 0.25 * log10(wins + 1))`. Both phases' melee cooldowns and ranged cooldown
are divided by this rate. Active windows remain unchanged and provide an additional cooldown floor.
The 0.8-second melee warning, damage, projectile speed, movement speed, phase threshold and reward
quantity remain unchanged. Effective melee cadence grows less than twice as fast because its warning
is preserved. Counts are per guardian and included in save snapshots; failed retries use the same
difficulty, and five repeated wins against one Boss still light only one stone.
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
Old Sage appears in both places with separate map-local NPC definitions: `HUB_GUIDE` at (184, 221)
provides repeatable beginner guidance through the ordinary dialogue path, while `GUIDE` in the Hall
retains stone progress and the ending conversation. Their distinct IDs prevent Hub dialogue from
entering the ending branch; neither Sage grants an inventory reward. SceneArt reads `BossQuests.getCompleted()` snapshots
to dim/light the five socket-aligned stone images; no inventory grant or second progress store exists.
After completing the Hall Sage's ending or subsequent completed-progress dialogue, E opens a
two-option credits menu (Up/Down to select, E to confirm). View credits pages through the supplied
acknowledgements; Exit dialogue and Escape close only the conversation. Credits remain available on
later completed Hall visits without replaying the one-time ending, and never appear in the Hub tutorial
or before all five guardians are defeated. CreditsDialogue owns only this small presentation state.
The hall shares existing movement, collision, camera, travel and encounter cleanup. It does not permit
Hub-only class/O-mode changes. Its viewport-sized bounds remain fixed when the window is resized.

The five arenas reuse the existing Sentinel HP, AI, patterns and material tuning; only identities,
names and arena routing differ in this first content version. No second damage engine or final
elemental Boss mechanics are introduced. Progress and the ending-consumed flag are included in save snapshots.

`MapType.TRAINING` is separate from `BATTLE`. It contains one `enemy.TrainingDummy`, with
no AI, movement, Boss reward or battle outcome; CombatDemo supplies fixed upward practice shots. Damage is delivered by the existing
CombatSystem; the dummy records actual positive delivered damage and restores its Health
after every hit. CombatDemo discards its knockback requests to keep the target stationary.
GameScreen draws total/last damage and hit count using existing debug text. R clears the
practice combat scope/cooldowns and restores player HP while retaining target/player positions. The Training-only Return to Hub NPC
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
  Polling does not execute anything. BossQuests observes it on the game-loop thread and owns
  Quest state changes. Distinct transition stamps distinguish retries of one acceptance.
- An in-memory `getPendingRewards()` outbox maps success results to immutable MaterialDrops.
  It survives all subsequent map changes and acceptances. `claimReward(result)` removes and
  transfers a payload at most once. InventoryRewards checks stack capacity before claiming,
  adds accepted rewards to the player bag and leaves overflowed rewards pending.
- Save snapshots include pending rewards and completed acceptance IDs together with inventory
  and quest state. This preserves replay protection across loading without a separate reward
  transaction log. Only the latest BattleResult is retained; ordinary game-loop coordination
  observes it once, rather than treating it as a durable event history.

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
├── audio/
│   └── BackgroundMusic.java
├── combat/
│   ├── Attack.java
│   ├── AttackSpec.java
│   ├── Combatant.java
│   ├── CombatSystem.java
│   ├── CombatStatus.java
│   ├── Damageable.java
│   ├── DamageType.java
│   ├── EquipmentModifiers.java
│   ├── ElementType.java
│   ├── ElementStatus.java
│   ├── ElementRules.java
│   ├── ElementField.java
│   ├── Faction.java
│   ├── Health.java
│   ├── HitFeedback.java
│   ├── HitInterceptor.java
│   ├── Projectile.java
│   ├── ProjectileSpec.java
│   ├── ReactionTimer.java
│   └── ReactionTuning.java
├── enemy/
│   ├── Boss.java
│   ├── BossChallenge.java
│   ├── BossConfig.java
│   ├── BossScaling.java
│   ├── BossState.java
│   ├── BossTrait.java
│   ├── BossTraits.java
│   ├── Enemy.java
│   ├── EnemyAI.java
│   ├── EnemyConfig.java
│   ├── EnemyState.java
│   └── TrainingDummy.java
├── inventory/
│   ├── Equipment.java
│   ├── EquipmentSlot.java
│   ├── GearUpgrades.java
│   ├── ItemCatalog.java
│   ├── InventoryRewards.java
│   ├── Inventory.java
│   ├── MaterialDrop.java
│   └── PotionSupply.java
├── npc/
│   ├── ClassTrainer.java
│   ├── MerchantServices.java
│   ├── NPC.java
│   └── PortalGuide.java
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
├── quest/
│   └── BossQuests.java
├── save/
│   ├── GameSaves.java
│   ├── InventorySnapshot.java
│   ├── LocationSnapshot.java
│   ├── SaveFields.java
│   ├── SaveFile.java
│   ├── SaveMetadata.java
│   ├── SaveSlot.java
│   └── SaveStore.java
├── screen/
│   ├── ArtAssets.java
│   ├── CameraFollow.java
│   ├── CombatDemo.java
│   ├── CreditsDialogue.java
│   ├── DialogueLayout.java
│   ├── GameConfig.java
│   ├── GameScreen.java
│   ├── HudLayout.java
│   ├── PlaceholderRenderer.java
│   ├── SaveMenu.java
│   ├── SceneArt.java
│   ├── StatusLabels.java
│   └── TrainingAppearance.java
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

Dialogue rendering (`DialogueLayout`, `CreditsDialogue`) remains part of `screen`,
and equipment and gear upgrades remain part of `inventory` until either area contains
enough behaviour to justify another package. No crafting package exists. `quest` and `audio` now exist as
small, single-class packages (`BossQuests` and `BackgroundMusic`); UI beyond
`screen`/`HudLayout` is still a documented boundary without its own package.

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
- Input handling and window-title feedback belong to `GameScreen` (the former U/1/2/3
  debug travel keys are removed); geometric rendering and role colours belong to `PlaceholderRenderer`.
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
- Player stores one UltimateMode per PlayerClass. PlayerAbilities.selectUltimateMode and Class Master
  require Hub; recovery preserves these choices and Mage element while cancelling active skills.
  ClassDefinition supplies both ultimate definitions without duplicating Player or combat.
- Invalid Hub configuration fails explicitly without healing in Battle or losing Boss loot.
  Hub death itself is not a battle failure. SUCCESS retains its one-time material payload
  before scope removal, closes task admission and also restores the same player in Hub.
  BossQuests, InventoryRewards and GameSaves handle progress, reward delivery and persistence
  outside combat. Failure recovery leaves existing disk checkpoints unchanged.
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
  Equipment bonuses enter through `EquipmentModifiers` in the same damage calculation.
- Does not handle input, rendering, movement collision, loot storage, or AI.
- Depends on stable combat contracts rather than concrete entity types.
- Public surface: `Damageable.takeDamage(int)` and `Damageable.isAlive()`.
- `Health` implements that contract and provides `getMaxHp()`, `getCurrentHp()`,
  `isDead()`, `heal(int)`, `restoreFull()` and `setMaxHp(int)`. Capacity is a positive
  integer; `setMaxHp` (used by Player when equipped headgear changes) preserves the health
  fraction, keeps living entities at one HP or more and never revives or records a hit.
  Current HP stays within `[0, maxHp]`. Every new instance starts full and alive.
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
- `CombatSystem.tryAffliction` submits one bounded pulse for attacker-owned damage-over-time or
  punishment effects, so they still pass the shared faction, life, hit-ledger, overlap and occlusion
  rules instead of touching a health model. The pulse is self-provenanced and therefore derived: it
  attaches no element and creates no reaction or child, so afflictions cannot chain. Its
  specification's cooldown is ignored, leaving the source's own melee/ranged pacing untouched.
- `CombatStatus` also carries externally applied control and immunity: push, freeze and stagger with
  explicit durations, and a total invulnerability window. All refresh rather than stack, are
  validated before mutation, age in the same `advance` call and clear with the rest of the status.
  An invulnerable receiver has every confirmed hit negated at the single damage boundary exactly as
  a defensive interception does, consuming the attack's ledger and suppressing damage, element,
  reaction and landed-hit follow-up. Unlike untargetability, which the contract limits to AI target
  selection, this does stop existing attack geometry, including non-blockable pulses and ground ticks.
- Displacement and control locks are applied by movement owners, never inside settlement: `EnemyAI`
  resolves its own queued displacement, and `CombatDemo` resolves the player's through
  `Player.moveBy` and substitutes an empty command while the player's status blocks actions.
- No equipment armour, animation-driven combos or visual damage effects are implemented.
  First-version skills and reactions already use this pipeline. Future class/AI code supplies tuning and
  geometry to this pipeline instead of applying Health damage separately. Receivers
  must not re-enter the pipeline during damage delivery; simulation is single-threaded.

### Boss melee telegraphs and elemental projectiles

Boss owns a cancellable `WINDUP` state before each sweep/slam. It locks the attack rectangle and
occlusion origin, stops pursuit for `BossConfig.MELEE_WINDUP` (0.8 seconds), then submits that exact
geometry to the existing CombatSystem. Players can dodge the warning without the box tracking them.
Death, phase transition, freeze/stagger, lost eligibility or detachment cancel preparation. Pattern
index advances only after successful submission; shared source cooldown prevents melee/bolt spam.
SceneArt/PlaceholderRenderer read preparation progress and geometry to draw accelerating flashes
and a red border; rendering never creates attacks. A warning rectangle may extend across an obstacle,
but confirmed-hit wall occlusion still rejects blocked targets.

The five campaign guardians fire their matching element while in CHASE and within 480 units, with
12 provisional damage, 300 units/second speed, 2-second cooldown, 2-second maximum lifetime and
20-unit collision size. Bolts use the existing non-piercing Projectile/CombatSystem swept collision,
including wall-wins ties, elemental attachment, parry, expiry and encounter cleanup. Custom maps
without a BossChallenge mapping remain melee-only. TrainingDummy remains stationary; CombatDemo launches a fixed upward physical practice projectile every two seconds through CombatSystem. Player applies the 1 HP floor only in Training and restores full HP after five simulation seconds without positive damage. Leaving the map clears practice metrics and removes these health rules.
The renderer uses independent copies of Mage bolt art at twice its width and height; gameplay has
no texture dependency. GameScreen and combat damage rules are unchanged by these attack additions.

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
  debug harness. The legacy F5 input API remains, but SaveMenu consumes F5 before gameplay
  in production. Use Class Master for O selection. Mode/class switches preserve remaining cooldowns.
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
  claimDrop. InventoryRewards checks capacity before claiming on the simulation thread,
  then transfers the reward to the bag. No pickup-distance UI or NPC reward visit is required.
- Production CombatDemo owns a single sentinel on each admitted built-in battle map, without ordinary enemies.
  Safe placement candidates and provisional pattern tuning are in GameConfig. Map changes
  clear registrations/attacks; reset replaces the Boss rather than reviving the rewarded
  instance. Formal SUCCESS rewards survive later travel/encounters in the in-memory outbox;
  the bag and any overflowed pending payload are saved together. Unrestricted demo loot remains scope-local. Old Bosses cannot
  expose/claim rewards after detachment. Generic Boss supports shared-engine use without
  changing production's one-Boss membership rule.
- PlaceholderRenderer draws the existing large enemy hurtbox and gold MaterialDrop
  rectangles. The native title displays phase/HP/state/material text, with no asset load.
  There is no Boss UI, cutscene, animation, pickup UI or inventory rewrite.
- Limitations: two static rectangular pattern cycles, conservative navigation corridors,
  no actor-body collision, no visual hitbox clipping and no mid-battle actor persistence. An
  attack requires an initially clear target corridor; all targets also pass post-cast
  world occlusion at settlement. A defeated encounter stays stopped even if its Health is restored;
  a deliberate new encounter/reset is needed to fight and reward it again.

### Guardian traits

- `BossTrait` is a strategy interface with all-default methods, composed into the shared `Boss`
  instead of subclassed from it. `Boss` resolves one trait from the arena's `BossChallenge`, the
  same lookup `tryElementBolt` already used for its element, so no caller changed and every
  non-campaign arena, including all test maps, receives `BossTrait.NONE` and is unchanged.
- Traits are stateful for exactly one encounter; the factory returns a new instance per Boss, so
  stacks, counters, countdowns and cooldowns never survive travel, defeat or a debug reset.
- Health, damage settlement, collision, navigation, phases, death, rewards, admission and travel
  remain owned by `Boss`, `CombatSystem` and `EncounterFlow`. A trait only observes and adds.
- Observation is deferred, never a damage callback. Landed impacts are read from the existing public
  `Attack.hasLandedHit` one decision step after settlement, the pattern `ClassSkills` already uses,
  from a bounded list of the Boss's own submitted attacks. Damage taken is derived from settled
  `Health`, which needs no callback and also covers reaction pulses, ground ticks and debug damage.
- `tune(BossConfig)` adjusts the shared template before rematch scaling and is restricted to
  navigation speed and the replan interval, plus the fire guardian's direct pattern damage. Health,
  detection and loss radii, attack stopping distance, pattern geometry, pacing, elements, phase
  threshold, transition duration and reward stay at the template's values, so a single balance
  change still reaches all five guardians.
- Effects that represent Boss agency, rather than an already delivered affliction, additionally
  check `Boss.canAct()`, so a frozen or transitioning guardian cannot act while a burn it already
  applied keeps running. Traits apply damage only through `CombatSystem.tryAffliction` and reposition
  only through `Boss.blinkTo`, which drops the prepared strike, the cached route and its own attacks.
- Wind blink destinations are sampled 180–320 world units from the current player hurtbox centre,
  not from the Boss's old position, with at least 120 units of displacement from its old centre.
  Both distance limits, target non-overlap and existing full-body world collision must pass.
  Twenty-four random candidates are followed by a bounded 48-bearing, three-radius fallback;
  no safe candidate means no blink, no immunity and no dodge cooldown consumption. Candidates
  are never clamped outside the distance band. A successful trait relocation consumes that entire
  Boss decision step, so pursuit or a new melee/ranged attack cannot resume in the arrival frame.
- The fire guardian keeps 60% of the template's direct pattern damage and returns the difference as
  burning stacks, so its total output stays comparable while the fight rewards breaking contact.
  See [Boss trait report](BOSS_TRAIT_REPORT.md) for the per-guardian tuning and measured balance.
- Limitations: one behaviour per arena, no trait composition or runtime replacement, no persistence,
  and no per-guardian art or audio cue. Final balance across all five arenas remains open.

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
- Depends only on read-only player information and the `quest.BossQuests` boundary.
- `NPC` exposes identity, position, dialogue lines, adjacency (`isAdjacent`) and a one-time
  reward flag (`hasInteracted`/`setInteracted`/`restoreInteracted`, persisted by saves).
  `PortalGuide`, `ClassTrainer` and `MerchantServices` are small Hub adapters; Hub NPCs,
  including the Notice Board and Storage Chest entries, are defined in `GameConfig`.
- `MerchantServices` offers three independent choices: the once-only Lightning Staff, a repeatable
  potion top-up (`PotionSupply`: bag plus chest to a combined three, never farming more) and the
  equipment upgrade menu (`GearUpgrades`).

### Quest

- `quest.BossQuests` owns acceptance, the active target, distinct completions, per-guardian
  success counts and the one-time ending (`accept`, `synchronize`, `getCompleted`,
  `getSuccessCount`, `consumeEnding`). See "Portal admission and quest progress" above.
- Does not render UI or implement inventory, combat, or map loading; material rewards are
  claimed separately through `InventoryRewards`.

### Inventory / Equipment

- Player owns the bag and Equipment; GameScreen owns separate chest storage. Both preserve
  identity across map changes and validated restoration.
- Inventory rejects overflowing stack additions. ItemCatalog defines supported gear and potion
  effects; Equipment owns weapon/head/body/hands/feet slots, validates bag ownership and
  reconciles removed items. Two/four-piece guardian bonuses are derived from current slots.
- Equipped items remain bag contents. Moving the last copy to storage removes its bonus.
- CombatSystem applies EquipmentModifiers in the existing damage calculation after reaction
  and physical modifiers, then floors once. No UI or save code calculates combat damage.
- InventoryRewards adapts EncounterFlow's outbox; it checks capacity before the one-time claim.
  Overflowed rewards stay pending and are included in the next save.
- GameScreen presents B-bag equip/unequip/potion actions and E-chest transfers. Storage input
  never equips; B switches to the bag. WEAPON reads only EquipmentSlot.WEAPON.
- `ItemCatalog.Gear` derives slot effects from the upgrade level: headgear adds maximum HP,
  gloves reduce K/L/O cooldowns and boots amplify ordinary movement. Equipment reports summed
  health bonuses through a capacity observer; Player applies them with `Health.setMaxHp`.
- `GearUpgrades` raises one carried item by one level (maximum +2), consuming one
  `sentinel-core` from the bag before the chest; a worn item stays worn and upgraded IDs
  (`id+1`, `id+2`) persist through saves and chest transfers. There is no crafting or enchanting.

### UI

- GameScreen and screen helpers own HUD, menus, dialogue and inventory presentation.
- They coordinate public feature APIs rather than mutating private model internals.
- SaveMenu consumes Escape, optional F5/F9 shortcuts and confirmations before gameplay.
  Its modal pauses simulation/animation without changing save safety. An ordinary open panel
  owns the first Escape; the next opens the menu. Shared layout drives drawing and hit-testing.
- GameSaves publishes operation sequence numbers; SaveMenu consumes each once for a four-second
  translucent toast with a final fade. No persistent notification competes with the training HUD.
- The music slider uses BackgroundMusic gain APIs; cached and future streams share the chosen
  gain without playback restart. It is independent of gameplay save snapshots.
- MainGame defers confirmed restart to the next application runnable, replaces GameScreen and
  disposes old resources. Fresh models and NPC flags are initialized, disk saves stay untouched,
  and the current music gain is passed to the replacement screen.
- No separate package exists until there are reusable UI classes.
- `screen/StatusLabels` is the graphics-free mapping from a `CombatStatus`, plus an optional
  attacker-owned label, to ordered uppercase status labels and colours. `PlaceholderRenderer`
  stacks them above every actor through the existing world-space name-tag batch and camera
  projection. Shared statuses are read per actor, so a guardian frozen by the player's own reaction
  is labelled as well; only burning arrives through the trait channel because the trait owns its
  stack count. Control effects sort first and the immunity window is included, so negated hits read
  as a mechanic. No new asset is loaded.
- `GameScreen.drawMinimap` marks a living Boss with a red square on both the corner minimap and the
  expanded `[M]` map, using the same world-to-minimap conversion as its obstacle, portal, NPC and
  player markers, drawn before the player so the two never hide each other.

### Save / Game State

- `save.GameSaves` coordinates snapshots through public module APIs on the game-loop thread.
  Player, abilities, GameSession, TravelSystem, EncounterFlow, BossQuests and NPC remain owners
  of gameplay rules. No reflection or LibGDX object-graph serialization is used.
- SaveFile version 1 uses Java properties, format/version markers, duplicate-key rejection
  and a 1 MiB limit. SaveFields and component snapshots validate required/unknown fields,
  finite numbers and cross-module consistency before any live restoration.
- SaveStore separates AUTO and MANUAL paths. Same-directory temporary writes are flushed and
  atomically moved; unsupported atomic replacement or IO failure leaves the previous file intact.
- Safe-state policy requires a living player in Hub/Memorial and idle ability execution.
  UI blockers are checked separately by SaveMenu. Cooldowns can be nonzero and are preserved.
- Loading restores exact location/HP, class/modes/element/cooldowns, bag/chest/worn slots, quest
  and ending state, win counts, pending rewards/replay IDs, portal routes/unlocks and NPC flags.
  All validation precedes live mutation; transient combat is rebuilt and arrival protection
  requires leaving a restored overlapping portal before re-entry. Bonuses are recomputed.
- Startup and loading never write either slot. Manual save only writes MANUAL; successful
  settled Boss returns only write AUTO. Failure and simultaneous player/Boss death do not autosave.
  Loading an older timeline does not merge progress; its next victory may replace a newer AUTO.
- Save management is a nested `SaveMenu` page. Confirmed per-slot deletion passes through
  `GameSaves.deleteSlot` safety checks to synchronized `SaveStore.delete`; it never restores or
  mutates live state. Only the selected enum-bound file is removed, directories are rejected and
  missing files are idempotent. The submenu refreshes summaries after deletion and retains
  pause/input ownership until the player returns to gameplay.
- No arbitrary mid-battle snapshot, automatic startup load or separate achievement subsystem
  is implemented. The menu restart creates fresh live state without deleting save files. See [Save/load](SAVE_LOAD_REPORT.md) for verification.

### Achievement (planned, dropped)

Not implemented; no achievement package or code exists. The bullets below record the
original boundary only.

- Would have owned achievement definitions, progress, and unlock state.
- Will consume stable gameplay events and expose read-only progress to UI.
- Will not duplicate quest progress or combat calculations.
- No package exists until the initial achievement list is agreed.

## 3. Dependency direction

```text
GameScreen -> feature modules (coordination only)
GameScreen -> SaveMenu, GameSaves, InventoryRewards, Equipment, BossQuests
SaveMenu -> GameSaves, HudLayout (input/confirmation/presentation only)
GameSaves -> SaveStore, snapshots, public player/session/travel/quest/encounter/NPC APIs
SaveStore -> SaveSlot, SaveFile (independent paths and atomic disk IO)
Player -> Inventory, Equipment; Equipment -> Inventory, ItemCatalog, EquipmentSlot
Equipment -> capacity observer (Player applies Health.setMaxHp)
MerchantServices -> NPC, Inventory, ItemCatalog, PotionSupply
GearUpgrades -> ItemCatalog, Inventory, Equipment, EquipmentSlot
InventoryRewards -> EncounterFlow, Inventory (capacity-checked one-time transfer)
CombatSystem -> EquipmentModifiers (shared damage calculation)
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
Achievement -> stable gameplay events (planned only; not implemented)
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
| Developer B — Hao Liu (`u7998420`) | `npc`, dialogue, `quest`, achievements, save/load | Read-only gameplay events, inventory reward operations, and module state snapshots |
| Developer C — Jiaqi Chen (`u7851266`) | `inventory`, equipment, crafting, UI, settings/debug UI | Read-only gameplay APIs, world data for the minimap, and combat equipment values |

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

> **Status (2026-09-26).** These tables are the original plan written on 2026-09-04 and are
> kept unchanged except for the added Status column. Actual owners and evidence are in
> Section 7; GitLab issue numbers are given where one exists.

### Developer A — Daoyan Zhu (`u7782042`)

The table below is the original branch-planning breakdown, not a list of missing features.
The current placeholder baseline includes these core systems, finalized first-version skills
and reactions, wall occlusion, death recovery and the task-portal/reward boundary described above.
This integration stage adds regression coverage and hardens lifecycle contracts, not new gameplay.

| Priority | Feature | Suggested branch | Status |
| --- | --- | --- | --- |
| P0 | Player position, WASD movement, and collision checks | `feature/player-movement` | Delivered (#1) |
| P0 | Hub/combat maps, boundaries, and map switching | `feature/map-system` | Delivered (#2) |
| P0 | Portals and fast travel between maps | `feature/portal-fast-travel` | Delivered (#3) |
| P0 | Health, shared damage rules, and attack resolution | `feature/combat-system` | Delivered (#5, #6) |
| P0 | Enemy state, movement/following, and basic AI | `feature/enemy-ai` | Delivered (#7) |
| P1 | Camera following and horizontal/room scrolling | `feature/world-camera` | Delivered (#4); no room snapping |
| P1 | J/K/L/O actions: light attack, special, dash, and ultimate | `feature/player-actions` | Delivered (#8, #19) |
| P1 | Archer, mage, and katana class behaviour | `feature/player-classes` | Delivered (#9–#12) |
| P1 | Boss combat and boss-material drop events | `feature/boss-combat` | Delivered (#13) |
| P2 | Derived attacks and elemental reactions | `feature/advanced-attacks` | Delivered early (#14, #20) |

### Developer B — `u7998420`

| Priority | Feature | Suggested branch | Status |
| --- | --- | --- | --- |
| P0 | NPC identity, interaction range, and interaction flow | `feature/npc-interaction` | Delivered by Jiaqi Chen (#22) |
| P0 | Dialogue data and dialogue progression | `feature/npc-dialogue` | Delivered by Jiaqi Chen (#30); layout by Daoyan Zhu (#25) |
| P0 | Quest acceptance, progress, completion, and rewards | `feature/quest-system` | Delivered by Daoyan Zhu (#18, #21, !23) |
| P1 | Quest manager, priest, and beginner-guide hub flows | `feature/hub-npcs` | Delivered by Daoyan Zhu and Jiaqi Chen (#28, !22) |
| P1 | NPC movement and player-following behaviour | `feature/npc-following` | Dropped |
| P1 | Save/load of player, map, quest, inventory, and achievement state, including the agreed special-save rule | `feature/save-load` | Delivered by Hao Liu (#48), without achievements |
| P1 | Achievement definitions, progress, and unlock events | `feature/achievements` | Dropped |
| P2 | Guided tutorial/onboarding sequence | `feature/tutorial-flow` | Reduced to tutorial NPC and Notice Board (#36) |

### Developer C — `u7851266`

| Priority | Feature | Suggested branch | Status |
| --- | --- | --- | --- |
| P0 | Item model and inventory add/remove/use operations | `feature/inventory-items` | Delivered: prototype Jiaqi Chen, integration Hao Liu (#47) |
| P0 | Weapon/armour equipment and stat values | `feature/equipment-system` | Delivered: prototype Jiaqi Chen (#33), integration Hao Liu (#47) |
| P0 | HUD, menus, dialogue view, and inventory view | `feature/game-ui` | Delivered (#29–#32, #47) |
| P1 | Four armour slots and two-/four-piece set bonuses | `feature/armour-set-bonuses` | Delivered by Hao Liu (#47) |
| P1 | Boss-material crafting, weapon forging, and enchanting | `feature/crafting-enchanting` | Reduced to merchant gear upgrades (`GearUpgrades`, !42) |
| P1 | Minimap presentation using read-only world data | `feature/minimap-ui` | Delivered by Jiaqi Chen (#34); Boss marker #45 |
| P1 | Health-bar and damage-number display settings | `feature/combat-display-settings` | Health bar delivered (#31, #32); damage-number settings not implemented |
| P1 | Automated gameplay smoke tester for the main loop | `feature/gameplay-smoke-tester` | Deferred to M4 (#40) |
| P2 | Debug-mode controls and diagnostic overlay | `feature/debug-mode-ui` | Partial: coordinate HUD (#29) |

### Shared integration

- Assign only one developer to each `GameScreen` integration change.
- Integrate completed P0 domain features before beginning P1 features.
- Use stable events/read-only interfaces for quest, achievement, save, minimap,
  and UI integration instead of depending on private fields.

## 7. Contribution evidence

### Current update: 2026-09-26

Read from `git shortlog -sne --no-merges` and `git log` at `ab76422`. Three people have
committed; `u8350466` has no commits and is not allocated work.

| Contributor | Non-merge commits | Main areas | GitLab issues authored |
| --- | --- | --- | --- |
| Daoyan Zhu (`u7782042`; commits appear under both the `Daoyan Zhu` and `u7782042` names) | 62 | Skeleton/build/CI, `player`, `combat`, `world`, `enemy`, `quest`, `audio`, most of `screen`, tests, docs | #1–#21, #24–#28, #37–#46, #49, #50 |
| Jiaqi Chen (`u7851266`) | 15 | Hub NPCs and dialogue box, coordinate HUD, health bar, minimap, inventory/weapon prototypes, storage chest, Notice Board (`screen`, `npc`, `inventory`) | #22, #29–#36 |
| Hao Liu (`u7998420`) | 8 | Inventory/equipment integration, `save`, SaveMenu pause menu and slot management, merchant services, potion supply, gear upgrades, health capacity | #47, #48 |

Hao Liu's commits: `21de289` (inventory/equipment, !38), `82f6a45` and `c8d14bc` (save/load,
!39), `534cb93` (save UI and docs, !41), `ec0a8fb`, `a4c7e9f`, `bbe814b` (pause menu, save
management, merchant services and equipment progression, !42) and `aa2a8d5` (root README).
Jiaqi Chen's work was committed directly to `main` on 2026-09-15 – 09-18 rather than through MRs.
The P potion shortcut is `721f9e6` (#50); Wind fixes are `36ce30d` (#49). Using another
module's API for integration does not imply authorship of that module. Commit counts are not a
measure of effort; non-code contributions are not visible in `git log`.

### Historical allocation snapshot (before inventory/save integration) — superseded

> Superseded by the 2026-09-26 update above. Retained unchanged as evidence of the repository
> state at that time (before Hao Liu's first commit on 2026-09-23). Statements such as
> "save/load were not started" or "equipment ... not built" were true then and are no longer current.

Sections 5 and 6 above record the **planned** three-developer split, written
before implementation started. This section records the **actual** split, read
directly from `git shortlog` / `git log --pretty=format:'%an <%ae>' -- <path>`
against the repository history available at that earlier update. It is descriptive, not
a revision of the plan: the plan is kept above so the gap is visible.

Git history contains commits from exactly two identities that are the same
person under two `git config user.name` values (`Daoyan Zhu` and `u7782042`,
both `u7782042@anu.edu.au`), plus one further person (`Jiaqi Chen`,
`u7851266@anu.edu.au`). Neither `u7998420` (planned Developer B) nor
`u8350466` has any commit in this repository's history to date; per Section 5,
`u8350466` was already excluded from the active allocation, and `u7998420`'s
planned scope was in practice absorbed by Daoyan Zhu and Jiaqi Chen below.

| Package / area | Daoyan Zhu (`u7782042`) commits | Jiaqi Chen (`u7851266`) commits |
| --- | --- | --- |
| `player` | 16 | 0 |
| `combat` | 11 | 0 |
| `world` | 15 | 0 |
| `enemy` | 12 | 0 |
| `npc` | 6 | 4 |
| `inventory` | 2 | 2 |
| `quest` | 3 | 0 |
| `audio` | 1 | 0 |
| `screen` | 28 | 15 |
| Root gameplay classes (`GameSession`, `TravelSystem`, `EncounterFlow`, `MainGame`, `ActiveEncounter`, `BattleResult`) | 7 | 0 |
| `core/src/test` | 41 | 0 |
| `lwjgl3` (source and tests) | 10 | 0 |
| `docs/` | 34 | 0 |

Reading this against the planned split:

- **Developer A's planned scope (`player`, `combat`, `world`, `enemy`, portals)**
  matches what actually happened: every commit in these packages is from
  Daoyan Zhu.
- **Developer B's planned scope (`npc`, dialogue, `quest`, achievements,
  save/load)** was never picked up by a distinct `u7998420` identity.
  Achievements and save/load were not started. `npc` and `quest` were built by
  Daoyan Zhu, with `npc/NPC.java` (base interaction/range) also carrying 4
  commits from Jiaqi Chen. Dialogue rendering (`DialogueLayout`,
  `CreditsDialogue`) was built by Daoyan Zhu as part of `screen`, not as a
  separate developer's package.
- **Developer C's planned scope (`inventory`, equipment, crafting, UI,
  settings/debug UI)** was partly realized: `inventory/Inventory.java` has
  contributions from both Daoyan Zhu and Jiaqi Chen, and Jiaqi Chen also
  contributed to the shared `screen` files (`GameConfig`, `GameScreen`,
  `PlaceholderRenderer`). Equipment, crafting, a dedicated UI package, and the
  gameplay smoke tester listed under Developer C in Section 6 were not built.
- Testing (`core/src/test`), build/CI-facing setup, and documentation were
  written entirely by Daoyan Zhu; no test or docs commit is attributed to
  Jiaqi Chen in this history.

In short: the project was implemented mainly by two contributors instead of
three, with Daoyan Zhu covering nearly all of Developer A's and Developer B's
planned scope plus testing/build/docs, and Jiaqi Chen contributing to `npc`,
`inventory`, and shared `screen` files that overlap Developer B's and
Developer C's planned scope. This is stated from the repository's own commit
log, not inferred about anyone's actual effort outside of Git (for example,
non-code contributions are not visible in `git log`).

## 8. Shared Core

- `core/src/main/java/au/edu/anu/comp2120/MainGame.java`
- `core/src/main/java/au/edu/anu/comp2120/screen/GameScreen.java`
- root and module `build.gradle` files
- `settings.gradle`
- `gradle.properties`

Changes to these files must be minimal and state the integration reason in the
commit message.

## 9. Git collaboration rules

1. Branch from the same up-to-date integration point and keep commits focused.
2. Do not mix package moves or unrelated refactors into a feature commit.
3. Agree on changes to public interfaces or Shared Core before implementation.
4. Only one branch performs a given `GameScreen` integration change.
5. Merge feature code before its small Shared Core integration change.
6. Never commit `.gradle/`, `build/`, IDE files, or local environment files.

## Decisions still requiring the team

- Developer B is Hao Liu (`u7998420`); Developer C is Jiaqi Chen (`u7851266`).
  Keep future task allocation aligned with the current contribution evidence in Section 7.
- Current maps use Java rectangle data and named bottom-left spawn anchors in
  world units. Agree on any future external file format and portal triggers when
  those features are implemented.
- Extend the existing BossQuests and InventoryRewards boundaries for future quest/reward work;
  do not introduce a competing progress store or reward claim path.
- First-version class behaviours and reaction rules are now fixed and implemented. Agree on
  balance refinements and future art/presentation mapping separately; those must preserve the
  shared damage, world-collision, Hub-only selection and one-generation reaction contracts.
- Define migration before changing the implemented version-1 save schema. Per-slot deletion is
  now implemented in the SaveMenu management page; decide separately whether to add more
  profiles or new safe-save locations. Restart game intentionally preserves both files.
- Agree on final guardian balance. Scripted fights show mobile classes winning every arena, while
  a deliberately stationary caster loses to the ice and wind guardians; measurements and the
  single-constant levers are recorded in [Boss trait report](BOSS_TRAIT_REPORT.md).
- Decide whether burning deserves its own status icon; the overlay currently uses text only and
  adds no asset.
- ~~Decide which P2 features will be dropped if the P0/P1 schedule slips.~~ Resolved: see the
  Status column in Section 6 (achievements and NPC following dropped; crafting and the tutorial
  flow reduced). Remaining open: the gameplay smoke tester (#40) and Docker (#42).


Save management is a nested `SaveMenu` page. Confirmed per-slot deletion passes through
`GameSaves` safety checks to synchronized `SaveStore.delete`; it never restores or mutates
live state. Only the selected enum-bound file is removed, directories are rejected, and
missing files are idempotent. The submenu refreshes summaries after deletion and retains
pause/input ownership until the player returns to gameplay.

### Runtime debug console

F10 or backtick opens the in-game command console and temporarily owns gameplay input. Enter executes a command; Esc, F10 or backtick closes it. Opening and closing consume their frames, and the prior input processor is restored on close. `DebugOptions` belongs to Player and is excluded from snapshots. Commands `god`, `freeupgrade`, `oneshot`, and `nocd` toggle their states (optional explicit `on/off`), plus `help`, `status`, and `debug off`.

God mode rejects player HP loss; free upgrades retain item ownership/capacity and +2 limits. One-shot applies only after normal enemy hit eligibility, invulnerability/interception and positive damage calculation. It delivers lethal damage through the existing receiver and settlement pipeline. No-CD clears J/K/L/O cooldown timers without clearing active actions, dash, projectiles or launch guards; normal cooldowns resume for future casts when disabled. Once any cheat is enabled, a sticky per-game flag blocks both manual and automatic writes, including after loading or disabling cheats. Creating a fresh game/player clears debug state; existing saves are neither modified nor deleted by the console.

Debug console shortcuts: type `god`, `oneshot`, `freeupgrade` or `nocd` and press Enter to toggle that feature. Command rows are green when enabled and white when disabled, with ON/OFF text. Explicit `on/off` syntax remains supported. Disabling cheats does not remove the current game’s save protection.

`screen.DebugConsole` separates bounded text editing, processor ownership, command submission and
colour markup from GameScreen rendering, allowing the production input path to run in headless
tests. See [current coverage and manual checks](DEBUG_TRAINING_UI_REPORT.md).

NPC modal handlers synchronize map changes through `CombatDemo.synchronizePausedMap()` without
advancing combat time. Cooldown restoration validates snapshot values first, then applies the active
no-CD override to live timers; persisted bytes and normal-game restoration remain unchanged.
