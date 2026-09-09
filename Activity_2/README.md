# COMP2120 Group Game

Java and LibGDX foundation for COMP2120 Assignment 3. The current application
demonstrates a Hub and multiple Battle Maps, named spawn points, eight-direction
player movement, static rectangle collision, automatic portals, and unlocked fast
travel, and a map-bounded follow camera using geometric placeholders. Player and
enemy models share validated integer health and one attack pipeline, independent
of the demonstration view. Battle maps include multiple basic autonomous enemies.
Crossroads also includes a two-phase placeholder Boss and encounter-scoped material drops.
The shared combat pipeline supports opt-in elemental attachment, two provisional reactions,
and bounded one-generation derived attacks; built-in class attacks remain non-elemental by default.
It loads no images, fonts, animation, sound, or music assets.

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

The run task automatically uses `assets/` as the working directory and applies
the macOS first-thread JVM option when required.

## Build and test

```bash
./gradlew clean build
./gradlew test
./gradlew core:javadoc
```

Build output is written under each module's `build/` directory and is ignored by
Git.

## Map and movement demonstration

The game starts in the Hub. Walk into a purple rectangle to use a portal. Number
keys now execute fast travel through its unlock checks, replacing unrestricted
debug map switching. The window title shows the active map and travel feedback.

| Key | Action | Initial state |
| --- | --- | --- |
| 1 | Fast travel to Hub `entry` | Unlocked |
| 2 | Fast travel to Training `entry` | Locked |
| 3 | Fast travel to Crossroads `entry` | Locked |
| U | Unlock both Battle fast-travel destinations for this session | Debug only |

The old key 4 map-selection shortcut has been removed. Visiting through a portal
does not automatically unlock fast travel; future quest/discovery code can call
`unlockDestination(id)`. Portal enabled state and fast-travel unlock state are
independent. Closing/restarting the game restores the initial state.

Hub has a blue-green floor and Battle Maps have a red-brown floor. Grey rectangles
are obstacles, gold circles mark spawn anchors, and the green rectangle is the
player. Bright purple rectangles are enabled portals; dark purple rectangles are
disabled examples. A fixed-size camera view scrolls over maps larger than the
view, instead of fitting the entire map into the window.

| Portal | Source trigger (x, y, width, height) | Destination |
| --- | --- | --- |
| hub-training | Hub (160, 64, 48, 64) | Training `entry` |
| hub-crossroads | Hub (160, 400, 48, 64) | Crossroads `entry` |
| hub-disabled | Hub (220, 64, 48, 64), disabled | Crossroads `east` |
| training-hub | Training (64, 64, 80, 80) | Hub `entry` |
| crossroads-hub | Crossroads (64, 64, 80, 80) | Hub `north` |

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

To check manually, launch the game, hold each movement key, walk into each side
of a wall, reverse away from contact, try W+D and opposing keys, and resize the
window. Initially press 2/3: the title should report that the destination is locked
and position should not change. Walk right from the Hub spawn into the nearby
purple portal to reach Training at (80, 80). Stay still: the overlapping return
portal must not send you back. Walk fully outside it (for example right past
x=144), then re-enter to return to Hub. Try U then 1/2/3 to exercise fast travel.
The disabled dark-purple portal must have no effect. A successful travel command
consumes the frame, leaving the player exactly at the selected spawn.

## Camera behaviour and verification

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

1. Launch with `./gradlew lwjgl3:run`, press U then 3 to enter Crossroads.
2. Walk right from its entry along the unobstructed lower part of the map. The
   view begins scrolling once the player reaches its centre, then stops at the
   right boundary. Reverse direction and verify the left boundary similarly.
3. Re-enter Crossroads with 3 and walk up near the left side to check vertical
   scrolling and the top boundary; reverse toward the bottom. Avoid re-entering
   the return portal if you want to stay on the same map during the check.
4. Resize the window wide, tall, and small while stationary and while moving.
   Shapes must retain their proportions and the same world region must remain
   visible. Minimize/restore and check that the camera still follows normally.
5. Use 1/2/3 or the portals and check that the new spawn is visible immediately.
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
  position, map, movement speed, or any other entity state. It is a foundation
  for a later respawn coordinator, not an implemented respawn workflow.
- `Player` and the existing minimal `Enemy` implement `Damageable` by delegation.
  Each owns a separate `Health`, exposed by `getHealth()` for queries and
  validated healing/restoration. Existing constructors remain valid and use
  each entity's `DEFAULT_MAX_HP` (currently 100). Custom capacities use
  `new Player(bounds, speed, area, maxHp)` or `new Enemy(maxHp)`.
