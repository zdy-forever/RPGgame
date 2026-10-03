# Player defeat and Hub recovery

Reviewed against `7fea86a` on 2026-10-02. Core recovery implementation: Daoyan Zhu (`u7782042`).
Source: [CombatDemo](../core/src/main/java/au/edu/anu/comp2120/screen/CombatDemo.java), coordinated
with [EncounterFlow](../core/src/main/java/au/edu/anu/comp2120/EncounterFlow.java).

## Outcome order

In Battle, a dead Player is processed before reset/class/travel input and again after shared
combat settlement. Death takes priority over Boss death in the same step: this is failure,
not victory. The coordinator records failure, transitions to Hub, clears the old combat scope
and restores the same Player to full equipment-adjusted HP. No further movement or portal
request from that frame is applied.

A successful living-player Boss kill also returns/heals, but separately claims the one-time
reward and marks the quest successful. Both outcomes show a three-second non-blocking banner
in Hub. The banner is presentation only and does not postpone return or settle rewards again.

## Retained and cleared state

| State | On failure return |
| --- | --- |
| Player, Health, bag and Equipment objects | Retained |
| Class, each class's O choice, Mage element | Retained |
| Slot cooldowns | Remaining durations retained; active execution is cancelled |
| Bag/chest items, worn gear, upgrades and potion stock | Retained; consumed potions are not refunded |
| Accepted quest/retry identity and previous distinct completions | Retained; failed guardian gains no win |
| Debug switches/save-write block | Retained for the live session |
| Old Boss/AI/phase state and navigation | Detached; retry creates a fresh encounter |
| Attacks/projectiles, attachment, fields, charged/control state | Cleared with the combat scope |
| Casts, dash/thrust/follow-up, parry/buffs and captured targets | Cancelled; targetability restored |
| HP and current map | Full HP in Hub |

The old Boss is detached, not revived. Stale references cannot continue attacking the new scope
or generate/claim another reward. Repeated failure polling is harmless because the recovered
Player is alive in Hub. A retry has a fresh map-transition stamp and fresh Boss state; previous
successful rematch counts still determine scaling.

Failure creates no material reward, distinct completion, victory-count increment or automatic
save. Both disk slots retain their previous contents. A later explicit safe manual save can
record the recovered timeline. Loading an older save intentionally replaces current progress.

## Training and debug boundaries

Training is not a failed Battle: its target is immortal, its ordinary incoming damage retains
at least 1 HP for a living Player, and five quiet simulation seconds restore HP. It does not
create campaign outcomes/rewards. Explicit invalid/dead Training states are not automatically
revived by that recovery timer.

R reset is ignored in production Battle. Outside Battle, it restores HP/abilities and clears
Training score where applicable; it is a debug/practice control, not a way to cancel a losing
admitted fight. Menu pause and save/load cannot bypass Battle's safe-state restrictions.

## Verification

From Activity_2:

```sh
./gradlew core:test --tests '*PlayerDefeatTest' --tests '*CoreGameplayIntegrationTest' \
  --tests '*QuestPortalFlowTest' --tests '*GameSavesTest' \
  --tests '*OutcomeBannerTest' --tests '*PortalGuideTrainingTest'
```

Tests cover failure priority, simultaneous deaths, preserved identity/configuration/cooldowns,
cleanup, no duplicate rewards, retained retry task, potion stock and lack of failure autosave.
The [tester guide](TEST_COVERAGE_REVIEW.md#recorded-result) records actual automated execution.

Manual regression: lose a real fight after consuming a potion, confirm full-HP Hub return and
unchanged stock/class/O choice, verify the failure banner, retry the same task, then check that
no automatic checkpoint was created by the defeat. Repeat through quit/reload for a known
pre-fight save; automated fixture assertions do not establish native visual/input acceptance.

The [2026-10-03 native follow-up](PLAYTEST_REPORT.md) observed unsuccessful Mage and Archer
Wind attempts returning to Hub at HP 120 with the task retained, followed by the user
completing Wind normally. The retained screenshot shows the failure result and retry objective;
exact cooldown, inventory and reward preservation boundaries remain covered by model tests
rather than fully isolated native assertions in this session.
