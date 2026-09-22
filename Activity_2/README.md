# COMP2120 Group Game

Java and LibGDX foundation for COMP2120 Assignment 3. The current application
demonstrates a Hub, Memorial Hall, a separate Training Ground and five Boss arenas, named spawn points, eight-direction
player movement, static rectangle collision, automatic portals, and unlocked fast
travel, and a map-bounded follow camera with pixel-art presentation. Player and
enemy models share validated integer health and one attack pipeline, independent
of the view. Each built-in Battle Map contains one two-phase
Boss and no ordinary enemies. Player defeat automatically returns to Hub, restores full
HP and clears the encounter while retaining player identity and selected class/ultimate configuration.
The shared combat pipeline supports the first-version ordered elemental reactions, finite
reaction statuses/ground fields and bounded one-generation derived attacks. Mage bolts use its selected element (FIRE initially).
The imported pixel-art pack supplies characters, arenas, portals, skills and icons; streamed background music follows the map and guardian progress.
Hub keeps its procedural background at 1080 x 400. Battle/Training maps are 1600 x 1200; Training randomly selects an elemental
background and a stationary Boss appearance per visit, preserving damage scoring and non-hostile rules.
See [Art integration](docs/ART_INTEGRATION.md) for resource layout, verification and the five Boss
projectile images awaiting actual Boss projectile mechanics. Historical placeholder-only descriptions
in the development notes below are superseded by that document; collision remains geometric.

## Current controls: one Hub portal, practice, battle or Memorial Hall

Background music loops automatically: Hub/Memorial Hall use `audio/hub_theme.wav` before all five
guardian stones are collected and `audio/victory_theme.wav` afterwards (no ending conversation required).
Training and Battle always use `audio/battle_theme.wav`. Moving between maps that share a song does
not stop, reload or restart it, including Hub/Hall travel. A different song starts from its beginning.
Music pauses/resumes with the application and is released when the game screen closes. Default volume
is 50%, centralized in `BackgroundMusic.VOLUME`; no system-volume changes or sound effects are added.

Hub has one portal and a nearby **Portal Guide**. Approach the guide, press **E**,
choose **Training Ground**, **Battle Map** or **Memorial Hall** with **Up/Down**, then press **E** to confirm
(**Escape** cancels). Confirmation configures the portal; walk into it to travel.
The portal starts closed. **Battle requires an accepted Boss quest**; the Portal Guide cannot
create one. Training and Memorial Hall remain available without a quest.

NPC dialogue panels size themselves from the actual wrapped title, body and control-hint text.
They shrink with small windows, reserve separate control space and cap normal font/panel growth
at the 960 x 540 reference size. Exceptionally long pages reduce font size to remain inside the window.

Talk to the **Quest Keeper** at (347,240): **E** opens the quest list, **Up/Down** highlights
one of the five guardians, **E** accepts it, and **Escape** closes the list. Then choose Battle
at the Portal Guide and walk into the single Hub portal. Its destination is the accepted
guardian's arena, not a default map. One unfinished task can be replaced with another in Hub.
**Accepting or replacing a task leaves the portal closed**, even if a previous Training/Battle
route was open. Reading the Portal Guide dialogue alone does not open it: select Battle and
press **E** to confirm. Re-accepting the same task also requires confirmation again.

- Provisional targets: Fire / Water / Lightning / Ice / Wind Sentinel, each with a distinct arena.
- A successful return records that target and shows a **green check before its name** next time
  the quest list is opened. Completed targets cannot be accepted again this session.
- Failure retains the task for retry with a fresh Boss; practice visits also retain the task.
- Memorial Hall fits one camera view: **640 x 360 world units**, displayed at **960 x 540** in the
  default window. Its lower-left portal returns to Hub; Old Sage stands on the left and a five-slot
  magic circle occupies the right. Each distinct guardian victory lights its matching elemental stone.
  The Hall omits the minimap so the stones stay visible; other maps retain their existing HUD.
- Defeating all **five distinct** targets unlocks, but does not automatically play, the ending.
  Visit Memorial Hall and speak to **Old Sage [E]** to start the one-time dialogue; advance its
  three lines with **E**. Repeated conversations do not replay it. Stones reflect quest progress,
  not inventory items, and remain lit across map visits within the session.