- Movement and map/portal/fast-travel transitions preserve HP. The health model
  does not decide whether dead players may move/travel. The common ability controller
  now blocks dead-player movement and abilities; debug fast travel remains allowed.
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

Temporary controls (attack values below are the default Mage, not final character skills):

| Key | Debug action |
| --- | --- |
| J | Magic bolt: 25 damage, 400 units/s, 360 range, 0.25 s launch, 0.5 s cooldown |
| K | Special attack: 40 damage, 120-unit extension, 0.3 s active, 2 s cooldown |
| L | Dash: 600 units/s for 0.2 s (120 units unobstructed), 1 s cooldown |
| O | Ultimate: 75 damage, 180-unit extension, 0.5 s active, 8 s cooldown |
| F1 / F2 / F3 | Select Archer / Mage / Katana when no player attack or dash is active |
| R | Restore player/enemy HP, reset AI awareness, and clear attacks/cooldowns; no repositioning |

1. Run `./gradlew lwjgl3:run`, then press U and 2 to enter Training. Two red enemies
   appear near the entry and begin approaching. Placement is checked against map
   walls, the player, and earlier spawns. Hub has no enemies.
2. The cyan outlines are hurtboxes. The player's is inset from its green body,
   demonstrating independence from visual size. Face an enemy with WASD and press J:
   a small yellow magic bolt travels until impact, 360 units, or 1.5 seconds.
   The first valid enemy it hits loses 25 HP; the bolt then disappears.
3. Stand still and watch the title change from CHASE to ATTACK. In range each enemy
   stops and automatically requests a 10-damage attack, with a one-second cooldown
   and 0.2-second active hitbox. Multiple enemies each have their own cooldown.
   The former H manual enemy-attack shortcut has been removed.
4. Press J with at least 0.5 seconds between successful strikes to kill enemies.
   Dead enemies become grey, report DEAD, and stop moving/attacking. Press R to
   restore all HP and clear attacks/cooldowns; positions do not reset. A dead player
   cannot attack, and enemies stop pursuing it. R allows normal decisions to resume.
5. Run away (player speed 180, enemy speed 80). Beyond the loss radius, enemies
   become IDLE and stop where they are. Re-enter the detection radius to reacquire.
   Use map obstacles to verify that pursuit routes around walls rather than crossing them.
   J bolts stop at walls; K/O remain fixed surrounding rectangle spell prototypes.
6. Attack then fast-travel or enter a portal. Old yellow rectangles disappear and
   cannot damage actors on the new map. Player HP is preserved; each new battle
   scope gets fresh enemies. Resize still uses the existing camera.

Class tuning is in `PlayerClasses`; scene/enemy tuning remains in `GameConfig`. Neither combat models nor detection import rendering
or asset APIs. `PlaceholderRenderer` only draws rectangles/outlines; HP feedback uses
the native title rather than loading fonts. `CombatDemo` delegates common movement/ability
execution to `PlayerAbilities`, coordinates placement and reset/transition policy,
updates every EnemyAI, then advances the shared CombatSystem exactly once.

Combat limitations: axis-aligned hitboxes, discrete area-attack overlap samples, no global wall
occlusion/line-of-sight for area attacks, and no armour, knockback, attack wind-up,
invulnerability, combos, or final class skills. These debug
area attacks may hit through nearby walls. Dead-player movement/abilities are blocked
by the common controller; debug fast travel remains allowed. Enemy HP is not persisted across transitions. Pause freezes
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
  UNAVAILABLE (dead/unregistered). Each slot owns its cooldown, measured from a
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
- Fast travel consumes input before simulation. Portals are checked at the player's
  final position after movement/combat. Arrival immediately clears old-map attacks
  and cancels dash. The same controller retains slot cooldowns across all successful
  transitions, including re-entry, so travel cannot recharge abilities. This is
  not a save system; restarting the application still starts a fresh session.
- Pause freezes timing; R explicitly clears arena attacks, dash and cooldowns and
  restores HP. Restoring Health alone does not reset ability cooldowns.

Manual checks: run `./gradlew lwjgl3:run`, press U then 2, and try J/K/O near enemies.
The native title shows each key's lifecycle and HP; yellow rectangles show actual
attack hitboxes. Hold keys and then tap rapidly to verify no automatic repeat or
cooldown bypass. Try WASD+L against a wall and near each map edge, then L without
WASD to check remembered facing. Press all four keys to check dash priority. Dash
or attack, travel with 1/2/3, and confirm active effects stop without resetting slot
cooldowns. Use R to recover after death.

