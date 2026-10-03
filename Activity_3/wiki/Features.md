# Features

[Home](home) · [User Manual](User-Manual) · [Game Engine](game-engine)

## Movement and Hub

Movement supports eight directions with normalized diagonals, so diagonal travel is not faster than horizontal or vertical travel. Swept collision makes the player slide along walls rather than pass through or stick to them. L performs the class-specific collision-safe dash.

The Hub uses a road, boundary fence, huts, trees, water and name tags to make its services readable. Interaction prompts change with the target, such as talking, reading and opening. Its NPCs cover quests, portal destinations, class/ultimate selection, merchant services, guidance and the ending flow; the storage chest is a separate item interface.

## Campaign and exploration

The Hub contains NPC services, a storage chest and one configurable portal. Quest Keeper offers five guardian quests; Portal Guide selects Training, the accepted battle destination, or Memorial Hall. Accepting a quest does not automatically open the portal. Confirm the destination and then walk into it.

Each battle arena contains one two-phase Boss. Victory grants rewards, records quest progress, restores health and returns the player to Hub. Defeat also returns the player at full health, without granting victory progress. Completed guardians can be challenged again; repeat victories do not add duplicate distinct guardian stones.

Memorial Hall displays progress toward five distinct victories. After all five, speaking to Old Sage starts the ending. Its completion flag is saved.

## Classes and combat

| Class | Basic attack / special | Ultimate choices |
| --- | --- | --- |
| Archer | Straight arrow and five-arrow fan | Arrow rain or temporary untargetability |
| Mage | Selected-element bolt; K cycles elements | Prepared elemental field or rapid randomized bolts |
| Katana | Facing-based slash and parry/counter | Counter stance or thrust with follow-up |

All classes have a collision-safe dash. Class Master changes class and ultimate choice in Hub. Combat includes elemental attachment, damage reactions, Freeze, Overload and Swirl. Walls obstruct attacks; skills, projectiles and reaction damage use the shared combat rules.

The five guardians differ in their mechanics: Fire applies burning stacks; Water pushes the player; Lightning punishes a long interval without taking damage; Ice freezes after repeated hits; Wind responds to burst damage with a bounded blink and temporary immunity.

## Inventory and upgrades

The bag supports equipping, unequipping and potion use. Storage transfers stacks between the bag and chest. Initial chest contents provide four armour pieces and three potions for testing. Weapon Merchant grants a Lightning staff once, replenishes the combined bag/chest potion supply to three when below that amount, and upgrades eligible carried or worn equipment using shared sentinel cores.

Each upgrade costs one core and each item stops at +2. The material pool is shared across equipment types.

| Equipment | Base | +1 | +2 |
| --- | --- | --- | --- |
| Lightning staff | +20% attack | +25% | +30% |
| Guardian hood | +20 maximum HP | +30 | +40 |
| Guardian coat | 8% damage reduction | 10% | 12% |
| Guardian gloves | 10% K/L/O cooldown reduction | 20% | 30% |
| Guardian boots | +10% walking speed | +20% | +30% |

Two guardian armour pieces add 5% damage reduction; four also add 10% attack. Hood changes preserve the health proportion, preventing repeated equip/unequip actions from providing free healing. Armour slots determine bonuses, not body-part hit locations.

## Save management and presentation

Manual and automatic slots are independent. Manual saving and loading require a living, idle player in Hub or Memorial Hall. Successful Boss settlement creates an automatic checkpoint. Pause does not allow saving in a battle or Training.

The Esc menu provides saving, slot loading/deletion, restarting and music volume. Confirmations protect overwrite, load, deletion and restart operations. Slot times are displayed in Australia/Sydney, including AEST/AEDT; stored timestamps remain UTC.

Short-lived translucent notifications appear centrally and fade away. The enlarged map identifies NPCs and live targets; the minimap marks the player and Boss/Training target. Bag, storage, dialogue and pause interfaces stop simulation. Keyboard and mouse options share the same item/menu actions.

## Testing tools

Training is stacked below the actual Objectives panel height, including wrapped text. Training displays delivered damage, hit count, walking speed and incoming damage. Its stationary target fires upward practice shots, allowing armour checks. A living player retains at least 1 HP and recovers fully after five simulation seconds without a positive hit. R resets practice without relocating the player or target.

The debug console offers damage immunity, one-shot damage, free upgrades and zero cooldowns. Using a cheat blocks save writes for that live session, protecting normal saves. See [Game Tester](game-tester).

## Visual evidence

These are real desktop captures from 3 October 2026. See [Playtest and Screenshots](Playtest-and-Screenshots) for capture versions, test preparation, all-five completion, Hall ending, Credits and remaining acceptance. Screenshots demonstrate the displayed state, not exhaustive behavior or coverage.

### Hub and exploration

![Hub with NPC services and configurable portal.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/04-hub.jpg)

Hub with NPC services and configurable portal.

![Expanded map identifies Hub services.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/11-expanded-map.jpg)

Expanded map identifies Hub services.

### Equipment and saving

![Four equipped armour pieces show HP 120/120, DEF 13% and Set 4/4.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/09-equipped-inventory.jpg)

Four equipped armour pieces show HP 120/120, DEF 13% and Set 4/4.

![Merchant upgrades show available equipment and shared materials; this capture records an attempted upgrade without sufficient cores.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/12-merchant-upgrades.jpg)

Merchant upgrades show available equipment and shared materials; this capture records an attempted upgrade without sufficient cores.

![After the final Wind victory, AUTO records 5/5 guardians while the independent older MANUAL records 4/5.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/27-five-guardians-autosave.jpg)

After the final Wind victory, AUTO records 5/5 guardians while the independent older MANUAL records 4/5.

### Training layout

![The fixed Training panel is below the wrapped Objectives panel, with a visible gap.](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/raw/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/screenshots/41-training-layout-fixed-resized.jpg)

The fixed Training panel is below the wrapped Objectives panel, with a visible gap.


## Implementation references

Source links are pinned to `59de4e6` for reproducibility; dated sections retain their original verification scope.

- [Current game README](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/README.md): controls, game systems and setup.
- [Inventory and equipment](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/INVENTORY_EQUIPMENT_REPORT.md): item ownership, stat bonuses, potions and merchant upgrades.
- [Save/load integration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/SAVE_LOAD_REPORT.md): independent slots, safe-state restrictions and restoration.
- [Guardian traits](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/BOSS_TRAIT_REPORT.md): five guardian mechanics and provisional balance.
- [Debug, Training and UI](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/DEBUG_TRAINING_UI_REPORT.md): practice rules, commands and verification boundaries.
- [Art integration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/ART_INTEGRATION.md): imported artwork and procedural presentation.

[Full class skills](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/FINAL_CLASS_SKILLS_REPORT.md) · [Element reactions](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/ELEMENT_REACTION_COMPLETION_REPORT.md) · [Quest, Hall and Credits flow](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/QUEST_PORTAL_FLOW_REPORT.md) · [Native results and limitations](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_2/docs/PLAYTEST_REPORT.md)
