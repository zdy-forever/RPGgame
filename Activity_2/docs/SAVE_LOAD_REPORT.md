# Save and load integration

Author: Hao Liu (u7998420)

## Integration status

Foundations: `82f6a450`; gameplay integration: `c8d14bc9`; team-main merge: `d47a1f6b`.
The first notification refinement was committed as `534cb93` and merged as `12d6da8`.
The Esc menu redesign below is the subsequent working-branch change.
See [the game README](../README.md) for launch instructions and
[the inventory report](INVENTORY_EQUIPMENT_REPORT.md) for equipment and reward rules.

## Player controls

- **Esc** opens the pause menu: Resume, Save manually, Manage saves, Restart game and music volume. Click rows or use W/S or Up/Down and E/Enter. Esc from a confirmation returns to the menu; Esc from the menu resumes. Ordinary dialogue/bag/chest/map panels close on the first Esc, before menu entry.
- **F5** remains a shortcut to save the manual slot. An existing manual file, even an invalid one, requires E/Enter confirmation before replacement. Escape cancels.
- **F9** opens the Manage saves submenu with the automatic-load row selected. **1** selects automatic, **2** selects manual. **E/Enter** confirms replacing current gameplay; **Escape** cancels.
- On macOS the keyboard may require **Fn+F5 / Fn+F9**.
- Close dialogue, inventory, chest, enlarged map, ending and credits before requesting either operation.
- Save and load require a living player in Hub or Memorial Hall. Training and Battle are excluded. Executing skills, dashes, launch windows and projectiles block both operations; inactive cooldowns are allowed and preserved.
- The timed translucent notification reports completion, blocked operations and disk/validation failures. The selection panel shows Sydney local save time (AEST/AEDT; UTC remains on disk), map and distinct guardian completion count.

## Independent slots and automatic checkpoints

Files live outside the repository at `${user.home}/.comp2120-game/saves/`:

- `automatic.properties`: replaced after each successful Boss battle, including repeat challenges.
- `manual.properties`: replaced only by an explicit manual save.

Victory autosave runs in `GameScreen.render` after combat settlement has returned the player to Hub and restored HP, inventory reward collection, equipment reconciliation and quest synchronization. There is no requirement to speak to an NPC to collect Boss rewards. Pending rewards that cannot fit an integer stack are included in the checkpoint instead of being lost.

Simultaneous player/Boss death is failure and does not autosave. Each result is observed once. Load clears result polling so a restored victory cannot immediately overwrite either slot. Startup does not write saves. Reading either slot neither modifies the other nor falls back to it. Loading an old manual save and winning again replaces the automatic slot with that timeline, even if its progress is lower.

If autosave fails, the victory and current inventory remain earned; the previous checkpoint remains on disk. The timed translucent notification reports failure. The player may manually save in the safe Hub or retry by completing another challenge. No automatic retry loop writes every frame.

## Persisted state

- Safe map ID, exact player coordinates, current and maximum HP compatibility.
- Profession, remembered ultimate mode for every profession, independent Mage element and every slot's remaining cooldown.
- Ordered inventory and chest stacks and worn slots; equipment bonuses are recomputed from them.
- Active quest and exact acceptance permission, distinct completed guardians, repeat victory counts and one-time ending consumption.
- Completed acceptance IDs for replay protection, and pending reward identities/quantities.
- Portal routes and enabled flags, fast-travel unlocks, and each registered NPC's one-time reward flag.

Map geometry, tuning, textures, combat actors, active effects and RNG state are not serialized. Loads are restricted to safe states, and combat registration is rebuilt after the validated location transition. Object identities of Player, Health, inventory, chest and ability controller are retained. Loading a portal-overlapping position requires leaving the trigger before it can activate again.

## Validation and disk behavior

`SaveFile` uses bounded Java properties with format/version markers and rejects duplicate keys. Same-directory temporary files are flushed before atomic replacement. A failed write does not replace a previous valid file; temporary files are cleaned up. Atomic replacement must be supported by the filesystem; failure is reported instead of falling back to a non-atomic overwrite.

`GameSaves` decodes every component and checks required/unknown fields, enums, finite numbers, positive item quantities, equipment ownership/slots, complete per-class selections, map collision, HP capacity, quest/win consistency, ending eligibility, reward provenance and portal permission before touching live gameplay. The staged restoration performs no disk IO or parsing. File errors and invalid data leave live gameplay unchanged.