Ability limitations: Archer, Mage and Katana use the projectile/spell/melee prototypes below.
No targeting cursor, energy economy, combo buffer,
animation, invulnerability, enemy-body collision or art assets are implemented.
Portal checks use final body overlap, not a swept dash/portal trigger; a thin trigger
may be crossed without arrival. World obstacles still use swept collision and cannot
be tunnelled through. A future class can reuse the command slots/timing and submit
its own geometry or straight-projectile configuration to combat-core; specialised
spells, weapon combos and other final skill execution remain future work.

Run ability-specific checks with:

```bash
./gradlew core:test --tests '*PlayerAbilitiesTest' --tests '*AbilityLoadoutTest' --tests '*PlayerInputTest' --tests '*CombatDemoTest'
```

### Player class foundation

Every class is the same concrete `Player`, with one Health model, collision body,
ordinary movement speed and canonical Combatant. There are no ArcherPlayer,
MagePlayer or KatanaPlayer subclasses. `PlayerClass` holds explicit stable IDs
(`archer`, `mage`, `katana`); `Player.getPlayerClass()` exposes current identity.
The default remains Mage, now with a travelling magic bolt for J and area pulses for K/O.

`ClassDefinition` composes identity with an immutable `AbilityLoadout`. Each J/K/O
entry supplies damage, range, active duration, cooldown and an `AttackShape` strategy.
L supplies speed, duration and cooldown to the same swept-collision dash executor.
The catalog is ordinary Java data; no new configuration library or asset format is introduced.

| Debug key | Class | J placeholder | K / O placeholders | Dash |
| --- | --- | --- | --- | --- |
| F1 | Archer | Moving arrow, 20 damage, 480 range | Power arrow / five-arrow fan | 650 units/s, 0.2 s, 1.2 s cooldown |
| F2 | Mage | Moving magic bolt, 25 damage, 360 range | Small / large surrounding spell rectangles | 600 units/s, 0.2 s, 1 s cooldown |
| F3 | Katana | Forward light slash, 30 damage, 48 reach | Forward heavy slash / single surrounding slash | 750 units/s, 0.16 s, 0.9 s cooldown |

These values demonstrate replaceable class configuration, not agreed final balance.
Forward rectangles follow the existing last WASD intent (right initially), including
normalized diagonals; diagonal shapes are axis-aligned envelopes, not rotated blades.
Mage K/O and Katana use fixed active hitboxes, not complete final skills or combos.
Archer arrows and Mage J bolts use the same moving, geometry-only projectile pipeline.
No renderer branches on class or loads weapons, sprites, animations, audio or textures.

The small `AttackShape` Strategy is used only where behaviour already differs:
AREA expands the source hurtbox on all sides, while FORWARD extends it in the facing
direction. A custom pure strategy can be injected per attack slot without modifying
PlayerAbilities, health, input, or damage calculation. It returns geometry only and
must not mutate gameplay state. Class-wide execution remains composition/data;
there is no separate strategy class, manager or factory for every profession.

Selection and integration rules:

- Initialize directly with the Player constructor accepting max HP and ClassDefinition,
  then use the three-argument PlayerAbilities constructor to respect that selection.
- For a running controller, call `selectClass(PlayerClasses.get(type))`. A custom
  ClassDefinition can also replace a class's tuning/shape strategies.
- A successful switch preserves the same Player, Health, Combatant, map, position,
  movement speed, facing and **remaining cooldown seconds for every slot**. It does
  not heal, revive, reposition or clear other actors' combat state.
- Active player attacks (including external pipeline submissions and flying arrows), launch windows, active dash,
  death, stale combat registration or an identical definition return false without
  mutation. Null definitions throw. Cooldown alone does not forbid selection, but
  switching back and forth cannot refresh it. Requests are never queued.
- F1/F2/F3 use key edges; simultaneous selection prefers F1, then F2, then F3.
  Selection attempts consume that frame's new movement/J/K/L/O requests, whether
  accepted or rejected. Existing attacks/dash and simulation time still advance.
  Travel and R reset take precedence over a same-frame class request.
- Map transitions preserve selection and slot cooldowns; R resets HP/abilities but
  retains class. Separate players share only immutable definitions, never HP or timers.
- The native title shows current class and the existing J/K/L/O lifecycle feedback.
  On keyboards using system media functions, use Fn+F1/F2/F3 as needed.

Manual check: start the game, press each selection key while idle, then use WASD to
set facing and try J/K/O near enemies in Training (U then 2). Compare the yellow
forward/area outlines, range, damage and dash distance. Try selecting during an
active attack/dash (rejected), then during cooldown (accepted without refreshing it).
Travel and press R to verify class persistence. No final selection UI or unlock/save
system is implemented. Straight projectile flight is supported by combat-core, separately
from AttackShape; more advanced class skill execution remains future work.