- All five identities share the Sentinel's 400 HP, two-phase pattern geometry and material tuning.
  Each now also owns one signature mechanic and its own movement speed, so the fights differ in
  play without a separate Boss class: see [Guardian traits](#guardian-traits) below and the
  [Boss trait report](docs/BOSS_TRAIT_REPORT.md). Final names, per-guardian art/audio cues, the
  final ending script and final balance remain future content work. Task progress and ending
  playback are session-local, without save/load.

- Training has one stationary, non-attacking target, not a Boss. J/K/L/O use the shared
  abilities/combat pipeline. The scoreboard shows total delivered damage, last damage and
  positive-damage hit count, including reaction/field ticks. The target replenishes its HP
  after each hit so it remains available; it never grants rewards or a BattleResult.
- **R** resets practice: score, attacks/effects, ability activity/cooldowns and player HP.
  Talk to the **Return to Hub** NPC near the Training entrance: **E** opens confirmation,
  **E** again returns, **Escape** cancels. Leaving and re-entering creates a fresh target/score.
- Every admitted Battle arena contains one real Boss and retains the success/failure flow below.
- F1/F2/F3 select class and F5 selects its O mode **in Hub only**, including before practice.

The **Class Master** in Hub at (320,130) provides the same choices without debug keys:
approach and press **E**, select a profession or its **O mode A/B** with **Up/Down**, then
press **E** to apply. The menu stays open so a class and its ultimate can be selected in turn;
**Escape** closes it. J/K/L remain class-defined. Each class remembers its own O choice.
Selections preserve HP, position and cooldowns and are rejected while an ability is active.

The small `PortalGuide` adapter uses the existing gameplay boundary; it does not implement
Quest state or inventory rewards. Choosing Training only reroutes the shared portal and preserves
the accepted target. `BossQuests` now owns this session's NPC acceptance and completion tracking.
The lower-level external adapter boundary remains
`gameScreen.getEncounterFlow().prepare(new ActiveEncounter(id, mapId, spawnId, portalId))`
on the game-loop thread after acceptance in Hub, using `TravelCatalog.QUEST_PORTAL_ID`.
`activate` remains the explicit open-route operation used by Portal Guide after confirmation.
Future Quest integration should reuse/replace the one quest owner, not create competing progress stores.

- The configured portal admits one fresh Boss; there are no ordinary enemies.
- Player death: FAILURE, automatic Hub return/full HP, same permission available for retry.
- Boss death: SUCCESS, automatic Hub return/full HP, task portal closed until a new acceptance.
- Results are read through `getLastResult()`. Reward data survives cleanup in
  `getPendingRewards()` and is transferred once by `claimReward(result)`.
- Player identity/class/O configuration persist. F1/F2/F3/F4 configuration changes are
  Hub-only in production; battle R reset is disabled.
- The old **U/1/2/3 debug travel keys are removed**. Entry uses NPC selection and the portal;
  practice return uses its exit NPC, while Battle returns are outcome-driven. The low-level
  fast-travel API remains available to adapters and contract tests, not keyboard shortcuts.

See [the integration contract and test report](docs/QUEST_PORTAL_FLOW_REPORT.md).
Historical feature/demo descriptions below that switch abilities during battle,
manually return through battle portals, or leave Boss loot on the map apply
only to the isolated unrestricted `CombatDemo(session)` harness, not the production screen.

## Attack wall obstruction

All gameplay attacks now respect static walls. Projectiles retain their existing swept
collision and wall-wins-equal-time rule. Fixed melee, spells, Boss patterns and reaction
areas require line of sight before damage or elemental settlement:

- Forward melee: cast-time attacker centre to current target hurtbox centre.
- Fixed AoE/custom area: fixed effect centre to target centre.
- Reaction child: its own captured centre to target centre.

Crossing a solid rectangle's interior blocks the hit. Merely touching its edge/corner does
not; a centre inside a wall or outside the map cannot hit. Moving the caster does not move
an already-cast area/origin. Yellow debug rectangles may extend through walls visually,
but occluded targets receive no damage, element attachment or reaction.

Concrete Arrow Rain and Mage magic fields now submit finite fixed-centre waves through this gate.
Their areas capture the Boss position at cast time, not the later caster or Boss position.
Details and validation: [wall occlusion report](docs/COMBAT_OCCLUSION_REPORT.md).

## Player failure and retry

Battle HP reaching zero means challenge **FAILURE**. After combat settlement, the existing
coordinator returns the same Player to Hub's default spawn (64,64), clears old attacks,
projectiles, elemental/derived effects and ability activity, then calls Health.restoreFull().
No death callback switches maps. The remainder of the death frame is consumed; R cannot
revive a dead player in Battle. Class configuration and remaining slot cooldowns are preserved.
Re-entering the admitted battle map creates a new Boss at full HP, phase 1 and fresh AI/cooldowns.

Quick check after external acceptance: select F1/F2/F3 in Hub, enter the enabled task portal,
approach the large Boss and let it attack. At zero HP you should immediately arrive alive in
Hub, with no old attacks following you. Use the same task portal to verify a fresh Boss.
TravelSystem's existing arrival guard prevents immediate automatic return through a portal.

Quest/NPC acceptance and quest status remain outside gameplay; success-return is now integrated.
Failure does not mark a Quest failed/completed or remove it; its external owner can keep it ACTIVE.
Each Player retains a separate A/B ultimate selection for every class. F5 and the gameplay
selection API work only in Hub; recovery cancels activity, not these choices. See
[the defeat implementation report](docs/PLAYER_DEFEAT_REPORT.md) for tests and limitations.

## Author

- Daoyan Zhu (`u7782042`)

## Requirements

- JDK 25
- No system Gradle installation is required; use the included Gradle wrapper.

The project is compiled with JDK 25 while targeting Java 17 bytecode so that the
game remains usable on common Java runtimes.

Gradle 9.7.1 runs on JDK 25. The desktop module constrains LWJGL to 3.4.1 because
LibGDX 1.14.2 otherwise selects LWJGL 3.3.3, which is not suitable for running
on JDK 25. Keep Gradle at 9.1 or newer and LWJGL at 3.4 or newer when updating
dependencies.

## Project modules

- `core`: platform-independent game code and unit tests.
- `lwjgl3`: desktop launcher and LWJGL3 runtime dependencies.
- `assets`: textures, maps, audio, fonts, and UI resources.

The Stage 2 package boundaries and collaboration rules are documented in
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Run the desktop game

On macOS or Linux:

```bash
./gradlew lwjgl3:run
```

On Windows:

```powershell
gradlew.bat lwjgl3:run
```

The desktop module includes `assets/` as its main runtime resources. Gradle runs, IDEA's
launcher classpath and distribution JARs all contain paths such as `art/hub/props/forge.png`;
LibGDX internal files can load them without an `assets/` working-directory override.
After changing Gradle configuration, use **Reload All Gradle Projects** in IDEA before running
`Lwjgl3Launcher.main()` again. Direct non-Gradle macOS run configurations still need the VM option
`-XstartOnFirstThread`; Gradle tasks and generated distribution scripts configure it automatically.

Build a runnable distribution with `./gradlew :lwjgl3:installDist`, then run
`lwjgl3/build/install/lwjgl3/bin/lwjgl3` (or `lwjgl3.bat` on Windows). Keep its `lib/` directory
with the script; this is a distribution with dependencies, not a standalone `java -jar` archive.
The application-default JVM options match the build host, so generate macOS distributions on macOS.

## Build and test

```bash
./gradlew clean build
./gradlew test
./gradlew core:javadoc
./gradlew javadocAudit
```

Build output is written under each module's `build/` directory and is ignored by
Git. `javadocAudit` checks production and test Java in both modules, including private
members, with full doclint and warnings treated as errors. See
[the integration report](docs/INTEGRATION_REPORT.md) for baseline coverage and remaining limitations.

## Map and movement demonstration

The game starts in the Hub. Ask the Portal Guide to select a destination, then walk
into the purple portal. There are no number-key map switches or keyboard unlocks.
The window title shows the active map and travel feedback. Visiting through a portal
does not automatically unlock fast travel; future quest/discovery code can call
`unlockDestination(id)`. Portal enabled state and fast-travel unlock state are
independent. Closing/restarting the game restores the initial state.

Hub has a blue-green floor and Battle Maps have a red-brown floor. Grey rectangles
are obstacles when configured (built-in test walls have been removed), a gold circle marks
only the default spawn anchor, and the green rectangle is the
player. Bright purple rectangles are enabled portals; dark purple rectangles are
the closed portal. A fixed-size camera view scrolls over maps larger than the
view, instead of fitting the entire map into the window.

| Portal | Source trigger (x, y, width, height) | Destination |
| --- | --- | --- |
| hub-training (stable ID for the single portal) | Hub (160, 64, 48, 64) | NPC-selected Training or Crossroads `entry` |

- Hold W/A/S/D to move up/left/down/right. Releasing keys stops movement.
- Movement supports eight directions. Opposite keys cancel on each axis. W+D
  moves diagonally up-right; diagonal input is normalized so its total speed
  equals straight movement. When one axis is blocked, the other can slide along
  the wall at its original component speed.
- The green rectangle is the player; grey rectangles are solid obstacles.
- The full player rectangle must stay inside the world and outside obstacles.
  Touching edges is permitted. Swept collision stops at the first obstacle even
  when a requested move would otherwise cross an entire thin wall. Collision
  resolves X first and then Y from the resolved position.
- Coordinates use a bottom-left origin, positive x right, and positive y up.
  World coordinates are independent of screen pixels; resizing letterboxes the
  view without changing map bounds, position, collision size, or movement speed.
- Tune player speed/size, camera world-view dimensions, and the frame-time limit
  in `screen/GameConfig.java`.
  Define map IDs, names, types, dimensions, walls, and spawns in
  `world/MapCatalog.java`. These are ordinary Java data definitions; no external
  JSON loader or new dependencies have been introduced.
- Speed is measured in world units/second. The screen caps simulation time per
  frame at 0.1 seconds and discards the first update after startup/resume, so a
  stall does not cause a large jump. Model tests can exercise larger deltas.

To check manually, try WASD, W+D, opposing keys and the map's outer boundaries,
then resize the window. Pressing U/1/2/3 must have no travel or unlock effect.
Choose Training at the Portal Guide and enter the portal to arrive at (80,80).
Stay still: arrival must not return you automatically. Talk to the Return to Hub
NPC and confirm E to leave; Escape cancels. Returning clears the practice scope.

## Camera behaviour and verification

The persistent HP, position, minimap and weapon HUD scales down with the smaller window axis.
Its maximum size is the existing **960 x 540** layout: larger windows move the anchored panels
but do not enlarge them. Configuration is centralized in `GameConfig.HUD_*`:

- Width below **640** or height below **360**: hide position, minimap and weapon; keep HP.
- Width below **320** or height below **180**: hide HP as well.
- At the exact thresholds the relevant elements stay visible; enlarging restores them.
- Shrinking below the minimap threshold closes an expanded M map and releases its movement lock.
  M cannot open an invisible map. The expanded map also has a capped reference size.

These thresholds affect the four persistent HUD elements, not NPC dialogues or inventory panels.

- `CameraFollow.viewFor(subjectBounds, mapBounds, viewWidth, viewHeight)` computes
  the visible world rectangle using only immutable geometry. `PlaceholderRenderer`
  applies its centre to the LibGDX camera before drawing. Player and world rules
  have no camera dependency.
- The default view is 640 x 360 world units (`GameConfig.CAMERA_VIEW_WIDTH` and
  `CAMERA_VIEW_HEIGHT`). It follows the player's rectangle centre directly on both
  axes, providing continuous horizontal/vertical scrolling. There is no room
  snapping, interpolation, camera shake, zoom animation, or cutscene state.
- For a map axis larger than the view, the view origin is clamped between the
  map's minimum coordinate and its maximum coordinate minus the visible extent.
  The camera stops at the edge while the player continues toward the map boundary.
- For a map axis smaller than or equal to the view, that axis stays centred on the
  map, independent of player movement. The remaining area is solid background with
  equal margins. A negative visible-rectangle origin in this case is deliberate;
  it does not create negative gameplay boundaries or move the player's coordinates.
- The `FitViewport` preserves the fixed world extent and aspect ratio on resize,
  adding letterboxing/pillarboxing as needed. Resize does not recenter the camera;
  zero-sized/minimized windows retain the last valid viewport. Different window
  sizes do not reveal extra map area or change follow/clamp calculations.
- After portal/fast travel, the next draw uses the new map and player's spawn
  directly. No previous-map camera position or clamp interval is retained.

To check manually:

1. Launch with `./gradlew lwjgl3:run`, choose Training at the Portal Guide and enter.
2. Walk right from its entry along the unobstructed lower part of the map. The
   view begins scrolling once the player reaches its centre, then stops at the
   right boundary. Reverse direction and verify the left boundary similarly.
3. Walk up near the left side to check vertical scrolling and the top boundary;
   reverse toward the bottom. Practice has no hostile Boss or automatic return portal.
4. Resize the window wide, tall, and small while stationary and while moving.
   Shapes must retain their proportions and the same world region must remain
   visible. Minimize/restore and check that the camera still follows normally.
5. Use the Hub portal and Training exit NPC and check that the new spawn is visible immediately.
6. Small-map centring is covered by JUnit. For an optional visual check, temporarily
   set the configured view to 1280 x 720 and relaunch: Hub should remain centred
   with equal background margins even while the player moves. Restore 640 x 360
   afterward. Existing demo maps were not changed merely to test this case.

## Map API and extension

- `GameMap` is immutable data plus the existing collision implementation. Its
  walkable space is the map rectangle minus static obstacles. IDs are unique
  within a `GameSession`; names and `MapType` describe the map's role.
- Every map requires named `SpawnPoint` anchors and an existing default spawn.
  Coordinates specify the entering body's bottom-left corner. Map construction
  checks the anchor; entry checks that the entire player fits at that point.
- `GameSession` owns the current map and one persistent player. Use
  `switchMap(mapId)` for default entry or `switchMap(mapId, spawnId)` for a named
  spawn. Re-entering the same map also resets the player to the requested spawn.
- Unknown IDs and blocked/oversized entries throw `IllegalArgumentException`;
  null arguments throw `NullPointerException`. A failed transition preserves the
  old map, player position, and collision environment. Size and speed are retained
  across successful transitions. Coordinate session changes on the game-loop thread.
- Add another `GameMap` definition to `MapCatalog.createMaps()` to register a new
  level. No new subclass or movement/collision implementation is needed. A future
  caller can select it by ID through `GameSession`; add a debug key separately if
  it should be reachable through this temporary demo UI.
- Colours and spawn markers belong exclusively to `PlaceholderRenderer`. Map
  data has no artwork paths, texture objects, or other rendering dependencies.

## Portal and fast-travel API

- `world/TravelCatalog.java` defines portal routes/regions and initial destination
  unlock states. `Portal` and `FastTravelDestination` are immutable data snapshots.
- `TravelSystem` receives the existing `GameSession` and validates duplicate IDs,
  map/spawn references, and full trigger geometry during registration. It never
  reimplements player movement or map switching. Source trigger rectangles must
  be fully walkable; portals are non-solid trigger regions, not obstacles.
- Call `updatePortals()` once after movement. Full-body interior overlap with an
  enabled current-map portal triggers it; edge contact alone does not. Overlapping
  enabled portals choose the first registered match, with at most one transition
  per call. Disabled portals never execute a transition.
- On startup inside a portal, after any successful travel, or after an external
  `GameSession.switchMap`, the body must be observed outside all portal regions
  before another automatic entry is permitted. This includes disabled regions
  and same-map re-entry. No timer, animation, or frame-rate-based cooldown is used.
- `fastTravel(destinationId)` requires a known unlocked destination. Unknown IDs
  or invalid target placement throw `IllegalArgumentException`; locked travel
  throws `IllegalStateException`; null IDs throw `NullPointerException`.
  GameSession validates the full body at execution, preserving old map/player
  state on failure. Failed automatic portal attempts wait for exit before retry.
- `unlockDestination(id)` changes in-memory state; repeated calls are harmless.
  `setPortalEnabled(id, enabled)` changes activation. Getters return immutable
  snapshots, so presentation cannot bypass these state operations.
- Coordinate travel on the game-loop thread. Use GameSession for every map/spawn
  transition so its transition stamp can protect arrivals. Direct calls to
  `Player.enterArea` are a low-level operation and bypass that session coordination.

## Shared health API

- `combat/Health` owns immutable positive `maxHp` and private `currentHp` in
  `[0, maxHp]`. Both are integers, matching the existing `Damageable` contract;
  fractional values, NaN, and infinity are not accepted by the Java API.
- `new Health(maxHp)` starts full and alive. Zero/negative capacity throws
  `IllegalArgumentException`; `Integer.MAX_VALUE` is supported.
- `takeDamage(amount)` accepts non-negative integers and stops at zero. Zero is
  a no-op, including after death. `isAlive()` and `isDead()` derive from current
  HP, so a separate death flag cannot become inconsistent.
- `heal(amount)` returns the actual restored HP, capped at the missing amount.
  It does not revive dead entities. Negative damage/healing throws
  `IllegalArgumentException` before mutation, even when full or dead. Extreme
  positive amounts cannot overflow the integer HP range.
- `restoreFull()` explicitly restores full HP and can revive. It does not change
  position, map, movement speed, or any other entity state. CombatDemo now calls it
  only after failure return and encounter cleanup; Health still owns no map policy.
- `Player` and the existing minimal `Enemy` implement `Damageable` by delegation.
  Each owns a separate `Health`, exposed by `getHealth()` for queries and
  validated healing/restoration. Existing constructors remain valid and use
  each entity's `DEFAULT_MAX_HP` (currently 100). Custom capacities use
  `new Player(bounds, speed, area, maxHp)` or `new Enemy(maxHp)`.
- Movement and map/portal/fast-travel transitions preserve HP. The health model
  does not decide whether dead players may move/travel. The common ability controller
  blocks dead-player movement and abilities; the screen processes battle failure before debug travel.
  The combat pipeline rejects attacks
  from dead sources; health itself owns no input, AI, effects, or respawn workflow.
- Use the model on the game-loop thread. It does not depend on LibGDX or load assets.

For example, this helper demonstrates the API with an existing player. The HP
comments assume a fresh default player; call it only for debugging because it
intentionally damages and restores that player.

```java
import au.edu.anu.comp2120.player.Player;

/**
 * Demonstrates health operations without rendering or assets.
 *
 * @author Daoyan Zhu (u7782042)
 */
final class HealthExample {
    /** Prevents construction of this example utility. */
    private HealthExample() {
    }

    /**
     * Exercises damage, healing, death, and explicit restoration.
     *
     * @param player existing player to modify for demonstration
     * @throws NullPointerException if player is null
     */
    static void demonstrate(Player player) {
        player.takeDamage(25);                // Default player: 100 -> 75 HP
        player.getHealth().heal(10);           // Returns 10; now 85 HP
        player.takeDamage(Integer.MAX_VALUE); // Zero HP; isAlive() is false
        player.getHealth().heal(10);           // Returns 0; remains dead
        player.getHealth().restoreFull();      // Full HP; position unchanged
    }
}
```

Run the graphics-independent checks with:

```bash
./gradlew core:test --tests '*HealthTest'
```

`HealthTest` covers model boundaries and `EntityHealthTest` covers delegation,
independent ownership, movement, and successful/failed map transitions. The combat
demonstration below exercises this same model rather than a separate debug HP counter.

## Combat core and placeholder verification

The common pipeline is:

```text
AttackSpec + registered source + world hitbox
  -> active Attack invocation
  -> self/faction/alive/already-hit filtering
  -> positive-area hitbox/hurtbox overlap
  -> optional centralized element reaction lookup
  -> shared base-plus-reaction damage calculation
  -> Damageable.takeDamage
  -> existing Health clamping and derived death state
```

- `Combatant` adapts an existing `Damageable`, a `PLAYER`/`ENEMY` faction, and an
  independent world-space `CollisionBox` hurtbox. It adds no second health state.
  Maintain one stable adapter per entity in one arena; `register` rejects duplicate
  receivers, even through another adapter. Dead/removed actors cannot be valid targets.
- `AttackSpec(type, damage, activeSeconds, cooldownSeconds)` is reusable data.
  This compatibility constructor uses NONE; the five-argument form also accepts ElementType.
  Type is a label, not a class-specific formula. Damage is non-negative integer;
  duration must be positive and finite; cooldown must be non-negative and finite.
- `CombatSystem.tryAttack(source, spec, hitbox)` returns an `Optional<Attack>`.
  Dead sources/cooling-down sources return empty; null arguments and unregistered
  sources fail explicitly. Validation happens before consuming cooldown.
- Each accepted invocation gets its own ID, fixed-area or moving-projectile hitbox, remaining
  duration, and identity-based hit ledger. The same type can be reused by any actor.
  One invocation hits each hostile receiver at most once, even across exit/re-entry,
  healing, revival, or re-registration of the same receiver. Multiple enemies can
  each receive one hit; a new invocation can hit them again.
- `update(delta)` samples currently active attacks against current hurtboxes **before**
  advancing duration/cooldown by delta. Creation is included; expiry is excluded.
  Zero-time samples are safe. A large finite delta samples once then expires the attack;
  fixed-area hitboxes are not swept. Projectiles use continuous path checks described below.
  Negative/non-finite delta is rejected
  before mutation. Registration order and attack creation order make resolution deterministic;
  if an earlier strike kills an attacker, its later queued strike is cancelled.
- Damage calculation adds any configured reaction bonus to raw damage with integer saturation.
  Health performs lethal clamping. Armour, critical hits and class-specific modifiers are absent.
  All future damage formulas should extend the shared calculation boundary.
- Cooldown is per registered source across attack types, measured from creation.
  A zero cooldown intentionally allows distinct invocations; it does not remove the
  once-per-target rule. PlayerAbilities owns independent slot cooldowns and submits
  attacks with zero source-level delay; EnemyAI still uses the source-level delay.
  `clear()` cancels old attacks and removes registrations/cooldowns
  without healing actors. `CombatDemo` uses the existing session transition stamp for
  every map change, including same-map re-entry, portals, and fast travel.
- The owner updates hurtboxes after movement, then submits attacks, then calls `update`
  once per simulation step. Use one game-loop thread; custom damage receivers must not
  call back into/mutate the combat system while damage is being delivered.

Placeholder controls (current first-version skills; numeric tuning is provisional):

| Key | Debug action |
| --- | --- |
| J | Magic bolt: 25 damage, 400 units/s, 360 range, 0.25 s launch, 0.5 s cooldown |
| K | Mage: cycle FIRE → WATER → LIGHTNING → ICE → WIND → FIRE; no attack |
| L | Dash: 600 units/s for 0.2 s (120 units unobstructed), 1 s cooldown |
| O | Mage: selected fixed-location casting field or rapid double-projectile buff |
| F1 / F2 / F3 | Select Archer / Mage / Katana when no player attack or dash is active |
| F4 | Hub-only outgoing debug element configuration; Mage J uses its own K-selected element |
| F5 | Toggle current class's O mode A/B in Hub only; active skills reject changes |
| R | Reset practice/Hub HP and abilities; ignored in production Battle encounters |

1. Run `./gradlew lwjgl3:run`, select Battle at the Portal Guide, then enter the portal.
   One large red Boss appears at a validated placement. Training has a stationary dummy instead.
2. The cyan outlines are hurtboxes. The player's is inset from its green body,
   demonstrating independence from visual size. Face an enemy with WASD and press J:
   a small yellow magic bolt travels until impact, 360 units, or 1.5 seconds.
   The first valid enemy it hits loses 25 HP; the bolt then disappears.
3. Approach and watch the title change from CHASE to ATTACK. The Boss alternates its
   configured sweep/slam patterns through the same pipeline; see Boss tuning below.
4. Press J with at least 0.5 seconds between successful Mage shots. A defeated Boss
   becomes grey, reports DEAD and produces one material placeholder. If the player
   dies instead, the encounter is cleared and the player returns to Hub at full HP.
5. Run away (player speed 180; guardian speed 54-104 depending on the arena, 70 elsewhere).
   Beyond the loss radius, hostiles
   become IDLE and stop where they are. Re-enter the detection radius to reacquire.
   Use map obstacles to verify that pursuit routes around walls rather than crossing them.
   J bolts stop at walls; Mage K cycles elements and O executes its selected mode.
6. Attack then fast-travel or enter a portal. Old yellow rectangles disappear and
   cannot damage actors on the new map. Player HP is preserved; each new battle
   scope gets a fresh Boss. Ordinary travel preserves HP; failure return explicitly restores it.
   Resize still uses the existing camera.

Class tuning is in `PlayerClasses`; scene/enemy tuning remains in `GameConfig`. Neither combat models nor detection import rendering
or asset APIs. `PlaceholderRenderer` now composes `SceneArt`/`ArtAssets`, retaining camera/procedural
Hub geometry; HUD icons follow the existing responsive size policy. `CombatDemo` delegates common movement/ability
execution to `PlayerAbilities`, coordinates placement and reset/transition policy,
updates the Boss, then advances the shared CombatSystem exactly once before processing failure.

Combat limitations: axis-aligned hitboxes, discrete area-attack overlap samples and centre-only
visibility, with no armour, knockback, attack wind-up, invulnerability, combos or final class skills.
Debug rectangles are not visually clipped against walls. Dead-player movement/abilities are blocked
by the common controller; battle failure is handled before travel. Enemy HP is not persisted across transitions. Pause freezes
combat, and the existing frame-time cap discards excess stall time.

### Common player abilities

- `PlayerInput.sample()` converts held WASD and newly pressed J/K/L/O into one
  immutable `PlayerCommand`. Holding an ability key does not repeat it; failed
  requests are not queued. The adapter never chooses damage; debug class requests
  are sampled separately by `sampleClassSelection()`.
- `AbilityLoadout` supplies validated attack/dash tuning; the placeholder defaults
  live together in `PlayerClasses`; `GameConfig.PLAYER_ABILITIES` is a compatibility
  reference to the default Mage loadout. Different common loadouts can be
  injected without changing key mapping, collision, health or hit resolution.
- `PlayerAbilities.update(delta, command)` returns the accepted slot as the
  **triggered event**. `getState(slot)` reports READY, ACTIVE, COOLDOWN, or
  UNAVAILABLE (dead/unregistered or locked by another skill). Each slot owns its cooldown, measured from a
  successful trigger. Area active time comes from the actual combat invocation;
  projectile slots instead use a short launch window, independent of longer arrow flight.
  Requests are checked before that frame's elapsed time is deducted, consistently
  with combat-core; becoming ready at the end of a step requires a fresh request.
- A ready L request wins over attacks. An active dash ignores new attack requests
  and steering. Otherwise the first ready requested attack wins in O, K, J order.
  Only one new ability starts per frame, but distinct attack slots may overlap
  across frames. No input can bypass its slot's cooldown, including zero-time spam.
- Dash captures normalized WASD direction, or the last movement intent when idle
  (initially right). It calls the existing player's swept collision resolver, so
  thin walls and map boundaries stop it; diagonal sliding follows normal movement.
  The burst uses only its remaining duration, then ordinary movement uses leftover
  frame time. Blocked distance is not refunded. There is no dash damage or immunity.
- The controller updates the independent player hurtbox after movement and submits
  shape-generated attacks or configured projectile volleys to the existing `CombatSystem`.
  It never applies damage itself.
  Core combat handles hostile filtering, once-per-invocation hits, HP and death.
- NPC practice returns consume input before simulation. Portals are checked at the player's
  final position after movement/combat. Arrival immediately clears old-map attacks
  and cancels dash. The same controller retains slot cooldowns across all successful
  transitions, including re-entry, so travel cannot recharge abilities. This is
  not a save system; restarting the application still starts a fresh session.
- Pause freezes timing; R explicitly clears arena attacks, dash and cooldowns and
  restores HP. Restoring Health alone does not reset ability cooldowns.

Manual checks: run `./gradlew lwjgl3:run`, use the Portal Guide to enter Training,
and try J/K/O near the stationary target.
The native title shows each key's lifecycle and HP; yellow rectangles show actual
attack hitboxes. Hold keys and then tap rapidly to verify no automatic repeat or
cooldown bypass. Try WASD+L against a wall and near each map edge, then L without
WASD to check remembered facing. Press all four keys to check dash priority. Dash
or attack, return through the Training exit NPC, and confirm effects stop without resetting slot
cooldowns. Battle death now returns to Hub and restores HP automatically.

Ability limitations: Archer, Mage and Katana use the first-version skills below.
No targeting cursor, energy economy, combo buffer,
animation, invulnerability, enemy-body collision or art assets are implemented.
Portal checks use final body overlap, not a swept dash/portal trigger; a thin trigger
may be crossed without arrival. World obstacles still use swept collision and cannot
be tunnelled through. A future class can reuse the command slots/timing and submit
its own geometry or straight-projectile configuration to combat-core. Further combos,
balance and visual presentation remain separate work.

Run ability-specific checks with:

```bash
./gradlew core:test --tests '*PlayerAbilitiesTest' --tests '*AbilityLoadoutTest' --tests '*PlayerInputTest' --tests '*CombatDemoTest'
```

### Current first-version class skills

All classes share one Player, Health, canonical Combatant, eight-direction movement,
collision-safe L dash and damage pipeline. ClassDefinition composes an immutable loadout,
an alternate ultimate and small SkillKind execution choices; there are no class-specific
Player subclasses or damage engines.

| Class | J | K | O mode A | O mode B |
| --- | --- | --- | --- | --- |
| Archer | Single non-piercing arrow | Atomic five-arrow 40-degree fan | Fixed Boss-position rain, six waves | Three seconds untargetable, not invulnerable |
| Mage | Selected-element magic bolt | FIRE → WATER → LIGHTNING → ICE → WIND | Fixed-position, element-locked 1.5-second cast, then six field waves | Four-second rapid buff: faster J, two independently random-element bolts |
| Katana | Facing-based light slash | 0.25-second parry; counter dash on block, recovery on miss | One-shot defensive counter stance | Collision-safe thrust; accepted hit unlocks six follow-up strikes |

Mage casting prevents WASD/J/K/L/O. Effective HP loss interrupts it without refunding O;
zero damage does not. Untargetability prevents new AI target acquisition but does not
stop existing hitboxes or environmental damage. Katana defensive hits use the generic
HitInterceptor boundary; counters execute after combat settlement, never in a damage callback.
All repeated attacks have explicit finite counts and use normal combat/wall occlusion.
Archer and Mage share straight, swept, non-piercing projectile flight, cleanup and atomic capacity checks.

F1/F2/F3 choose classes in production Hub; F5 toggles A/B for the selected class.
Player stores one UltimateMode per class, initially A, and the Mage element separately.
PlayerAbilities.selectUltimateMode(mode) always requires Hub, including debug harnesses.
Class selection, travel, defeat and retry retain choices and remaining slot cooldowns.
Active casts, defensive states, dash, launch windows and live attacks prevent unsafe selection.
Mage K may change element during battle, but not while dead, casting or in its J launch window.

Stateful skills are composed into PlayerAbilities by ClassSkills. The game-loop order is
abilities.update → combat.update → abilities.afterCombat. Rebinding after travel/death clears
activity, targetability overrides and defensive registrations, without replacing Player.
Only one stateful K/O execution runs at a time. Fields/buffs permit ordinary actions;
casting and Katana defensive/recovery/forced-movement stages lock ordinary inputs.

The window title exposes Mage element, O mode and skill state. Existing yellow rectangles
show active attack geometry; no textures, fonts, sprites, animation or audio are loaded.
The fresh production screen still needs Developer B's external encounter acceptance before
a task Boss can spawn; fast travel does not bypass that boundary.

Full tuning, changed files, automated verification and manual checks:
[First-version class skills report](docs/FINAL_CLASS_SKILLS_REPORT.md).

```bash
./gradlew test --tests '*FinalClassSkillsTest' --tests '*CombatCapabilitiesTest' --tests '*QuestPortalFlowTest'
```

## Boss encounter prototype

Run `./gradlew lwjgl3:run`, accept a Boss task, select Battle at the Portal Guide and enter its arena. The Sentinel has
a 64 x 64 collision body centred on the map: (800, 600) in the built-in 1600 x 1200 arenas. It uses 400 HP and
the same player/enemy combat pipeline. Training has a stationary practice target instead; Hub remains peaceful.
The practice target also starts at the map centre. Nearby safe placement is used only if the centre is
blocked or occupied by the player, such as during a debug reset; invalid bodies are never forced into walls.
The native window title reports Boss HP, phase, state and any dropped material.

`Boss` composes the existing `Enemy` health owner and `EnemyAI` controller; there is
no Boss-specific HP/damage engine or duplicated A*. `EnemyAI.update(target, delta,
false)` performs normal detection, cached full-body A* chase, wall clearance and
attack readiness without submitting a basic attack. Boss selects its pattern instead.
The original two-argument EnemyAI update still performs normal enemy attacks.

The shared template's pattern cycles, used unchanged by four of the five guardians:

| Phase | Ordered pattern cycle | Damage | Reach | Active / cooldown |
| --- | --- | --- | --- | --- |
| 1 | Forward sweep | 15 | 96 | 0.2 s / 1.2 s |
| 1 | Surrounding slam | 20 per target | 128 expansion | 0.3 s / 1.6 s |
| 2 | Heavy forward sweep | 20 | 120 | 0.2 s / 0.8 s |
| 2 | Heavy surrounding slam | 30 per target | 160 expansion | 0.3 s / 1.1 s |

- Configuration is centralized in `GameConfig.SENTINEL` using validated `BossConfig`
  and immutable pattern lists. Patterns reuse AttackSpec and AttackShape, not animation
  events. A successful cast advances the cycle; rejected cooldown attempts do not.
- Template navigation speed is 70; each campaign guardian overrides only its speed and replan
  interval, listed under [Guardian traits](#guardian-traits). Detection radius 500, loss radius 650
  and attack stopping distance 96 (centre-to-centre) are shared by all arenas. The fire guardian
  also scales the damage column above to 60%. The existing conservative clear-corridor rule gates
  attacks at the chosen target. Position/hurtbox updates use the current GameMap.
- At HP <= 50%, phase changes from 1 to 2 exactly once. A 0.75-second transition
  pauses navigation/new attacks and cancels existing Boss attacks **without resetting
  source cooldown**. Healing does not revert phase or restart the pause. Large transition
  frames discard leftover movement/attack time instead of issuing catch-up attacks.
- The arena updates player/Boss decisions, advances CombatSystem once, processes any
  player failure return, then calls `Boss.synchronizeOutcome()` if the encounter is still active.
  Settled damage changes phase/death/drop state
  before rendering. Lethal damage takes priority over phase change. Each attack uses
  combat-core's per-target hit ledger; no frame-driven repeated damage is introduced.
- Defeat stops decisions and cancels remaining Boss attacks. Exactly one immutable
  `MaterialDrop` is generated at the last valid Boss position: a unique reward instance
  ID, material ID `sentinel-core`, quantity 1, owning map ID and a small rectangle.
  The existing renderer shows it in gold; no icon, texture, font or effect is loaded.
- Inventory is still a placeholder. `getDrop()` supports inspection and `claimDrop()`
  transfers a reward at most once. A future pickup/inventory adapter must check distance
  and capacity/acceptance before claiming on the game-loop thread. There is deliberately
  no automatic pickup or fake inventory storage in this branch.
- Loot is encounter-scoped, not saved: travel/reset discards unclaimed rewards and
  returning creates a fresh encounter. R restores the living player and replaces the
  Boss with a fresh, safely placed instance. Dead Battle players return to Hub instead.
  It is a debug reset,
  not a farming/persistence policy. Restoring a defeated Boss's Health alone does not
  restart its encounter. Detached old-map Boss references cannot claim or generate loot.

Manual checks: approach the Sentinel, attack with J/K/O and observe HP/phase in the
title. Check that the second phase starts once at 200 HP and has a short pause.
Defeat it and verify one gold drop remains after further updates. Try attacking then
travelling with 1, or pressing R, to confirm cancellation and fresh encounter state.
Boss bodies and attacks remain simple rectangles; no formal Boss UI or art is added.

### Guardian traits

Each campaign arena also selects one signature behaviour, composed into the same `Boss` class
through `BossTrait`; arenas outside the campaign keep the template's behaviour exactly.

| Guardian | Arena | Signature mechanic | Numbers |
| --- | --- | --- | --- |
| Fire | Crossroads | Connected attacks add burning stacks that keep dealing damage | 2 s per tick, 5 ticks, 5 stacks, 2 damage per stack per tick |
| Water | Water Arena | Connected attacks shove you outward through your own collision | 120 units, no action lock |
| Lightning | Lightning Arena | Dealing it no damage for ten seconds calls down a strike | 10 s, 10 damage, 0.6 s lock |
| Ice | Ice Arena | Its third connected attack freezes you | 3 hits, 2 s freeze |
| Wind | Wind Arena | Burst damage makes it blink to a random nearby spot, arriving immune | 45 damage in 1.5 s, 4 s cooldown, 120-320 units, 2 s immunity |

Movement speed also differs: fire 78, water 62, lightning 92, ice 54, wind 104. Health, detection
and loss radii, stopping distance, pattern geometry, pacing, phase threshold and reward stay shared,
so one balance change still reaches all five. The fire guardian is the one exception on damage: it
keeps 60% of the template's direct pattern damage and returns the difference as burning, so its
total output stays comparable while the fight rewards breaking contact.

Negative statuses are named in large text above the actor they affect: `BURNING xN`, `FROZEN`,
`STAGGERED`, `CHARGED`, `VULNERABLE` and `INVULNERABLE`. These read from shared combat state, so
a guardian you freeze with a water-plus-ice reaction is labelled as well. Burning stacks, the
lightning countdown, the frostbite counter and the blink cooldown also appear in the window title.
A living Boss is a red square on the minimap and on the expanded `[M]` map.

Limitations: two fixed-rectangle pattern cycles, no projectile Boss pattern, cutscene,
animation or phase immunity. Shared elemental reactions can affect Boss health. The full-body navigation corridor can be
conservative in narrow passages. Once cast, fixed hitboxes do not follow targets or
clip visually against walls; all candidate targets now require clear origin-to-centre
segments at settlement, in addition to initial AI clearance. Actors have no mutual body collision. Inventory pickup,
persistent defeat/drop state and final material/skill balance remain future work.

```bash
./gradlew core:test --tests '*Boss*Test' --tests '*EnemyAITest' --tests '*CombatSystemTest'
```

## Element attachment, reactions and derived attacks

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

See [Element reaction completion report](docs/ELEMENT_REACTION_COMPLETION_REPORT.md) for tuning,
files, validation and limitations.

## Basic enemy AI

- `Enemy` still owns only shared Health. `EnemyAI` composes that entity with one
  collision body, a stable Combatant, the current WalkableArea, and immutable
  `EnemyConfig`. Construction validates spawn geometry before registering the actor.
  There is no duplicate health, damage calculation, or cooldown timer in AI.
- `EnemyState` is a simple enum-based finite-state model, not separate State-pattern
  classes or a behaviour tree. IDLE acquires a living registered hostile target at
  centre distance <= detectionRange. CHASE/ATTACK keep tracking until distance is
  greater than loseRange. This hysteresis avoids flicker around the detection edge.
- CHASE uses direct, normalized, delta-based movement when a full-body corridor is
  clear, otherwise follows cached A* waypoints around obstacles. Actual movement still
  uses the existing `WalkableArea.move` X-then-Y swept collision. One distance budget
  is shared across all waypoints consumed in a frame, so route turns cannot multiply
  speed. Direct/final approaches stop at attack range. Attack-distance comparison
  tolerates four coordinate ULPs solely for rounding; map bounds are not relaxed.
- ATTACK stops movement and calls `CombatSystem.tryAttack` with the configured
  AttackSpec and a fixed square of half-size attackRange, centred on the enemy.
  Detection/decision ranges are Euclidean centre distances; damage still uses the
  common rectangular hitbox/hurtbox pipeline. AI requests an attack only when the
  conservative body-to-target corridor is clear, avoiding strikes initiated through
  walls. An attack can hit other valid targets
  in that square. AI never calls takeDamage directly or advances shared combat time.
- Missing, dead, friendly, self, or unregistered targets cause IDLE. A detached old
  controller cannot pursue actors in a new arena. DEAD reflects zero Health immediately,
  even after combat settlement in the same frame, and prevents movement/attack requests.
  Ordinary healing cannot revive; explicit debug restoration resets awareness.
- `EnemyConfig` validates positive finite speed/radii, attack <= detection <= loss,
  and positive autonomous cooldowns. Invalid time/overflowing movement is rejected.
  Ordinary AI remains available to isolated tests, but the gameplay scene no longer
  spawns mobs or uses the former ENEMY_COUNT/ENEMY_SIZE/ENEMY_SPAWN_OFFSET settings.
- `EnemyConfig` additionally configures `pathCellSize` (default 16 world units) and
  `repathSeconds` (default 0.5). The existing five-argument constructor remains valid.
  `AStarPathfinder` builds a bounded four-neighbour navigation grid from full-body
  occupancy, not textures or tile artwork. Each edge and off-grid connector checks
  the entire swept envelope, so thin walls and diagonal corners cannot be skipped.
  A priority queue uses travelled cost plus an admissible Euclidean stand-off-distance
  heuristic, with stable tie-breaking. Map edges are included even when dimensions
  are not multiples of cell size. `WalkableArea.getBounds()` bounds the search.
- Each controller keeps its own waypoint index and target snapshot. Clear direct
  approaches skip A*. A moving target is replanned at the configured interval while
  blocked; unchanged valid routes are reused. No-route results are also throttled,
  leaving the enemy still rather than repeatedly walking into a wall. Death, target
  loss, reset, and arena replacement discard route state. Detection/loss radii still
  apply while detouring; A* does not grant unlimited pursuit.
- Each enemy has independent health, state, geometry, and per-source combat cooldown.
  Generic clients can own a normal list of controllers; there is no singleton enemy or ECS.
  Initial spawn candidates do not overlap, but moving entities are not solid to one
  another, so enemies can later overlap. Movement/attack pause with the existing loop.

Navigation limitations: the grid is a discrete approximation. Narrow, off-grid passages
may require smaller cell spacing, and a sealed region correctly returns no route.
Grids above 20,000 nodes are rejected instead of allocating unbounded search data.
Collision occupancy is cached for static maps; rebuild the pathfinder if terrain changes.
Conservative rectangular corridors may choose longer routes than an exact line sweep.
There is no patrol, home return, dynamic entity avoidance, or crowd pathfinding.
Detection remains distance-based. The enemy's attack-initiation clearance check is
separate from combat-core's shared wall-occlusion gate for player attacks and collateral hits.
No animation, audio, particles, loot, or enemy-state
persistence is implemented. HP/state use the title; geometry remains placeholder-only.

To check the new detour behaviour manually, lure enemies near the first Training wall
(x=240..264, ending at y=380), then move around its top opening to the opposite side
while staying within the loss radius. Enemies should follow the opening, not push into
the wall. Automated navigation tests also traverse both alternating Training walls,
check moving-target replanning, and verify that repeated no-route queries are throttled.

```bash
./gradlew core:test --tests '*Enemy*Test' --tests '*CombatDemoTest'
```

## Current class responsibilities

- `MainGame`: application lifecycle and active-screen selection.
- `GameSession`: map registration, current map, player ownership, and safe transitions.
- `TravelSystem`: portal triggering, arrival protection, unlock state, and fast-travel checks.
- `GameScreen`: coordinates input, simulation, lifecycle, and the temporary view.
- `Player`: validated position/size/speed, eight-direction movement, and owned shared health.
- `PlayerClass`: stable Archer/Mage/Katana identity independent of visuals and Java subclasses.
- `ClassDefinition`: composition of identity and immutable common ability tuning.
- `PlayerClasses`: centralized class prototypes, with Mage as the default identity.
- `AttackShape`: pure interchangeable area/forward hitbox strategies, not separate damage pipelines.
- `Health`: bounded integer HP, damage, healing, life status, and explicit restoration.
- `Damageable`: common validated damage/life-status contract for combat targets.
- `Combatant`: entity/faction adapter with independently supplied hurtbox geometry.
- `Faction`: minimal player/enemy friendly-fire distinction.
- `AttackSpec`: reusable validated attack tuning.
- `Attack`: one invocation's identifier, hitbox, active lifetime, and hit ledger.
- `Projectile`: read-only flight observations with pipeline-owned position/range/lifetime updates.
- `ProjectileSpec`: validated straight-shot and symmetric-volley tuning.
- `CollisionSweep`: pure continuous rectangle interior-entry mathematics for walls and targets.
- `CombatSystem`: registration, cooldowns, hit detection, damage calculation, and delivery.
- `CombatDemo`: battle enemy collection, player requests, AI/combat update order, and map/reset cleanup.
- `PlayerInput`: translates held WASD and edge-triggered J/K/L/O into a `PlayerCommand`.
- `PlayerCommand`: immutable movement and common ability intent, without keyboard dependencies.
- `PlayerAbilities`: common attack execution, independent cooldowns, dash, and lifecycle queries.
- `AbilityLoadout`: validated immutable common area-attack and dash tuning.
- `AbilitySlot` / `AbilityState`: stable slot identifiers and observational lifecycle values.
- `GameConfig`: shared player, camera, combat-debug, and frame-time tuning.
- `CameraFollow`: stateless subject-centre, map-clamp, and small-map view mathematics.
- `PlaceholderRenderer`: applies the follow camera and renders shapes through a fixed `FitViewport`.
- `Enemy`: hostile entity with independent shared health, composed by EnemyAI.
- `EnemyAI`: one actor's decisions, cached navigation route, and collision-constrained geometry.
- `EnemyConfig`: validated movement/ranges, shared attack tuning, and navigation resolution/timing.
- `AStarPathfinder`: bounded full-body grid search, clear direct approaches, and safe route segments.
- `EnemyState`: IDLE, CHASE, ATTACK, and DEAD diagnostics.
- `NPC`: future non-player character.
- `GameMap`: map identity/type, dimensions, obstacles, named spawns, and existing collision.
- `MapCatalog`: built-in Hub and Battle Map definitions.
- `MapType`: Hub/Battle metadata.
- `SpawnPoint`: validated world-space placement anchor.
- `Portal`: source map/region, destination map/spawn, and enabled snapshot.
- `FastTravelDestination`: named destination and unlocked snapshot.
- `TravelCatalog`: built-in portal routes and initial destination states.
- `CollisionBox`: immutable geometry with finite, positive extents.
- `WalkableArea`: point/body occupancy and collision-resolved movement.
- `Faction`, `DamageType`, `ElementType`: shared combat/damage classification enums.
- `ElementRules`: the fixed attached-to-triggering reaction table and its results.
- `ElementField`: a fixed-geometry ground/charged effect with a finite `ReactionTimer`.
- `ElementStatus`: one Combatant's current attachment and reaction bookkeeping.
- `ReactionTimer` / `ReactionTuning`: periodic-tick scheduling and reaction damage/count tuning.
- `CombatStatus`: freeze/stagger/vulnerability and queued displacement for one Combatant.
- `HitInterceptor` / `HitFeedback`: defensive-hit interception boundary and its resolution.
- `Boss`: two-phase Sentinel encounter composing `Enemy` and `EnemyAI`, with pattern cycles and drops.
- `BossConfig` / `BossScaling`: validated Boss tuning and phase-threshold scaling.
- `BossState`: CHASE/ATTACK/TRANSITION/DEAD Boss lifecycle diagnostics.
- `BossChallenge`: one admitted Boss encounter's outcome/reward bookkeeping for `EncounterFlow`.
- `TrainingDummy`: the stationary, non-hostile, self-healing Training Ground practice target.
- `Inventory`: stackable material storage keyed by material type, in acquisition order.
- `MaterialDrop`: one immutable claimable Boss reward instance.
- `NPC`: shared identity/interaction-range base for Hub non-player characters.
- `PortalGuide`: Hub NPC adapter that selects and opens the shared Hub portal's destination.
- `ClassTrainer`: Hub NPC (Class Master) adapter for profession/ultimate-mode selection.
- `ClassSkills`: composes each class's stateful K/O execution into `PlayerAbilities`.
- `SkillKind` / `SkillTuning`: stateful skill identifiers and their validated numeric tuning.
- `UltimateMode`: a class's selectable A/B ultimate configuration.
- `ActiveEncounter` / `BattleResult`: an admitted encounter request and its settled outcome.
- `EncounterFlow`: the external-adapter boundary for admitting encounters and claiming results.
- `BossQuests`: this session's NPC guardian-task acceptance, completion and reward tracking.
- `DialogueLayout`: shared self-sizing NPC dialogue panel presentation.
- `CreditsDialogue`: the one-time Old Sage ending dialogue shown in Memorial Hall.
- `HudLayout`: responsive HP/position/minimap/weapon HUD sizing and visibility thresholds.
- `SceneArt` / `ArtAssets`: imported pixel-art texture lookup used by `PlaceholderRenderer`.
- `TrainingAppearance`: randomized Training background/Boss-appearance selection per visit.
- `BackgroundMusic`: map- and guardian-progress-driven background music playback.
- `Lwjgl3Launcher`: desktop window configuration and application entry point.

## Scope

Movement supports static, axis-aligned rectangles across eight built-in maps
(Hub, Memorial Hall, Training Ground and five guardian Battle arenas). The
first-version elemental reactions, class skills and imported pixel-art
presentation described above are implemented; this section only tracks what
remains out of scope. It does not include dynamic entity-to-entity collision,
external map-file loading, crowd navigation, saved/persisted map or quest
state, or final (post-balance) character skill numbers and artwork. Collision
uses X-then-Y axis separation rather than a continuous diagonal sweep, so large
diagonal steps can take a different route around corners than many smaller
steps. Normal screen updates cap delta to keep this effect limited; each axis
still cannot tunnel. Player/world model code is independent of LibGDX graphics
and asset APIs; future artwork changes can replace the renderer without
changing movement or collision rules.

Portal triggers sample overlap once per update, not a continuous swept trigger
path; a custom very large movement that crosses an entire portal between updates
can miss it. Built-in regions and the demo frame-time cap avoid that in normal
play. There is no travel menu, distance/cost restriction, automatic discovery,
quest gating, persisted unlock state, or portal animation/audio in this branch.