The initial component-only prototype files are not complete gameplay snapshots and are rejected by the integrated loader. No migration is needed for released gameplay saves because this is the first playable persistence version.

## Scope of changes

New save coordination and menu files reuse existing screen drawing resources and public module APIs. Existing gameplay owners gained small explicit snapshot/restoration methods; there is no reflection, serialized Java object graph, Git operation or project restructuring. (Explicit per-slot file deletion was added later through Manage saves; see below.) Existing named-spawn travel, normal healing/damage and interactive class-selection behavior remain available.

The architecture's earlier descriptions of save/load as future work are historical. The implementation described here supersedes those persistence limitations for the states listed above. There is no implemented separate achievement subsystem to persist; guardian completion and ending progress are covered.

## Original integration validation (2026-09-23)

On the local environment, JDK 25 was unavailable. Tests used the existing temporary `/tmp/inventory-java26.gradle` toolchain override with JDK 26; no repository build configuration was changed.

```sh
./gradlew --init-script /tmp/inventory-java26.gradle test core:javadocAudit lwjgl3:classes
```

Recorded final integration run: **1,165 core tests and 4 desktop tests passed**; JavaDoc audit and desktop compilation passed. The save feature has 31 persistence tests plus one menu-input test. Coverage includes restart restoration, repeated victory checkpoints, independent timelines, unsafe-state guards, corruption without partial mutation, overflow rewards, ending consumption, write failure and menu confirmation/cancellation.

An earlier full run had one failure in `BossTraitTest.windBlinksOnBurstDamageThenCoolsDown`. The unchanged baseline passed both standalone and full execution; the current standalone test and final full run also passed. The Wind trait uses an unseeded random blink location. This intermittent failure was not suppressed or changed, and baseline reproduction of the failure was not established.

The desktop process started; UI automation could not identify the Java window. Subsequently,
the user reported passing manual checks for save/load restoration of position, bag/equipment
and chest state, including loading after restarting the game. Boss-victory autosave was not
fully manually accepted because Boss gameplay issues were being handled separately.
Automated checkpoint coverage is not a substitute for that remaining playthrough.

The earlier Wind-test failure above is historical: upstream later changed blink behavior
in `36ce30d`. This report does not assert that the old failure remains in the current code.

## Manual regression checklist

1. Launch the game and, in Hub, equip an item, move, take/retain an injured state if available, then F5. Move or change equipment and F9 → 2 → Enter; check exact restored state.
2. Press F5 again and cancel; confirm the previous manual timestamp stays unchanged. Repeat and confirm overwrite.
3. Defeat a Boss. Confirm the automatic-save notification appears immediately on settled Hub return, without an NPC interaction. F9 → 1 → Enter should restore the settled reward and quest progress.
4. Restart the application and load each slot. Verify the manual slot was not overwritten by the victory.
5. Open the menu during Battle, Training or an active skill; verify save/load rows remain unavailable. F5 is rejected, while F9 may open the menu but cannot load. Close an open dialogue/chest before retrying in Hub.
6. Inspect the load panel at the normal window size. If the OS intercepts function keys, use Fn with the function key.

The original feature is merged; use this checklist for follow-up regression testing.
Corrupt-file and failed-write cases are covered by automated tests; no additional manual
acceptance of those cases is claimed here.

## Earlier save UI refinement (superseded by the Esc menu)

The first refinement placed the save notification at the top left below the visible HP/coordinate HUD,
with a dark background and wrapping width. Font size and margins scale with the smaller
relative window dimension using the existing HUD policy, including short, wide windows.
The repeated default shortcut suffix was removed. The load/overwrite modal also uses
both-axis scaling and fits long summaries within its panel.

A blocked-operation hint clears when its blocking condition disappears (for example,
closing a dialogue after pressing F9), or after four seconds of UI time. Another blocked
request refreshes that duration. This supersedes the earlier references to a footer.

