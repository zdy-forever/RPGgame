# Debug console, Training tools and UI refinement

Updated 2026-09-30. This report describes the current implementation and its test boundaries.
Earlier dated reports retain historical results; they are not evidence of the current run.

## Player controls

| Area | Current behavior |
| --- | --- |
| Notifications | Centred translucent feedback expires after four seconds; repeated operations can show it again. |
| Potion quickbar | All classes show a P bottle and bag count; grey x0, exact x1–x999, then 999+. Large counts never shrink the font. P and bag use share actual-healing/refusal feedback. |
| Save onboarding | Hub Old Sage and Notice Board teach Esc / F5 / F9, safe maps, victory autosave and loading consequences. |
| Menus | W/S or Up/Down selects; E/Enter confirms; Esc returns/closes. NPC rows support left-click selection and right-click activation. |
| Bag/storage | Select rows with W/S or arrows. Storage A/D or Left/Right selects a column. E/Enter acts; Q/R or PageUp/PageDown pages. Right-click remains supported; footer controls are clickable. Bag/storage pause simulation. |
| Map | M expands to 92% of window width and 88% of height, preserving world proportions. NPC labels are shifted to avoid nearby labels. Red marks a live Boss or Training target; green marks the player. M/Esc closes. |
| Legacy HUD | Coordinate and top-right weapon rendering calls are commented out. Original implementations remain available. Equipment is inspected in the bag. |
| Save timestamps | Slot summaries use Australia/Sydney with AEST/AEDT; files retain UTC timestamps and need no migration. |

Menu handling returns before player movement/skill sampling. Opening and closing frames are consumed.
The same item action methods serve mouse and keyboard. Expanded map and console layout are recomputed
from window dimensions. Actual rendering, pointer geometry and native key delivery require the manual
checks below; model tests alone do not establish visual acceptance.

## Debug console

Press F10 (possibly Fn+F10 on macOS) or backtick to open the in-game console. Type a command and press
Enter. Esc, F10 or backtick closes it. No native text dialog is used: the project's LWJGL3 backend
cancels that API immediately. `DebugConsole` owns bounded ASCII text editing, escaped font markup,
command submission and input-processor restoration; `GameScreen` owns drawing and modal frame routing.

| Command | Effect |
| --- | --- |
| `god` | Toggles player HP damage immunity. Does not revive a dead player or promise immunity to status effects. |
| `oneshot` | Toggles lethal player damage to enemy receivers after collision, invulnerability/interception and damage calculation. Zero-damage hits do not become lethal. Immortal practice targets remain immortal. |
| `freeupgrade` | Toggles material-free upgrades. Ownership, distinct storage, capacity and maximum +2 checks remain active. |
| `nocd` | Clears existing J/K/L/O cooldowns and suppresses new ones. Active actions, dash and projectile launch guards remain. Disabling restores normal cooldowns on subsequent casts. |
| `status` / `help` | Displays current state / command reference without enabling cheats. |
| `debug off` | Disables all four switches. |

Bare commands toggle; explicit `on`/`off` forms remain supported. Rows are green with ON when enabled,
white with OFF when disabled. Invalid commands cannot change switches. Text input is limited to 80
printable ASCII characters, supports Backspace, and escapes typed colour markup.

`DebugOptions` belongs to each Player and is excluded from snapshots. Enabling any cheat permanently
blocks manual and automatic save writes for that live game, even after disabling cheats or loading a
save. Existing files remain loadable. Starting a fresh game/restarting the application creates clean
switches; loading alone does not. Debug victories still settle rewards/quests normally, but their
progress cannot overwrite either normal save slot. The window title identifies a debug-used session.

## Training and equipment checks

The stationary TrainingDummy is independent of quest Boss AI. CombatDemo fires a physical projectile
straight upward every two simulation seconds, with base damage 25, speed 180, range 720 and lifetime
four seconds. It uses the ordinary combat pipeline and existing enemy projectile art. Stand above it
to test armour, or beside it to practise safely.

The scoreboard shows dealt damage, hit count, post-mitigation incoming damage, and walking speed with
boots. Gloves reduce K/L/O cooldowns by 10% / 20% / 30%; boots increase walking speed by 10% / 20% / 30%
at base / +1 / +2. Other attributes and upgrades are detailed in the inventory report.

Training damage retains at least 1 HP for a living player. After five simulation seconds without a
positive hit, HP restores to the current equipment-adjusted maximum. Hits at 1 HP restart the timer;
zero damage does not. This protection is intentionally omitted from the status-panel text. It ends
when leaving Training. Explicit invalid/dead states are not automatically revived by recovery.

R clears score, incoming-hit feedback, attacks, cooldowns and the shot countdown, and restores HP.
It retains player position, target position and the visit's artwork. Resetting while overlapping the
target must not re-run spawn selection or cause sideways relocation. Initial map entry still checks
walkability and player overlap. Map markers read the current target, never a discarded registration.

## Automated coverage map

The latest added coverage and blocked execution are recorded in [Test coverage review](TEST_COVERAGE_REVIEW.md).
The 2026-09-28 results at the end of this document do not include subsequent working-tree tests.