```bash
./gradlew core:test --tests '*PlayerClassTest' --tests '*AttackShapeTest' --tests '*PlayerInputTest' --tests '*CombatDemoTest'
```

### Archer projectile prototypes

Select Archer with F1 (Fn+F1 on some keyboards). Facing comes from the current/last
WASD direction, right initially; arrows capture that direction at launch and do not
turn when the player later moves. All arrows are small yellow collision rectangles
drawn by the existing placeholder renderer. No bow/arrow texture, sprite, animation,
particle, sound or other art asset is loaded.

The local Activity 0 file only links to an online design document, which could not
be read during this implementation. K/O below are explicit replaceable prototypes,
not claimed final designs. All values live in the Archer entry of `PlayerClasses`.

| Key | Prototype | Damage | Speed | Max range | Launch window / cooldown |
| --- | --- | --- | --- | --- | --- |
| J | One ordinary arrow, 6 x 6 body | 20 | 600 units/s | 480 | 0.15 s / 0.45 s |
| K | One power arrow, 8 x 8 body | 45 | 800 units/s | 640 | 0.25 s / 2.5 s |
| L | Existing collision-safe Archer dash | None | 650 units/s | 130 unobstructed | 0.2 s / 1.2 s |
| O | Five-arrow fan, 6 x 6 bodies, total 40-degree spread | 30 per arrow | 550 units/s | 500 | 0.4 s / 8 s |

- Arrow lifetime is at most 1.2 seconds for all three attacks; range, impact or
  cancellation may end flight sooner. K is stronger/faster, not piercing. O angles
  are -20, -10, 0, +10, +20 degrees around the captured direction. Each arrow has
  its own invocation ID and can damage only one target once. A broad target can
  receive multiple fan arrows (up to 150 total damage from this prototype).
- Common J/K/L/O arbitration, input edges and independent cooldowns are reused.
  Flight does not hold J on cooldown: after its launch window and 0.45-second
  cooldown, another arrow can launch while the previous arrow is still flying.
  There is no energy/ammunition system or new input path.
- `ProjectileSpec` holds speed/size/lifetime/count/spread. The existing AreaAttack
  record retains its compatibility name and constructors; a non-null projectile
  component selects flight instead of AttackShape. Mage J reuses the same component;
  Mage K/O and Katana continue to use shape delivery.
- `CombatSystem.tryProjectiles` validates the entire volley before issuing IDs or
  consuming source cooldown. All arrows start at the source hurtbox centre so a
  forward spawn offset cannot bypass a nearby wall. A blocked/full-body-invalid
  spawn throws without spending cooldown. A dead/cooling source or full projectile
  capacity returns an empty volley; failed casts do not spend slot cooldown.
- The same CombatSystem owns area attacks and projectiles in invocation order;
  no second manager or damage system exists. It advances each arrow by the minimum
  of frame travel, remaining range and remaining lifetime, then selects the nearest
  swept intersection with a valid hostile hurtbox or static world collision.
- `CollisionSweep` performs continuous rectangle/segment interior-entry checks.
  `WalkableArea.firstBlock` / GameMap combine walls and map bounds for straight
  flight, separate from existing axis-sliding actor movement. Thin walls and thin
  targets cannot be skipped merely because a frame is large. Off-path diagonal
  objects do not collide just because they lie inside a broad bounding rectangle.
- Self, allies, dead and unregistered targets do not consume arrows. The nearest
  valid target wins; equal-time wall contact wins over a target, and equal-time
  targets use registration order. Tangential or endpoint-only target contact is
  not damage; interior entry is required. Damage uses the same central calculation,
  Damageable/Health update and death handling as fixed area attacks.
- Impact, range expiry, lifetime expiry, source death (on combat update), unregister,
  R reset and map transitions remove/cancel the relevant arrows. Retained references
  also report inactive. A maximum of 256 live projectiles per CombatSystem and at
  most nine arrows per volley bound work; capacity rejects a whole volley, never half.
- Class selection stays blocked while a launch window or any arrow is active;
  map travel cancels flight and preserves slot cooldowns. Existing R reset deliberately
  resets cooldowns. Ordinary movement, health, map and camera logic are shared.

Manual verification: run `./gradlew lwjgl3:run`, select F1, then U and 2 for Training.
Face an enemy with WASD and tap J: damage should happen on arrival, not key press.
Try K for the single power arrow and O for the fan. Fire at walls and around corners,
tap repeatedly during cooldown, then fire and travel/reset to check cleanup. Existing
Mage spells and Katana slashes are available with F2/F3 after arrows disappear.

