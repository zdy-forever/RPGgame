# Guardian encounters and traits

Reviewed against `7fea86a` on 2026-10-02. Core guardian implementation: Daoyan Zhu (`u7782042`).
Sources: [BossTraits](../core/src/main/java/au/edu/anu/comp2120/enemy/BossTraits.java),
[BossScaling](../core/src/main/java/au/edu/anu/comp2120/enemy/BossScaling.java),
[BossConfig](../core/src/main/java/au/edu/anu/comp2120/enemy/BossConfig.java),
[GameConfig](../core/src/main/java/au/edu/anu/comp2120/screen/GameConfig.java).

## Shared encounter framework

Quest Keeper offers Fire, Water, Lightning, Ice and Wind Sentinels. Fire uses Crossroads;
the others use their named arenas. Each admitted Battle spawns one Boss and no ordinary mobs.
The body is 64 × 64, centred at (800, 600) in built-in arenas, with validated fallback placements
for an obstructed/player-occupied centre. Training uses a separate stationary immortal target.

The shared unscaled template has 400 HP, phase two at 50% HP, a 0.75-second transition and
one sentinel-core reward. Phases alternate the following patterns:

| Pattern | Base damage | Active time | Cooldown | Range/shape |
| --- | --- | --- | --- | --- |
| Sweep | 15 | 0.2 s | 1.2 s | 96, forward |
| Slam | 20 | 0.3 s | 1.6 s | 128, area |
| Heavy sweep | 20 | 0.2 s | 0.8 s | 120, forward |
| Heavy slam | 30 | 0.3 s | 1.1 s | 160, area |

Melee first locks a harmless warning rectangle, stops pursuit and prepares for 0.8 seconds;
the red region and accelerating exclamation flashes show the planned area. Phase changes,
control interruption, death and scope changes cancel the relevant prepared attacks.
Campaign guardians also fire aimed non-homing elemental bolts while pursuing: base damage 12,
speed 300, range 480, lifetime 2 seconds and base cooldown 2 seconds. They use shared swept
projectile collision and their arena's element.

## Five distinct mechanics

| Guardian | Pursuit speed | Signature behavior |
| --- | --- | --- |
| Fire | 78 | Landed hits add burning up to five stacks; each stack deals 2 per tick, every 2 s, for five ticks after refresh |
| Water | 62 | Landed hits request a 120-unit push through the target's collision-controlled movement |
| Lightning | 92 | After 10 s without the Boss losing HP, an eligible target receives a 10-damage retribution strike and 0.6 s stagger |
| Ice | 54 | Every third landed hit applies 2 s freeze, then resets the hit counter |
| Wind | 104 | A 45-damage burst within its 1.5 s tracking window can trigger a blink; cooldown 4 s and post-blink invulnerability 2 s |

Fire retains 60% of shared direct melee damage, rounded with a minimum of one. Burning supplies
additional damage over time. This is a deliberate damage tradeoff, so it is inaccurate to say
all five guardians have numerically identical attacks. Trait updates use actual settled events
and the shared combat/status paths; they do not create a second HP/reward system.

Wind searches within a 180–320 centre-distance ring around the target, with at least 120 units
of displacement from its old centre. It uses at most 24 random attempts and a bounded fallback
of 48 bearings × three radii. Candidates must be walkable and non-overlapping; it does not clamp
an invalid landing into a wall. A successful blink clears its prepared melee/navigation state
and consumes the decision step, preventing immediate walking back into contact.

## Rematches

Only successful wins against the same guardian affect its next fresh encounter. Failures,
Training and wins against other guardians do not increase it. For previous successes `n`:

```text
health multiplier = min(4, 1 + log10(n + 1))
attack rate       = min(2, 1 + 0.25 * log10(n + 1))
pattern cooldown  = max(active time, base cooldown / attack rate)
```

Health is rounded from the base definition; tuning is applied once to each new Boss, not
compounded on the previous scaled result. Damage, movement and warning duration are unchanged
by rematch scaling. Distinct quest completion still has only five stones. Save/load restores
win counts, and the next encounter uses the restored matching guardian's count.

## Presentation and cleanup

Live Boss/Training target markers appear red on the minimap and expanded map; the player is
green. Marker state reads the current target and disappears on return to Hub. StatusLabels
shows control effects before other states, including FROZEN, STAGGERED, CHARGED, VULNERABLE,
INVULNERABLE and Fire's BURNING xN where applicable. Burning currently uses text, not a new icon.

Traits are fresh per Boss; old effects and references are cleared on death/travel. Fire/Lightning
periodic effects use ordinary eligibility, damage and status handling. God mode prevents player
HP loss but is not a general promise of status immunity. The non-blocking three-second outcome
banner does not delay recovery or duplicate rewards. See [defeat](PLAYER_DEFEAT_REPORT.md).

## Verification and balance limits

From Activity_2:

```sh
./gradlew core:test --tests '*BossTraitTest' --tests '*BossAttackTest' \
  --tests '*BossScalingTest' --tests '*BossTest' --tests '*PlayerControlEffectsTest' \
  --tests '*GameSavesTest' --tests '*CoreGameplayIntegrationTest'
```

Tests cover trait triggers, timers, target isolation, safe blink placement, controls, warning/
projectile behavior, per-guardian scaling and restored win counts. Actual automated evidence
is in the [tester guide](TEST_COVERAGE_REVIEW.md#recorded-result). Old development-stage balance
measurements are not current acceptance evidence and are no longer repeated here.

The guardians share a pattern framework and art-selection pipeline while differing in trait,
movement and element. No five entirely separate movesets are claimed. Human testing still needs
to assess warnings, readability, all class matchups, repeated challenges and difficulty; automated
passes alone do not establish balanced combat.
