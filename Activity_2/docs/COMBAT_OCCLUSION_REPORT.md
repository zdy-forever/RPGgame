# Unified combat wall occlusion

> Current integration note (2026-09-26): The occlusion contract below is still current. Its test counts belong to the original checkout; equipment modifiers now also enter the same shared damage calculation.
> See [current controls](../README.md), [inventory/equipment](INVENTORY_EQUIPMENT_REPORT.md)
> and [save/load](SAVE_LOAD_REPORT.md) for the current production behavior.

## Result

Gameplay attacks now use the existing world collision data at settlement. No class-specific
damage branches, additional projectile wall engine, physics dependency or art/audio assets
were introduced. Battle scenes remain Player plus one Boss; generic damage contracts still
support other receivers in isolated tests and future consumers.

## Contract and origins

`WalkableArea.hasLineOfSight(fromX, fromY, toX, toY)` asks whether a segment crosses static
blockers. GameMap uses its existing obstacle rectangles and the new
`CollisionSweep.segmentCrossesInterior` slab query. The original projectile rectangle sweep
is unchanged, including strict target overlap and **wall wins equal-time tie**.

| Delivery | Origin / obstruction check |
| --- | --- |
| Archer arrows and Mage bolts | Existing swept projectile body against target/walls. |
| Forward melee, including Katana J/K | Captured attacker hurtbox centre to current target hurtbox centre. |
| Mage K/O, Katana O and custom fixed areas | Captured effect centre to current target centre. |
| Boss patterns | The same AttackShape origin policy as player slots. |
| Reaction-derived areas | Captured child effect centre, not the parent's caster position. |
| Future Arrow Rain / ElementField adapters | Explicit fixed effect centre through the same attack API. |

Attack stores an immutable origin anchor independently of its fixed hitbox. Moving or turning
the caster after casting cannot redirect either. AttackShape's default origin is the created
effect; FORWARD overrides it with the source hurtbox. Custom melee shapes should override
origin as well. AttackSpec remains damage/timing/element data, not world or visual state.

Fixed settlement is: valid target → overlap → world visibility → existing damage calculation
and Health delivery → hit ledger / element / reaction processing. A blocked target receives
no damage or attachment, consumes no prior attachment and creates no child. It is not added
to that attack's hit ledger, so becoming visible during the active window permits one hit.
Existing derived-generation bounds, next-update timing and cleanup remain unchanged.

## Boundaries

- A segment through solid obstacle interior is blocked, including a zero-length origin inside a wall.
- Pure edge/corner tangency and endpoint-only contact with a wall boundary are clear.
- Finite endpoints outside the closed world bounds are blocked; non-finite geometry throws.
- One centre ray is used per candidate. This is deliberately not partial-cover sampling, polygon
  clipping or a visibility graph. A partially visible large target can be conservatively rejected.
- Fixed yellow placeholder rectangles are not visually clipped; visible overlap alone no longer
  implies a hit. No texture dimensions are consulted.
- Fixed effect placement/range is separate from damage occlusion. An effect placed on the far
  side of a wall checks sight from that location, not from its distant caster. Future targeted
  skills must validate their own legal cast locations/ranges.

## World scope and integration

CombatSystem.bindWorld assigns one collision provider to the arena. CombatDemo binds it on
map synchronization and reset; PlayerAbilities binds when attached/rebound, EnemyAI binds at
construction (including Boss composition), and projectile launches bind their existing world.
Binding the same provider is harmless. A different provider requires clearing the old scope;
clear removes the world together with attacks, registrations, cooldowns and attachments.
No old attack is allowed to silently adopt a different map's blockers.

Standalone map-free CombatSystem calculations remain compatible: without a world there are
no known blockers. All existing gameplay paths bind a world automatically. New generic
adapters not using these controllers must bind their world before settlement.

## Arrow Rain / ElementField availability

Neither a concrete Arrow Rain skill nor an ElementField class exists in this checkout.
Archer O is still the configured five-arrow fan. This branch does not silently replace it or
create an unrelated periodic-field system. Tests configure an Archer O fixed-area fixture
and a field-style elemental invocation through the real public pipeline to verify fixed
origin, obstruction, damage/attachment and expiry/deduplication contracts. These are **adapter
contract tests**, not a claim that missing final skills were implemented or exercised.

## Changed files

Production Java, relative to `core/src/main/java/au/edu/anu/comp2120/`:

- `world/WalkableArea.java`: common static visibility contract.
- `world/CollisionSweep.java`: exact segment/interior rectangle query; original projectile entry retained.
- `world/GameMap.java`: query using current static blockers and map bounds.
- `combat/Attack.java`: captured immutable origin.
- `combat/CombatSystem.java`: world binding, explicit-origin overload, shared fixed-hit gate and derived origins.
- `player/AttackShape.java`: source/effect origin policy without profession dispatch.
- `player/PlayerAbilities.java`: bind current world and submit the shape-selected origin.
- `enemy/EnemyAI.java`: bind the AI's world for common settlement.
- `enemy/Boss.java`: forward the pattern's origin to shared combat.
- `screen/CombatDemo.java`: bind current map on scope rebuild/reset.

Tests, relative to `core/src/test/java/au/edu/anu/comp2120/`:

- New `world/WorldOcclusionTest.java`: 23 cases covering crossings, symmetry, horizontal/vertical/
  diagonal rays, tangency, endpoints, interior origins, thin walls, world bounds and invalid numbers.
- New `combat/CombatOcclusionTest.java`: 37 cases covering all three classes' offensive slots with/
  without walls, fixed-area rain/field contracts, captured origins, visibility changes, elemental
  and derived settlement, all four Boss patterns, scope replacement and invalid origins.
- `combat/ProjectileTest.java`: retain front-of-wall and clear-diagonal assertions; explicitly
  clear/re-register before the second fixture changes collision worlds. Existing wall/tie tests remain.
- `enemy/EnemyNavigationTest.java`: delegate the new visibility method through its counting wrapper.

Documentation: `README.md`, `docs/ARCHITECTURE.md`, `docs/COMBAT_OCCLUSION_REPORT.md`.

## Verification

Executed from Activity_2:

```bash
./gradlew core:test
./gradlew core:test --tests '*CombatOcclusionTest' --tests '*WorldOcclusionTest' --tests '*ProjectileTest'
./gradlew clean test build javadocAudit
```

- Full suite: **719 tests in 39 suites; 0 failures, 0 errors, 0 skipped**.
- Added 60 cases; no existing test was deleted or disabled.
- Clean compilation and core/desktop packaging: **BUILD SUCCESSFUL**.
- Both modules' strict JavaDoc audits: **passed**, including production/test/private members,
  full doclint and warnings-as-errors. Added/modified Java uses the confirmed author
  **Daoyan Zhu (u7782042)** and applicable descriptions/parameter/return/exception documentation.
- No graphics assets or graphics context are needed by the new logic/tests. This branch's
  validation is automated model/build verification, not a claim of fresh manual desktop playtesting.

## Suggested handoff

Commit message:

```text
feat(combat): enforce shared static-wall occlusion for fixed attacks
```

MR title:

```text
Prevent attacks and elemental effects from hitting through solid walls
```