Limitations: this is straight non-piercing projectile flight, not homing, gravity,
ricochet, penetration, arrow rain, animation or final skill balance. Enemy hurtboxes
are sampled after their movement each frame; relative swept collision against the
target's entire movement path is not implemented. Fixed Mage/Katana area attacks
still retain their previous no-wall-occlusion rules. Graphics-level key/visual checks
remain separate from the headless JUnit coverage.

```bash
./gradlew core:test --tests '*ArcherAbilitiesTest' --tests '*ProjectileTest' --tests '*ProjectileSpecTest' --tests '*CollisionSweepTest'
```

### Mage spell prototypes

Select Mage with F2 (Fn+F2 if needed); it is also the default class. No confirmed
final spell specification is available locally, so K/O are deliberately replaceable
prototypes, preserving their previous area tuning. All Mage values are centralized
in `PlayerClasses`; Archer and Katana configurations are unchanged by this feature.

| Key | Prototype | Damage | Geometry / movement | Active window / cooldown |
| --- | --- | --- | --- | --- |
| J | Magic bolt | 25 | 10 x 10 rectangle, 400 units/s, 360 maximum travel | 0.25 s launch / 0.5 s |
| K | Small area pulse | 40 per target | Source hurtbox expanded 120 units on each side | 0.3 s / 2 s |
| L | Shared collision-safe dash | None | 600 units/s, up to 120 units over 0.2 s | 0.2 s / 1 s |
| O | Large area pulse | 75 per target | Source hurtbox expanded 180 units on each side | 0.5 s / 8 s |

- J captures normalized current/last WASD facing, initially right. Its independent
  flight lifetime is 1.5 seconds; the 360-unit range normally expires first after
  0.9 seconds. Neither turning nor the end of the launch window changes its flight.
  Range measures body translation; the bolt's half-size extends beyond its centre.
- Magic bolts reuse `ProjectileSpec`, `Projectile`, swept world/target collision,
  nearest valid target selection, and the same `Attack` list as Archer. Hits go through
  combat-core and Health, never a Mage damage method. A bolt is non-piercing and is
  removed on impact, expiry, source death, reset or map transition. No new manager exists.
- K/O capture an axis-aligned area at cast time; it does not follow the caster.
  Each valid enemy entering during its active window can be hit once. Touching only
  the rectangle edge is not a hit. These inherited area attacks do **not** test wall
  occlusion; they are not projectiles, circles, explosions, or damage-over-time spells.
- L retains ordinary shared collision/axis sliding, with no teleport, immunity or
  wall bypass. Every slot keeps independent cooldowns and existing input arbitration.
  Class switching waits for active spells/flight to end; map travel cancels them but
  preserves cooldowns. R deliberately restores HP and clears active spells/timers.
- Stable `mage-light`, `mage-special` and `mage-ultimate` attack IDs identify the
  prototypes for future extensions. Elemental metadata can now be supplied through shared
  AttackSpec configuration; no element is assigned to Mage by default. There is no mana economy,
  class-specific status system, separate MagePlayer/MageProjectile, or duplicate damage pipeline.
- Existing yellow rectangles display spell collision geometry; the native title
  shows class, HP and slot state. No textures, fonts, sprites, animation or audio load.

Manual verification: run `./gradlew lwjgl3:run`, select F2, then U and 2 for Training.
Use WASD to aim J and verify delayed impact rather than immediate ranged damage.
Fire toward a wall, spam J/K/O during cooldown, and try K/O with multiple nearby enemies.
Dash into a wall with L; fire then travel/reset to confirm cleanup. Use R after death.
The automated suite exercises model integration; keyboard/visual checks remain manual.

Limitations: straight non-homing bolts; no relative sweep against the target's entire
movement during a frame; K/O square areas can affect enemies through walls. These
are provisional spell effects and balance values, with no final class UI or artwork.

```bash
./gradlew core:test --tests '*MageAbilitiesTest' --tests '*PlayerClassTest' --tests '*CombatDemoTest'
```

Run combat-specific checks with:

```bash
./gradlew core:test --tests '*Combat*Test'
```

### Katana melee prototypes

Select Katana with F3 (Fn+F3 if needed). The shared player/ability/combat foundation
already supports J/L/O; this feature gives K a directional heavy-slash definition
instead of the former surrounding placeholder and adds dedicated melee regression tests.
No confirmed final Katana skill specification is available locally. K/O below are
explicitly replaceable prototypes, not an assumed final combo or animation design.