On 2026-09-24, the targeted SaveMenu, HudLayout and save-package run passed 62 tests;
core JavaDoc audit and desktop compilation also passed with the local JDK 26 override.
These are targeted results, not a new full-suite run. Regression checks cover clearing after panel closure, timeout, invalid timer deltas,
normal/narrow/short window layouts, confirmation/cancellation and the persistence tests.
Manual visual check: press F9 during dialogue, close the dialogue, then resize the game
both horizontally and vertically; verify the notification remains below the upper-left
HUD and no longer overlaps bottom dialogue or skill text.

## Starting fresh and current limits

Starting the application creates a fresh live game without deleting or loading either slot.
To keep that new run, save manually and confirm replacement of the manual slot; its first
successful Boss battle will replace the automatic slot. Until those writes, older files
remain available. The Esc menu now offers Restart game with explicit confirmation. It rebuilds
player/map/quest/combat state and starter chest contents, resets NPC reward flags, and disposes
the old screen after its render completes. Music volume carries over. It performs no save-file
read, write or deletion; users can still load either older slot afterwards. There is no single
reset-both-slots command, but each slot can be deleted individually from Manage saves (see below).

Saving only supports Hub and Memorial Hall, not every non-combat location: Training is
excluded even when idle. Arbitrary mid-battle recovery, live Boss HP/AI, current skill effects,
training score, RNG continuation, multiple profiles and migration of future format versions
are outside this implementation. Loading intentionally restores the selected timeline;
it does not merge progress or enforce a highest-progress checkpoint.

## Re-running focused checks

From Activity_2 with the configured JDK 25 installed:

```sh
./gradlew core:test --tests 'au.edu.anu.comp2120.save.*' --tests '*SaveMenuTest' --tests '*HudLayoutTest' --tests '*EquipmentIntegrationTest'
./gradlew core:javadocAudit lwjgl3:classes
```

The temporary JDK override used for historical local verification is not a repository file
or a required setup step. Commands above are reproduction instructions, not results from
this documentation update. Full test reports are generated under each module's
`build/reports/tests/test/`; their counts depend on the selected suites and checkout.

## Pre-merge verification (2026-09-25)

Before committing the UI/documentation follow-up, ran `test javadocAudit lwjgl3:classes`
with the same temporary JDK 26 override. Core tests: **1,184 passed**; desktop tests:
**4 passed**, with zero failures, errors or skips. The test tasks executed; compilation
and the core JavaDoc audit were up-to-date, and the desktop JavaDoc audit executed successfully.
This does not replace the outstanding manual visual/Boss-checkpoint checks above.

## Esc menu redesign

The permanent status strip is removed. GameSaves publishes an operation sequence alongside
its message; SaveMenu consumes each event once. Successful manual saves, loads, autosaves
and failures all produce four-second toasts with a 0.6-second fade. Repeating the same action
produces a fresh event even if the text is identical. Startup shows no stale status, and an
expired notification never falls back to the coordinator's last message. Notifications use
alpha blending, wrapped text and both-axis scaling. They are centred horizontally and vertically in the game
window for gameplay, dialogue and pause menus alike.

The centred menu is scaled from a 500 × 460 layout using both dimensions. Drawing and mouse
hit-testing share the same transform. Opening, closing and confirmation frames consume input;
world animation and simulation pause while the menu is open. The menu is available in Battle
and Training for resume, volume and restart, but pause cannot bypass GameSaves.isSafe().
Existing confirmations still protect manual-file overwrites and replacement of live progress.

Music gain is an application preference, not save content. The 0–100 slider supports click/drag
and one-percent A/D or Left/Right steps when focused. It updates all cached music streams and future
tracks without restarting playback. Zero mutes. Menu pause leaves music playing for preview;
application lifecycle pause still pauses it. Gain survives an in-game restart, but is not
persisted after quitting the application.

Verification: the complete local test run passed **1,191 core tests and 4 desktop tests**, with
zero failures, errors or skips. Both JavaDoc audits and desktop compilation passed using the
existing temporary JDK 26 override. Menu tests cover Escape ownership, paused unsafe maps,
confirmation/cancellation, unchanged slot bytes on load/restart request, repeated identical
notifications, expiry, and keyboard/resized mouse slider input. Music tests cover current,
cached and future streams, pause, invalid values and disposal. Restart callback/file invariants
are tested without OpenGL; a complete fresh-screen restart still needs the manual check below.
The desktop launch task completed without an exception; native visual acceptance is not claimed.

Manual checks for this redesign:

1. Esc opens the menu and stops player/Boss movement; Resume/Esc restores play without a time jump.
2. In Hub, save, move and load manually. Confirm the toast disappears after four seconds. Repeat
   an identical save/load and check that feedback appears again; shrink the window in both axes.
3. In Training and Battle, verify save/load remain unavailable even while paused. Confirm the
   training score has no permanent save notification over it.
4. Drag volume to 0, 50 and 100; also use arrows. Travel/load and check the chosen gain remains.
5. Cancel Restart game once, then confirm it. Verify fresh Hub, full HP, initial class, empty bag,
   starter chest, cleared quests and merchant reward availability. Both prior saves must remain
   selectable; loading restores old progress. Confirm music volume is retained.
6. Open dialogue/bag/chest/map; Esc closes that panel, and another Esc opens the menu.


## Save management submenu

Esc → Manage saves (or F9) groups Load automatic, Delete automatic, Load manual,
Delete manual and Back. Each load/delete requires confirmation. Esc cancels a confirmation
back to this submenu, returns from the submenu to the main menu, then resumes gameplay.
Deletion is permanent and names the selected slot. It changes neither the live session nor
the other slot. Slot summaries refresh after deletion; feedback expires after four seconds.
Missing files report an already-empty slot; malformed files can be deleted without loading.
Filesystem errors are reported without resetting gameplay. Directory paths are never deleted.
Saving, loading and deleting require the existing safe Hub/Memorial state; pausing combat
never bypasses that guard. Deleting automatic storage does not disable future Boss autosaves.
Restart game still preserves both files. Tests use temporary directories only.

Manual check: open Manage saves, cancel deletion once, then confirm deletion of a disposable
slot. Verify the other slot still loads, the current player is unchanged, and Esc navigates
back one level. Repeat with the other slot and an empty slot.

Validation after adding slot deletion: 1,198 tests passed (1,194 core and 4 desktop),
with zero failures/errors; JavaDoc audits and desktop compilation also passed using the
same JDK 26 override. Added checks cover cancel/confirm for both slots, malformed and
missing files, directory refusal, unsafe-map rejection, live-state preservation and
mouse interaction at a reduced window size. Visual in-game acceptance remains manual.


## Inventory combat pause

The bag now skips combat simulation updates while open, instead of only substituting an
empty player command. Boss/enemy actions, projectiles and skill timers therefore pause along
with world animation. Inventory input remains active; closing with B/Escape resumes normal
frame updates without accumulating paused time. Battle-map save/load restrictions remain.
Manual check: open the bag during a Boss attack, wait several seconds and verify HP, enemy
positions and projectiles remain stationary; close it and verify combat resumes.


## Equipment upgrade persistence

The catalogue recognises base IDs and bounded `+1`/`+2` variants. Existing inventory snapshot
fields preserve upgraded item identity, quantity, chest contents and worn slot without a
new file-format version. Base IDs remain level zero. Worn invalid variants are rejected by
existing catalogue validation. Upgrade level travels with each item, rather than a global
slot bonus. Upgrade services do not write files; manual and automatic slots remain independent.


### Headgear capacity compatibility

Full-load validation computes expected HP capacity from the saved worn items before touching
live state. Inventory/equipment restores before exact location/HP. Older snapshots recording
base maximum HP with a hood are accepted and retain their saved current HP; other mismatched
capacities are rejected before mutation. Boots and glove modifiers are derived from worn IDs;
remaining cooldowns restore exactly without a second reduction.

Save-slot summaries display `yyyy-MM-dd HH:mm:ss` in `Australia/Sydney`, including AEST/AEDT according to daylight saving rules. Metadata remains a UTC Instant on disk, so existing saves need no migration and viewing a summary never rewrites a file.


## Debug-session save protection

Enabling any console cheat blocks both manual and automatic writes for the remainder of that live
game. Disabling cheats or loading a file does not remove the block. A fresh game/player restores
normal saving. Reads and existing slot files remain available; normal victory and reward processing
still occurs. `GameSavesTest.debugVictoryPreservesBothSaveFiles` and
`SaveMenuTest.debugSaveProtectionIsSticky` verify file preservation. See the
[consolidated coverage report](DEBUG_TRAINING_UI_REPORT.md) for console and UI validation boundaries.