Paths below are test class names under `core/src/test/java/au/edu/anu/comp2120`.

| Feature | Tests and boundary |
| --- | --- |
| Console toggles, validation, per-game defaults | `player/DebugOptionsTest`: bare and explicit commands, invalid input, disable-all, no revival, free-upgrade constraints. |
| Console entry/editing/output state | `screen/DebugConsoleTest`: processor capture/restore, repeat close/open, Backspace, length cap, ignored control characters, submit clearing, green/white markup and escaping. Native pixels and F10 delivery are manual. |
| God and one-shot damage | `player/DebugOptionsTest`, `inventory/EquipmentIntegrationTest`: actual damage delivery, enemy isolation, toggle-off, invulnerability and zero-damage cases. |
| Zero cooldown | `player/PlayerAbilitiesTest`: every slot, clearing existing timers, active-action guard, normal cooldowns after disabling. |
| Save protection | `save/GameSavesTest`, `screen/SaveMenuTest`: both files unchanged after debug victory, sticky manual block, loading does not clear protection. Victory-file test settles a Boss directly; it does not claim a complete keyboard-driven debug playthrough. |
| Sydney timestamps | `save/GameSavesTest`: AEST/AEDT and date rollover; summary leaves bytes unchanged. |
| Training damage/recovery | `screen/PortalGuideTrainingTest`: actual projectile hits and coat mitigation, fixed upward direction, 1 HP floor, repeated-hit timer, zero damage and map scope. |
| Training reset | `screen/CombatDemoTest`: repeated resets under/beside target, stable player/target positions, cleared score/attacks, fresh registration and unchanged appearance. |
| Map target selection | `screen/BossDemoTest`: all built-in battle/training targets and transition to Hub. Size/label appearance remains manual. |
| Gear tuning and persistence | `inventory/EquipmentIntegrationTest`, `inventory/GearUpgradesTest`, `player/PlayerAbilitiesTest`, `save/GameSavesTest`: actual movement, cooldowns, material accounting, ownership, cap and save round-trip. |
| Shared menu keys and save UI | `screen/MenuKeysTest`, `screen/SaveMenuTest`: aliases, confirmation ownership, persistence safety, mouse/slider behavior. NPC and item pointer hit areas remain manual. |
| Toast lifetime and layout models | `screen/SaveMenuTest`, `screen/DialogueLayoutTest`, `screen/HudLayoutTest`: expiry/repeat and layout constraints. Centred positioning/overlap is checked manually. |

## Manual regression cases (not claimed completed by automated tests)

1. At default, small and large window sizes, open F10/backtick; confirm visibility and text wrapping.
   Type each bare command twice; verify green ON then white OFF. Backspace edits; Esc closes without
   executing unfinished text. While typing WASD/E/B, the world and other menus must not act.
2. While a skill is active, enable nocd: it must complete normally before another conflicting cast.
   Disable nocd; new casts regain cooldowns. In a disposable debug game, use oneshot on a Boss:
   normal victory/reward return occurs and automatic-save feedback reports the block.
3. Navigate bag, chest and NPC choices using mouse, then only keyboard. Check small-window click
   targets and page boundaries, empty columns, final-item transfers and closing-frame input isolation.
4. Expand M in Training and Battle. Check target markers, NPC labels in Hub and label legibility
   after resizing. Return to Hub: no stale red target. Verify hidden weapon/coordinate HUDs stay hidden.
5. In Training, compare boots and coat before/after equipping. Stand above target, then step aside:
   incoming damage/HP and five-second recovery should match the rules. Pause via bag/menu mid-wait:
   recovery should wait too. Stand on target and press R repeatedly: neither actor relocates.
6. Save/load or complete NPC actions: centred notifications disappear and can repeat. Inspect both
   slot timestamps for Sydney labels. Existing files still load; confirmed deletion affects one slot.

Run automated validation from the repository root:

```bash
./Activity_2/gradlew -p Activity_2 test javadocAudit lwjgl3:classes
```

Execution on 2026-09-28 passed 1,235 core and 4 desktop tests (1,239 total), with zero failures, errors or skips, plus JavaDoc audits and desktop compilation. The documentation index records this run. No commit,
push, merge or manual acceptance is implied by this report.


## Follow-up interaction audit — 2026-09-28

- NPC-specific dialogue handlers now call `CombatDemo.synchronizePausedMap()` rather than advancing
  combat. This still binds a dialogue-triggered map transition, but does not advance projectiles,
  incoming damage or recovery timers. `CombatDemoTest.pausedMapSyncDoesNotSimulateTraining` covers
  frozen attacks/HP and cleanup on returning to Hub.
- Restoring saved cooldowns while no-CD is active clears the restored live timers after validating
  them. Disabling no-CD cannot reveal old hidden timers. The original file remains intact and a
  fresh normal game restores its original cooldowns; covered by
  `GameSavesTest.debugNoCooldownAppliesToRestoredTimers`.

Follow-up full validation: 1,237 core + 4 desktop tests (1,241 total), zero failures/errors,
plus JavaDoc audits and desktop compilation. Manual visual acceptance remains separate.