| Key | Prototype | Damage | Reach / movement | Active window / cooldown |
| --- | --- | --- | --- | --- |
| J | Forward light slash | 30 per target | 48-unit normalized forward extension | 0.15 s / 0.35 s |
| K | Forward heavy slash | 45 per target | 80-unit normalized forward extension | 0.25 s / 2 s |
| L | Shared collision-safe dash | None | 750 units/s, up to 120 units | 0.16 s / 0.9 s |
| O | Single surrounding slash | 85 per target | Hurtbox expanded 110 units on each side | 0.35 s / 7 s |

- All tuning stays in the Katana entry of `PlayerClasses`. J/K use the existing
  `AttackShape.FORWARD`; O uses `AttackShape.AREA`. No KatanaPlayer, melee damage
  manager, animation timeline or class-specific input handler is needed. Archer/Mage
  behavior and configurations are unchanged by this feature; Mage remains the default.
- Facing is the current/last nonzero WASD intent, initially right. Eight directions
  normalize forward reach. J/K capture the independent source hurtbox and facing
  at invocation; moving/turning does not drag or rotate an already active slash.
  The next swing uses the latest facing. Diagonals are axis-aligned envelopes, not
  precise arcs, rotated blades or circular-distance tests; source geometry is included.
- The existing pipeline owns each swing's ID, active window and per-target hit ledger.
  A swing can damage multiple hostile targets, but each only once, including late
  entrants. Leaving/re-entering and zero-time input spam cannot repeat damage.
  A fresh swing after cooldown can hit the same enemy again. Exact edge contact is
  not damage. Health, lethal hits and invalid/friendly/self filtering are unchanged.
- O emits exactly one attack, not repeated per-frame slashes or a multi-stage combo.
  J/K/O keep independent slot cooldowns; existing simultaneous input priority remains
  dash, then O/K/J among ready slots. L uses normal swept wall/boundary collision and
  axis sliding, with no teleport, invulnerability or attack damage attached.
- Source death, reset and map transitions cancel existing attacks. Travel preserves
  cooldowns; R deliberately resets them and restores HP. Active swings prevent class
  switching through the same selection guard used by the other classes.
- Yellow debug rectangles show the actual collision boxes, and the native title
  shows HP/class/slot state. No character/weapon textures, slash animation, hit effect,
  particle, sound or other artwork is loaded.

Manual verification: run `./gradlew lwjgl3:run`, select F3, then U and 2 for Training.
Face enemies from each direction with WASD and tap J. Try K at a distance where J
misses, then face away and confirm J/K do not hit behind. Use O among several enemies;
each should lose HP only once. Spam inputs during cooldown, dash into a wall with L,
then attack and travel/reset to check cleanup. Use R after death.

Limitations: fixed rectangular slash prototypes, no precise sword arc, combo, charge,
multi-hit timeline or final skill balance. Like the existing fixed-area attacks,
J/K/O do not check wall occlusion and can hit through walls within their rectangles.
Only L movement is wall-constrained. Melee samples current hurtboxes per update,
not targets' complete movement paths; keyboard/visual verification remains manual.

```bash
./gradlew core:test --tests '*KatanaAbilitiesTest' --tests '*PlayerClassTest' --tests '*CombatDemoTest'
```

## Boss encounter prototype

Run `./gradlew lwjgl3:run`, press U then 3 to enter **Crossroads**. The Sentinel is
a large 64 x 64 red rectangle, normally starting at (360, 100). It uses 400 HP and
the same player/enemy combat pipeline. Hub and Training keep their existing behavior.
The native window title reports Boss HP, phase, state and any dropped material.

`Boss` composes the existing `Enemy` health owner and `EnemyAI` controller; there is
no Boss-specific HP/damage engine or duplicated A*. `EnemyAI.update(target, delta,
false)` performs normal detection, cached full-body A* chase, wall clearance and
attack readiness without submitting a basic attack. Boss selects its pattern instead.
The original two-argument EnemyAI update still performs normal enemy attacks.

| Phase | Ordered pattern cycle | Damage | Reach | Active / cooldown |
| --- | --- | --- | --- | --- |
| 1 | Forward sweep | 15 | 96 | 0.2 s / 1.2 s |
| 1 | Surrounding slam | 20 per target | 128 expansion | 0.3 s / 1.6 s |
| 2 | Heavy forward sweep | 20 | 120 | 0.2 s / 0.8 s |
| 2 | Heavy surrounding slam | 30 per target | 160 expansion | 0.3 s / 1.1 s |

- Configuration is centralized in `GameConfig.SENTINEL` using validated `BossConfig`
  and immutable pattern lists. Patterns reuse AttackSpec and AttackShape, not animation
  events. A successful cast advances the cycle; rejected cooldown attempts do not.
- Navigation speed is 70, detection radius 500, loss radius 650 and attack stopping
  distance 96 (centre-to-centre). The existing conservative clear-corridor rule gates
  attacks at the chosen target. Position/hurtbox updates use the current GameMap.
