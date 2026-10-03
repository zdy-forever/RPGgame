# Save, load and settings

Reviewed against `7fea86a` on 2026-10-02. Save integration was implemented by Hao Liu
(`u7998420`), with subsequent team changes. Current code:
[GameSaves](../core/src/main/java/au/edu/anu/comp2120/save/GameSaves.java),
[SaveMenu](../core/src/main/java/au/edu/anu/comp2120/screen/SaveMenu.java),
[GameSettings](../core/src/main/java/au/edu/anu/comp2120/screen/GameSettings.java).

## Startup and controls

Startup displays New Game, Continue, Settings and Quit. New Game creates fresh live state
without reading, writing or deleting a slot. Continue chooses the newest readable slot by
timestamp and attempts full validation/restoration; it is disabled if neither slot is readable.
A readable metadata record is not a guarantee that the complete snapshot can be restored.

Esc opens Resume, Save manually, Manage saves, Restart game, Settings and Quit game. Settings
contains the same persisted music-volume slider and fullscreen toggle as the title menu. Quit
requires confirmation and leaves both slots/settings unchanged. Use W/S or Up/Down, E/Enter or
mouse selection; A/D or Left/Right adjusts the focused setting. Esc first closes ordinary
dialogue/bag/chest/map panels; a later press opens the pause menu. A confirmation cancels back
one level.

- **F5** requests manual saving. Replacing an existing manual file, including a corrupt file,
  requires confirmation. F5 does not select an ultimate mode.
- **F9** opens Manage saves. **1** selects automatic load and **2** manual load. Confirm the
  operation to replace the live timeline. Each slot also has a separate confirmed Delete action.
- Save/load/delete require a living player in Hub or Memorial Hall with no executing skill,
  dash, launch window or projectile. Inactive cooldowns are allowed. Training and Battle remain
  unsafe even while paused. Close other modal panels before requesting operations.
- Function keys may require Fn on macOS.

## Independent files and victory checkpoints

Files are stored outside the repository:

```text
${user.home}/.comp2120-game/
  settings.properties
  saves/
    automatic.properties
    manual.properties
```

Only explicit manual saving replaces `manual.properties`. Successful admitted Boss returns,
including rematches, attempt `automatic.properties` after full-HP Hub recovery, reward collection,
equipment reconciliation and quest synchronization. Speaking to an NPC is not required.
A pending reward whose stack cannot yet fit remains in the checkpoint rather than being lost.

Failure, including simultaneous Player/Boss death, does not autosave. Results are observed once;
loading clears result polling so it does not immediately produce a second checkpoint. Loading a
slot does not write either file or merge progress. Winning after loading an older manual timeline
can replace the automatic slot with that older timeline's new progress.

A failed automatic write leaves the earned live victory/reward and previous checkpoint intact.
The error is shown once; there is no per-frame retry loop. An explicit safe manual save or a
later successful challenge can create another checkpoint.

## Persisted state

| State | Restoration contract |
| --- | --- |
| Location/HP | Exact safe map/coordinates and current HP; capacity is checked against worn gear |
| Class/abilities | Profession, every class's O mode, Mage element and each slot's remaining cooldown |
| Items | Ordered bag/chest stacks and worn slot IDs, including +1/+2 variants; bonuses recomputed |
| Quests | Active target/permission, distinct completions, per-guardian win counts, ending consumption and Hall guidance flag |
| Outcome/rewards | Completed acceptance IDs and pending reward identities/quantities for replay protection |
| Travel/NPCs | Portal routes/enabled flags, internal fast-travel unlocks and registered NPC interaction/reward flags |

Player, Health, bag, chest and ability-controller identities are retained during a validated
load. Combat registration is rebuilt for the restored map. A saved position overlapping a
portal cannot immediately trigger it; the player must leave the trigger first.

Live Boss HP/AI, attacks, fields, buffs, RNG continuation, Training score, debug switches,
textures and music preferences are not snapshot content. Loading restores an exact selected
timeline, not a highest-progress checkpoint. There is no mid-battle or multi-profile save UI.

## Validation and disk behavior

