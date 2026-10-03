# Element reactions and status effects

Reviewed against `7fea86a` on 2026-10-02. Core reaction implementation: Daoyan Zhu (`u7782042`).
Sources: [ElementRules](../core/src/main/java/au/edu/anu/comp2120/combat/ElementRules.java),
[ReactionTuning](../core/src/main/java/au/edu/anu/comp2120/combat/ReactionTuning.java),
[CombatSystem](../core/src/main/java/au/edu/anu/comp2120/combat/CombatSystem.java).

## Ordered reaction table

The left element is already attached; the right is carried by the new confirmed hit.

| Attached → trigger | Reaction | Current effect |
| --- | --- | --- |
| Fire → Water | Vaporize | Triggering base damage ×2 |
| Water → Fire | Vaporize | Triggering base damage ×1.5 |
| Ice → Fire | Melt | Triggering base damage ×2 |
| Fire → Ice | Melt | Triggering base damage ×1.5 |
| Fire ↔ Lightning | Overload | 20-damage Fire area, half-extent 80; 100 poise loss and 60-unit collision-resolved knockback |
| Water ↔ Lightning | Electro-Charged | Four target-local Lightning ticks of 8, every 0.5 s over 2 s |
| Water ↔ Ice | Freeze | 0.8 s action suppression |
| Ice ↔ Lightning | Superconduct | 12-damage Ice area, half-extent 70; physical vulnerability for 4 s |
| Fire/Water/Ice/Lightning → Wind | Swirl | Fixed field of the consumed element, half-extent 80; six ticks of 6, every 0.5 s over 3 s |

Symmetric pairs have both explicit ordered rules. Wind must be the trigger for Swirl;
Wind attachment followed by another element uses ordinary replacement. Water Swirl deals
actual damage. Duplicate ordered rules are rejected rather than resolved by ambiguous precedence.

## Attachment and delivery

Ordinary attachment lasts 3 seconds. With no attachment, a direct non-None element attaches;
the same element refreshes it. A compatible different element reacts and consumes attachment
without leaving the trigger attached. An incompatible element replaces it. None preserves the
attachment without refreshing. Derived hits neither attach, refresh nor react.

CombatSystem first checks target eligibility, hostility, contact, wall visibility and defensive
capabilities. Missed/blocked/friendly/dead/duplicate contacts do not react. A valid zero-damage
direct elemental hit can still attach/react; this differs from effects requiring positive HP loss,
such as Mage interruption or debug one-shot eligibility.

Element tags and Physical/Elemental damage category are independent. Archer and Katana keep
Physical categories; Mage and all reaction outputs use Elemental. Superconduct changes physical
resistance from zero to -0.25, giving ×1.25 physical damage while active. Elemental damage does
not receive this bonus. Reaction amplification, applicable vulnerability and equipment modifiers
enter the shared calculation, with bounded integer delivery; no separate reaction HP system exists.

## Fields, ticks and bounded derivation

ElementField captures fixed world geometry, original source and direct parent attack identity.
It does not follow or retain a Boss target. ReactionTimer begins after the creation update;
its first tick is after one interval. Zero-time updates cannot create extra ticks. Catch-up
emits only newly due finite ticks, including a final boundary tick, then expires the schedule.

Overload/Superconduct children are queued for the next combat update. Field pulses use ordinary
attacks and wall visibility, then retire within their update. Charged uses one refreshed/replaced
schedule on the affected receiver, without chain damage to nearby actors. Outputs are Elemental,
non-blockable and limited to one reaction generation. They cannot recursively trigger reactions.

Bounds are 64 ticks per configured periodic instance, 64 live fields and 64 live derived attacks.
Capacity-rejected scheduled ticks are consumed rather than retried. Invalid optional child geometry
does not roll back already delivered triggering damage or repeatedly retry the reaction.

## Control effects and lifecycle

CombatStatus contains frozen/stagger timers, physical vulnerability, poise, charged schedule
and pending displacement. The default 100-poise break causes 0.5 s stagger and refills poise.
Knockback is latched during settlement, then consumed by the actor's movement owner through
world collision. Player displacement/control is integrated by CombatDemo/PlayerAbilities;
EnemyAI owns the Boss's movement/control handling. Freeze stops movement and new actions but
does not remove already active attacks.

Map/encounter clear removes registrations, attacks, attachment, fields, charged schedules,
controls and pending displacement. Source death/cancellation/unregistration ends its continuing
fields and charged damage. Other targets' already-applied freeze/vulnerability can expire
independently. A location field may remain after its original target dies while its source lives.
Safe-map snapshots do not preserve transient reaction/status state.

Health damage-version tracking records effective damage; healing does not rewind it. Potions
cannot conceal a prior hit from Mage cast interruption. Fire Boss burning is a separate guardian
trait, not an additional ElementRules pair; see [guardian traits](BOSS_TRAIT_REPORT.md).

## Verification

From Activity_2:

```sh
./gradlew core:test --tests '*ElementRulesTest' --tests '*ElementCombatTest' \
  --tests '*ReactionCompletionTest' --tests '*CombatStatusTest' \
  --tests '*CombatCapabilitiesTest' --tests '*ElementClassIntegrationTest' \
  --tests '*PlayerControlEffectsTest' --tests '*CoreGameplayIntegrationTest'
```

Tests cover ordered pairs, amplification/category boundaries, attachment replacement/consumption,
finite schedules, no recursive derivation, occlusion, control/displacement and success/failure
cleanup. [CI evidence](TEST_COVERAGE_REVIEW.md#recorded-result) records executed results.
Reaction animation clarity and numerical balance remain manual playtest subjects; passing logic
tests does not validate native VFX or every class/guardian matchup.