- At HP <= 50%, phase changes from 1 to 2 exactly once. A 0.75-second transition
  pauses navigation/new attacks and cancels existing Boss attacks **without resetting
  source cooldown**. Healing does not revert phase or restart the pause. Large transition
  frames discard leftover movement/attack time instead of issuing catch-up attacks.
- The arena updates player/enemy/Boss decisions, advances CombatSystem once, then
  calls `Boss.synchronizeOutcome()` so settled damage changes phase/death/drop state
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
  returning creates a fresh encounter. R restores the existing player/basic enemies
  but replaces the Boss with a fresh, safely placed instance. It is a debug reset,
  not a farming/persistence policy. Restoring a defeated Boss's Health alone does not
  restart its encounter. Detached old-map Boss references cannot claim or generate loot.

Manual checks: approach the Sentinel, attack with J/K/O and observe HP/phase in the
title. Check that the second phase starts once at 200 HP and has a short pause.
Defeat it and verify one gold drop remains after further updates. Try attacking then
travelling with 1, or pressing R, to confirm cancellation and fresh encounter state.
Boss bodies and attacks remain simple rectangles; no formal Boss UI or art is added.

Limitations: two fixed-rectangle pattern cycles, no projectile Boss pattern, cutscene,
animation or phase immunity. Shared elemental reactions can affect Boss health. The full-body navigation corridor can be
conservative in narrow passages. Once cast, fixed hitboxes do not follow targets or
clip against walls; the initial target requires clearance but secondary area targets
may still be hit through walls. Actors have no mutual body collision. Inventory pickup,
persistent defeat/drop state and final material/skill balance remain future work.

```bash
./gradlew core:test --tests '*Boss*Test' --tests '*EnemyAITest' --tests '*CombatSystemTest'
```

## Element attachment, reactions and derived attacks

The local repository contains no agreed elemental reaction table. The following rules
are **explicit prototypes**, not final class assignments, balance or element-design decisions.
All elements use `ElementType`: NONE, WATER, FIRE, LIGHTNING, ICE and WIND.
No particles, shaders, textures, fonts, sounds or status icons are loaded.

### Current centralized rules

| Attached + incoming (either order) | Reaction | Result |
| --- | --- | --- |
| WATER + FIRE | vaporize | Add 10 damage to the triggering hit; consume attachment |
| WATER + LIGHTNING | discharge | Consume attachment; queue one 5-damage square attack, half-extent 40, active 0.15 s |
| Same element | None | Refresh attachment duration without bonus damage |
| Other non-NONE pairs | Undefined / no reaction | Replace attachment with the incoming element |
| Incoming NONE | None | Preserve existing attachment without refreshing it |

- `ElementRules.PROTOTYPE` centralizes the two recipes and 3-second attachment lifetime.
  `new CombatSystem(customRules)` supports an alternative immutable table without editing
  Player, Mage, Enemy or Boss. Reversed/duplicate pair definitions are rejected.
- Each registered target has at most one attachment in CombatSystem, exposed as immutable
  `ElementStatus`. Any valid direct elemental hit, including zero damage, can apply it.
  Misses, edge-only contact, self, friendly and dead targets cannot apply or react.
- Timing follows existing combat semantics: sample hits at the start of the update,
  then age attachments once by delta, including newly applied statuses. An attachment
  expires at zero; invalid delta is rejected before changes. Separate same-step attacks
  resolve in invocation order, so later direct hits can react with earlier applications.
- A reaction consumes the existing element without leaving the incoming element behind.
  Death, unregister and arena clear also remove attachments. R and map transitions use
  those existing cleanup paths. Cancelling attacks alone does not clear target attachments.
- There is one damage path: confirmed hit -> pure rule lookup -> central saturated
  base-plus-bonus calculation -> Damageable -> Health. Elements have no separate damage
  method or HP implementation; Boss phase/death/drop handling observes the same HP.

### Derived attack safeguards

- A reaction child is a normal fixed `Attack` owned by the same CombatSystem, with a
  new ID, the original incoming attacker, and `parentAttackId` for provenance. It does
  not consume another source/slot cooldown. Geometry is centred on the reacting target.
- New children first resolve on the **next combat update**, even if that update has
  zero delta; they are not recursively executed inside damage delivery. Their lifetime
  starts being sampled/aged there, not during the creation step.
- Children are explicitly marked derived and cannot apply elements or trigger reactions.
  They carry NONE and share normal faction, alive, geometry and once-per-target filtering.
  Nearby targets keep their attachments unless killed. No grandchildren are possible.