SaveFile uses bounded Java properties with format/version markers and duplicate-key rejection.
SaveStore writes/flushed temporary files in the same directory and replaces atomically. If atomic
replacement is unsupported, the save fails rather than falling back to an unsafe overwrite.
Failed writes retain the previous file and clean up temporary files.

GameSaves stages component decoding and cross-checks before changing live state: required and
unknown fields, numeric bounds, enums, map collision, item quantities/ownership, HP capacity,
per-class modes/cooldowns, quest/win consistency, ending eligibility, reward provenance and
portal permission. The optional Hall-guidance flag defaults to false when absent. Older
snapshots that recorded base maximum HP with a hood retain saved current HP while adopting
the computed capacity; unrelated mismatched capacities are rejected.

Unreadable/invalid snapshots produce feedback and leave live state unchanged. Component-only
prototype files are not complete gameplay saves and are rejected. No general format migration
or arbitrary future-version compatibility is promised.

## Delete, restart and debug sessions

Manage saves provides Load automatic, Delete automatic, Load manual, Delete manual and Back.
A confirmed deletion removes only the named slot, changes no live progress and leaves the other
slot untouched. Malformed files can be deleted; missing files report an empty slot; directories
are refused. Safe-state guards still apply. Deleting the automatic slot does not disable future
automatic saves.

Restart game requires confirmation, creates a fresh Player/maps/quests/combat and starter chest,
resets NPC flags and disposes the old screen after rendering. It retains the current music gain
and both disk slots. Old progress remains available until an explicit deletion/overwrite or new
victory checkpoint.

Enabling `god`, `oneshot`, `freeupgrade` or `nocd` sets a sticky per-game save-write block.
Disabling cheats or loading does not clear it. Loading existing files remains possible and
ordinary quest/reward settlement still occurs. New Game/restart creates clean debug state.
With nocd enabled, validated restored cooldowns are cleared in live state without rewriting the file.

## Settings and feedback

Title Settings and Esc → Settings persist the same `music.volume` and `fullscreen` values in
`settings.properties`, independent of progress files. Defaults are 50% volume and windowed mode.
Both entry points immediately apply changes. Neither save slot controls volume/fullscreen.

Volume applies to current/cached/future tracks without restarting playback. Menu pause leaves
music playing; application lifecycle pause pauses it. Settings write failures are ignored by
GameSettings; gameplay save failures instead show explicit feedback.

Save summaries use `Australia/Sydney` local date/time with AEST/AEDT; disk metadata is a UTC
Instant. Summarizing does not rewrite a file. Operation messages use centred translucent
four-second toasts, fading for the final 0.6 seconds. Repeated identical operations produce
fresh notifications; expiry does not redisplay stale status.

## Verification

Run from Activity_2:

```sh
./gradlew core:test --tests 'au.edu.anu.comp2120.save.*' \
  --tests '*SaveMenuTest' --tests '*EquipmentIntegrationTest' \
  --tests '*GameSettingsTest' --tests '*MainMenuScreenTest'
```

GameSavesTest covers independent slots, restart restoration, victory checkpoints, unsafe states,
corruption without partial mutation, pending rewards, debug file protection, restored rematch
scaling and equipment/cooldown compatibility. SaveFile/SaveStore tests cover parsing and file
failure boundaries. Menu tests exercise confirmation, deletion, restart callbacks, input ownership,
volume and notification lifetime. These are automated models/fixtures, not native screen acceptance.

The [tester guide](TEST_COVERAGE_REVIEW.md#recorded-result) contains the verified CI result.
[Native checks on 2026-10-03](PLAYTEST_REPORT.md) verified F5 save/overwrite, load confirmation
and cancellation, pre-dash position restoration, equipment restoration, battle save rejection,
victory autosaves through five distinct wins with independent manual history, AUTO loading, application relaunch,
Continue selection, title settings persistence and saved/reloaded Hall ending completion.
Wind was completed by the user without debug features. Some later scenarios used position-only
fixtures; these are disclosed in the report. Still verify native slot deletion/corruption paths,
full resize behavior and all campaign/rematch restoration paths.
