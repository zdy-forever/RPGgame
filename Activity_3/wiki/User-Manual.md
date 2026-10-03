# User Manual

[Home](home) · [Installation and Setup](installation-and-setup) · [Features](features)

## Main menu

The application opens a title menu before entering a game. Use W/S or Up/Down and E/Enter, or click a row.

- **New Game:** start a fresh Hub session without deleting existing automatic/manual saves.
- **Continue:** select the most recently timestamped readable save slot and attempt to load it. The option is disabled when neither slot is readable. Use in-game Manage saves when you want to choose a particular slot yourself.
- **Settings:** change music volume or fullscreen. These title-menu preferences are stored in `.comp2120-game/settings.properties` under the Java user's home directory.
- **Quit:** close the application.

## First playthrough

1. Select New Game in the title menu to enter Hub. Move with WASD and approach an NPC; E interacts.
2. Visit Weapon Merchant, choose the weapon service and collect the staff. Open B, select it and confirm to equip.
3. Open Storage Chest, transfer armour and potions, then press B to equip armour from the bag. Transferring an item is not equipping it.
4. Choose a class and O ultimate mode at Class Master.
5. For practice, select Training Ground at Portal Guide and walk into the portal. The exit NPC returns you to Hub.
6. Accept a guardian at Quest Keeper. At Portal Guide, select Battle and confirm, then enter the portal.
7. Fight with J/K/L/O and use P for a potion. Victory returns you to Hub and creates an automatic checkpoint.
8. Use earned cores to upgrade gear at Weapon Merchant. Complete the five distinct guardian quests and visit Old Sage in Memorial Hall for the ending.

## Controls

| Context | Controls |
| --- | --- |
| Move / face | WASD |
| Interact | E near an NPC or chest |
| Combat | J basic attack; K class action; L dash; O selected ultimate |
| Potion | P during gameplay; heals up to 30 HP, does not consume at full HP |
| Bag | B |
| Expanded map | M; M/Esc closes |
| Pause menu | Esc after closing other modal interfaces |
| Manual save / save management | F5 / F9 |
| Class shortcuts | F1 Archer, F2 Mage, F3 Katana, in Hub |
| Practice reset | R in Training |
| Debug console | F10 or backtick; Enter executes; Esc closes |

On macOS, Fn may be needed with function keys. Use Class Master for ultimate selection: F5 is reserved for saving.

## Menus and items

Use W/S or Up/Down to select, E/Enter to confirm, and Esc to return/close. Pause-menu rows can be clicked; NPC choices support left-click selection and right-click activation. During dialogue/menu interaction, navigation keys do not move the player.

In the bag, left-click selects and right-click uses/equips/unequips. E/Enter activates the selected row. `[Worn]` identifies equipped gear. Q/R or PageUp/PageDown pages; footer controls are clickable.

Storage uses A/D or Left/Right to choose bag/chest, W/S or arrows to choose an item, and E/Enter or right-click to transfer the whole stack. Storing the last copy of worn equipment removes its bonus. B switches from storage to bag; Esc closes storage.

## Saving and restarting

- Save manually through Esc or F5. Confirm before replacing an existing manual slot.
- Manage saves offers separate Load and Delete actions for both slots. Loading restores that snapshot without rewriting either file.
- Save/load requires Hub or Memorial Hall, a living player, and no active action/projectile. Close dialogue, bag, chest and map first. Remaining cooldowns are allowed.
- Restart game creates a fresh live game while keeping both disk saves and current music volume. A later save or victory may overwrite its corresponding slot.
- Delete removes only the confirmed slot; it does not reset the current game.
- Launching shows the title menu; select Continue to attempt the latest readable slot or New Game for a fresh session. Loading is an explicit choice.

## Training and equipment checks

Stand above the target to receive its fixed upward shots; stand beside it to practise attacks. Compare incoming damage with and without the coat, walking speed with and without boots, and K/L/O cooldowns with and without gloves. R clears practice metrics, attacks and cooldowns and restores HP while retaining positions. Training grants no Boss rewards or quest completion.

## Music

In Esc, drag the volume slider or select it and use A/D or Left/Right. The range is 0–100. Music continues during the menu for preview. This volume survives in-game restart/load, but the Esc slider does not write the settings file. To retain a startup preference, set it in the title menu's Settings screen. Without a stored preference, music defaults to 50%.

## Implementation references

Reviewed against source `59de4e6` on 2 October 2026. Older dated report sections may describe earlier controls or startup behavior.

- [Main menu implementation](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/core/src/main/java/au/edu/anu/comp2120/screen/MainMenuScreen.java) and [Continue slot selection](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/core/src/main/java/au/edu/anu/comp2120/save/SaveSelection.java).
- [Inventory/equipment behavior](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/INVENTORY_EQUIPMENT_REPORT.md).
- [Save/load integration and restrictions](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/SAVE_LOAD_REPORT.md).
- [Debug and Training behavior](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/DEBUG_TRAINING_UI_REPORT.md).
- [Persisted settings](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/core/src/main/java/au/edu/anu/comp2120/screen/GameSettings.java).

## Illustrated walkthrough and source map

See [Playtest and Screenshots](Playtest-and-Screenshots) for the title menu, inventory, five-guardian checkpoint, Hall ending and Credits. [Quest/portal report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/QUEST_PORTAL_FLOW_REPORT.md) documents the precise progression; [player defeat report](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/PLAYER_DEFEAT_REPORT.md) explains loss and retry. [Documentation Index](Documentation-Index) links every supporting feature report.