- One direct attack-target hit can generate at most one child, using the existing hit
  ledger. Two different reacting targets are two events and may produce overlapping
  children; those distinct attacks can each damage a shared nearby target once.
- A lethal reaction may still produce its child. Source death, cancellation, unregister
  and map/reset clearing cancel queued/active children. A source killed later in the same
  update cannot leave a child to damage targets on the next step.
- At most 64 derived attacks may be live at once. Capacity exhaustion, ID exhaustion or
  unrepresentable effect geometry skips only the optional child; the triggering hit and
  attachment consumption remain settled and are never retried. No reaction history grows
  indefinitely; completed attacks and expired/dead-target statuses are removed.

### Opt-in class configuration and diagnostics

Existing four-argument AttackSpec calls still produce NONE. Archer/Mage/Katana retain
their previous tuning and behavior until a slot's AttackSpec is configured with an element:

```text
new AttackSpec("mage-light", 25, 0.25, 0.5, ElementType.WATER)
```

Keep the slot's existing range/shape/ProjectileSpec when replacing its AttackSpec.
PlayerAbilities preserves that tag when submitting attacks with zero source-level cooldown.
EnemyConfig and BossConfig.Pattern already accept AttackSpec, so they need no separate
elemental controller. No hard-coded profession-to-element mapping or new input key is added.

CombatDemo displays attached names such as `[WATER]` in the native window title for
player, enemies and Boss; derived attacks use existing yellow hitbox rectangles. Default
loadouts therefore do not show reactions until explicitly configured. Automated class/demo
tests exercise opt-in configuration and verify labels, reset and map cleanup without graphics.

Still undecided: every reaction other than the two prototypes above, including water/ice,
fire/ice, fire/lightning and all wind combinations; final direction-dependent multipliers,
attachment duration/stacking, freeze/stun/burn behavior, class assignments and resistance rules.
These are not implemented. Area children currently use square hitboxes without wall clipping,
not a circle, chain lightning, damage-over-time system or VFX. One-update child latency and
the existing frame-sampled attachment timing are intentional foundation semantics.

```bash
./gradlew core:test --tests '*Element*Test'
```

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
- `GameConfig.BASIC_ENEMY` configures speed 80, detection radius 200, loss radius 260,
  attack radius 44, damage 10, active time 0.2 seconds, and cooldown 1 second. Speed
  and all radii must be positive and finite, attack <= detection <= loss, and an
  autonomous attack must have a positive cooldown. Invalid time/overflowing movement
  distance is rejected. `ENEMY_COUNT`, `ENEMY_SIZE`, and `ENEMY_SPAWN_OFFSET` configure
  the temporary battle-map demonstration. Existing built-in battle entries support
  two enemies; a map with fewer safe placement candidates gets fewer or none.
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
  The arena owns a normal list of controllers; there is no singleton enemy or ECS.
  Initial spawn candidates do not overlap, but moving entities are not solid to one
  another, so enemies can later overlap. Movement/attack pause with the existing loop.

Navigation limitations: the grid is a discrete approximation. Narrow, off-grid passages
may require smaller cell spacing, and a sealed region correctly returns no route.
Grids above 20,000 nodes are rejected instead of allocating unbounded search data.
Collision occupancy is cached for static maps; rebuild the pathfinder if terrain changes.
Conservative rectangular corridors may choose longer routes than an exact line sweep.
There is no patrol, home return, dynamic entity avoidance, or crowd pathfinding.
Detection remains distance-based. The enemy's attack-initiation clearance check does
not add global wall occlusion to combat-core: player attacks and collateral hits still
use its existing overlap rules. No animation, audio, particles, loot, or enemy-state
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
- `Inventory`: future item storage and item operations.
- `Lwjgl3Launcher`: desktop window configuration and application entry point.

## Scope

This is intentionally a small foundation. Movement currently supports static,
axis-aligned rectangles and three built-in maps. It does not include dynamic entity
collision, external map-file loading, crowd navigation, elemental map rules,
saved map state, final character skills, or final artwork. Collision uses X-then-Y
axis separation rather than a continuous diagonal sweep, so large diagonal steps
can take a different route around corners than many smaller steps. Normal screen
updates cap delta to keep this effect limited; each axis still cannot tunnel.
Player/world model code is independent of LibGDX graphics and asset APIs; future
artwork can replace the renderer without changing movement or collision rules.

Portal triggers sample overlap once per update, not a continuous swept trigger
path; a custom very large movement that crosses an entire portal between updates
can miss it. Built-in regions and the demo frame-time cap avoid that in normal
play. There is no travel menu, distance/cost restriction, automatic discovery,
quest gating, persisted unlock state, or portal animation/audio in this branch.
