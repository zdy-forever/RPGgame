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

Training displays delivered damage, hit count, walking speed and incoming damage. Its stationary target fires upward practice shots, allowing armour checks. A living player retains at least 1 HP and recovers fully after five simulation seconds without a positive hit. R resets practice without relocating the player or target.

The debug console offers damage immunity, one-shot damage, free upgrades and zero cooldowns. Using a cheat blocks save writes for that live session, protecting normal saves. See [Game Tester](game-tester).

## Visual evidence

Five existing screenshots below demonstrate Hub services, map labels, inventory, upgrades and save management. Their capture commit/date was not recorded, so they should not be treated as proof of the latest build or a completed manual regression run. Additional evidence still needed: title menu, Boss combat/elemental reactions, Memorial Hall ending, Training and debug console.

### Hub and NPC services

The Hub exposes NPC services, storage, the portal, health, class state and ability controls in one playable scene.

![Hub gameplay](uploads/49dd769f14ed3b5096087caefc5978ee/clipboard.png){width=900}

### Expanded map

The expanded map labels NPCs and services so players can identify destinations at different window sizes.

![Expanded Hub map with NPC labels](uploads/ab44df0ea294303c87fb346a4e8d5291/clipboard.png){width=900}

### Equipment and progression

The bag shows equipped slots, upgrade levels, shared materials and set bonuses.

![Equipped inventory and set bonuses](uploads/49f7be2734981b642e0b538ea4baa1ba/clipboard.png){width=900}

The merchant upgrade page displays the shared sentinel-core pool, current effects and the +2 cap.

![Weapon Merchant equipment upgrade page](uploads/d1aa3666eb92457579ddf9efa4e53afa/clipboard.png){width=900}

### Save management

The pause-menu save manager provides independent automatic and manual slots, load/delete actions and Australia/Sydney timestamps.

![Save management menu](uploads/ab9835f62cb0e185e8567a3da08f5827/clipboard.png){width=900}

## Implementation references

Source links are pinned to `1a2c60c` for reproducibility; dated sections retain their original verification scope.

- [Current game README](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/README.md): controls, game systems and setup.
- [Inventory and equipment](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/docs/INVENTORY_EQUIPMENT_REPORT.md): item ownership, stat bonuses, potions and merchant upgrades.
- [Save/load integration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/docs/SAVE_LOAD_REPORT.md): independent slots, safe-state restrictions and restoration.
- [Guardian traits](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/docs/BOSS_TRAIT_REPORT.md): five guardian mechanics and provisional balance.
- [Debug, Training and UI](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/docs/DEBUG_TRAINING_UI_REPORT.md): practice rules, commands and verification boundaries.
- [Art integration](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/1a2c60c2b7602e2501d86ac380c364b1d704b175/Activity_2/docs/ART_INTEGRATION.md): imported artwork and procedural presentation.
