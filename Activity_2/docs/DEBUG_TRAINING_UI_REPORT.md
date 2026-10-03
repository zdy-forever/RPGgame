# Debug console, Training and UI

Reviewed against `7fea86a` on 2026-10-02. Sources:
[DebugConsole](../core/src/main/java/au/edu/anu/comp2120/screen/DebugConsole.java),
[DebugOptions](../core/src/main/java/au/edu/anu/comp2120/player/DebugOptions.java),
[CombatDemo](../core/src/main/java/au/edu/anu/comp2120/screen/CombatDemo.java),
[HudLayout](../core/src/main/java/au/edu/anu/comp2120/screen/HudLayout.java).

## Console

F10 (Fn+F10 where required) or backtick opens the in-game console. Enter executes; Esc,
F10 or backtick closes. It owns text input and pauses gameplay, supports Backspace, limits
input to 80 printable ASCII characters, escapes font markup and restores the prior input
processor when closed. Typing WASD/E/B must not also control the world or other panels.

| Command | Effect |
| --- | --- |
| `god` | Toggle player HP-loss immunity; does not revive or guarantee immunity to status effects |
| `oneshot` | Toggle lethal player damage against eligible enemy receivers after contact, defensive guards and positive damage calculation; immortal target stays immortal |
| `freeupgrade` | Skip upgrade material cost only; ownership, distinct stores, capacity and +2 cap remain |
| `nocd` | Clear existing J/K/L/O timers and suppress new cooldowns; retain active actions/dash/launch guards |
| `status` / `help` | Show switches or command reference without enabling cheats |
| `debug off` | Disable all four switches |

Bare switch names toggle; explicit `on`/`off` forms are also accepted. Enabled rows show green
ON, disabled rows white OFF. Invalid commands do not mutate switches. Disabling nocd restores
normal cooldowns for subsequent casts, not old hidden timer values.

Enabling any cheat marks the live game as debug-used and blocks manual/automatic save writes.
Disabling switches or loading cannot remove that block. Existing files remain readable. A fresh
game/restart creates clean debug state. Debug victory still grants ordinary quest/reward state
but cannot overwrite normal saves. The window title identifies debug-used sessions.

F1/F2/F3 class selection and F4 element tagging remain available in an eligible Hub state.
Mage K selects Mage's own element; use Class Master for O mode. F5 always belongs to manual
saving in the production UI. These shortcuts are distinct from the four console save-blocking switches.

## Training

Portal Guide selects Training without a quest. It contains one stationary immortal TrainingDummy
and no Boss AI/outcome/reward. Arena background and guardian appearance are chosen independently
on entry and remain fixed until leaving; re-entry can randomly choose the same appearance again.

CombatDemo fires a physical projectile straight upward every two simulation seconds: base damage
25, speed 180, range 720, lifetime four seconds. It uses normal combat settlement and enemy projectile
art. Stand above the target to test armour, or move aside to practise attacks safely. The scoreboard
shows dealt damage/hits, incoming post-mitigation damage and walking speed with equipment effects.

Normal Training damage keeps a living Player at least 1 HP. After five simulation seconds without
a positive hit, HP restores to equipment-adjusted maximum. Hits at 1 HP reset the timer; zero damage
does not. Menus pause the timer. Explicit invalid/dead states are not revived by quiet-time recovery.
Leaving Training ends this protection.

R clears score, attacks, cooldowns and shot timing and restores HP while retaining Player/target
positions and the visit's art. Reset while overlapping the target does not choose another spawn.
Initial entry still uses validated centre/fallback placement. R is ignored in production Battle;
the code also permits reset in Hub and Memorial Hall when no modal panel owns input. In a bag,
R is page-next, not a practice reset.

## Current UI and controls

| UI | Behavior |
| --- | --- |
| Title/Settings | New Game/Continue/Settings/Quit; persistent volume and fullscreen preference |
| Objective tracker | Wrapped primary/secondary guidance below HP; hidden in Hall, bag/chest/expanded map and below the detailed-HUD threshold |
| NPC/menu input | W/S/arrows select, E/Enter confirms, Esc backs out; NPC left-click selects/right-click activates |
| Bag/chest | Shared row actions, whole-stack transfers, A/D columns, Q/R or PageUp/PageDown paging; B switches chest to bag |
| Minimap/expanded map | Green Player, red current Boss/Training target; M expands to 92% width/88% height preserving map proportions; hidden in Hall |
| Quickbar | Class and J/K/L/O slots, Mage element where relevant, P bottle and bag count; zero grey, large counts `999+` |
| Feedback | Centred four-second translucent toasts, final 0.6 s fade; three-second non-blocking outcome banner |
| Save summaries | Sydney local date/time with AEST/AEDT; disk metadata remains UTC |

The coordinate and top-right weapon renderers have been removed; inspect equipment in the bag.
HudLayout uses a 960 × 540 reference. Base HUD scale caps at 1.4; the separate skill scale can
grow to 1.8 on larger windows. Persistent panels use 0.8 of skill scale. Detailed HUD requires
both dimensions at least 640 × 360; health requires 320 × 180. Resizing below the detail threshold
also closes the expanded map so hidden UI cannot keep gameplay locked.

Modal handlers return before movement, abilities and potion use. NPC-triggered travel can rebind
through `synchronizePausedMap()` without advancing attacks/recovery. Menu pause leaves music
playing; lifecycle pause pauses it. Title Settings writes preferences; Esc volume is session-only.

## Verification

From Activity_2:

```sh
./gradlew core:test --tests '*DebugConsoleTest' --tests '*DebugOptionsTest' \
  --tests '*PortalGuideTrainingTest' --tests '*CombatDemoTest' \
  --tests '*HudLayoutTest' --tests '*PotionFontLayoutTest' \
  --tests '*MenuKeysTest' --tests '*SaveMenuTest' --tests '*GameSavesTest' \
  --tests '*GameScreenObjectiveTest'
```

Tests cover console ownership/editing/switches, one-shot eligibility, all no-CD slots, sticky
save protection, Training shots/mitigation/recovery/reset, menu aliases, font metrics and layout
models. GameSavesTest also checks validated restored timers under nocd and unchanged save files
after a debug victory. [CI execution](TEST_COVERAGE_REVIEW.md#recorded-result) is recorded centrally.

The [2026-10-03 native session](PLAYTEST_REPORT.md) checked M expanded map, keyboard menus,
Training portal entry, skill score increase and R score/skill reset. The initially observed
Objectives/Training overlap is now fixed: Training starts below the rendered objective panel,
including wrapped text, with a scaled gap. A freshly compiled native app verified the correction;
see the report’s before/after screenshots and 83-test follow-up. Incoming shots, mitigation and
timed recovery were not observed.

Native checks still include resized console/text, pointer hit areas, colour/wrapping, closing-frame
input isolation, HUD/map markers, actual Training timing and a full debug Boss fight in a disposable
fresh game. Model assertions alone do not verify those window/audio interactions.
