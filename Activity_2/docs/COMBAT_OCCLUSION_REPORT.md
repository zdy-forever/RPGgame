# Movement collision and combat wall occlusion

Reviewed against `7fea86a` on 2026-10-02. Core implementation: Daoyan Zhu (`u7782042`).
Sources: [GameMap](../core/src/main/java/au/edu/anu/comp2120/world/GameMap.java),
[CollisionSweep](../core/src/main/java/au/edu/anu/comp2120/world/CollisionSweep.java),
[CombatSystem](../core/src/main/java/au/edu/anu/comp2120/combat/CombatSystem.java).

## Current map scope

All built-in map interiors are open. Map boundaries remain solid; obstacle rectangles are
supported by the engine and constructed explicitly in automated tests. Painted arena borders,
NPC sprites and Boss sprites do not create hidden solid walls. The game does not implement
dynamic body-to-body collision.

Player movement resolves swept X displacement followed by swept Y displacement, including dash
and other collision-controlled motion. This prevents tunnelling through thin walls on either
axis. It is not a continuous diagonal sweep; large versus small diagonal steps may take different
routes around a corner. GameScreen caps normal simulation delta at 0.1 seconds.

## Visibility and attack origins

`WalkableArea.hasLineOfSight` tests a segment against static world blockers. GameMap uses
`CollisionSweep.segmentCrossesInterior`; sprite dimensions never enter the calculation.

| Delivery | World check |
| --- | --- |
| Arrows and elemental bolts, including Boss/Training projectiles | Swept projectile body against world and targets; a wall wins equal-time contact |
| Forward melee | Captured attacker hurtbox centre to current target centre |
| Fixed area skills, Arrow Rain and Magic Circle | Captured effect centre to current target centre |
| Boss patterns | Shared AttackShape origin policy |
| Reaction child areas and ground-field pulses | Captured child/effect centre, independent of the caster's later position |

Mage K changes the selected element; it is not an area attack. Arrow Rain, Magic Circle and
ElementField are implemented and use the shared gate, not hypothetical future adapters.

Attack stores an immutable origin separately from its fixed hitbox. Moving/turning the caster
does not redirect it. Forward shapes use the source origin; default area shapes use the effect
origin. Legal effect placement is a separate concern: a field on the far side of a wall checks
from that field, not across the entire distance back to its caster.

## Boundary behavior

A segment crossing a solid rectangle's interior is blocked, including a zero-length point
inside a wall. Edge/corner tangency or endpoint-only wall-boundary contact is clear. Finite
endpoints outside world bounds are blocked; non-finite geometry is rejected.

Fixed-hit settlement requires overlap and visibility before damage/attachment/reaction delivery.
A wall-blocked candidate gains no hit-ledger entry, so it may be hit once if it becomes visible
within the attack's active lifetime. Interception/invulnerability and the other ordinary damage
guards still apply. One centre ray is used per target: partial cover is not sampled, and a large
partially visible target can be rejected. Visual effects are not clipped to the visibility ray.

## World lifetime

CombatSystem binds one WalkableArea for a scope. Gameplay coordinators bind the current map on
entry/reset; changing world requires clearing the old scope. Clearing removes old actors,
attacks, projectiles and element/status effects so they cannot silently use a new map's walls.
Map-free unit fixtures can intentionally have no blockers; production paths bind a world.

## Verification

From Activity_2:

```sh
./gradlew core:test --tests '*PlayerTest' --tests '*CollisionSweepTest' \
  --tests '*WorldOcclusionTest' --tests '*CombatOcclusionTest' \
  --tests '*ProjectileTest' --tests '*EnemyNavigationTest' \
  --tests '*ReactionCompletionTest'
```

WorldOcclusionTest checks horizontal/vertical/diagonal rays, tangency, inside-wall origins,
thin walls, bounds and invalid values. CombatOcclusionTest covers source/effect origins,
blocked delivery/attachment, visibility changes, Boss patterns and world replacement.
ProjectileTest retains swept collision and wall-tie checks. These are explicit obstacle
fixtures; a native tour of the current open arenas cannot replace them.
See [test execution and manual limits](TEST_COVERAGE_REVIEW.md).
