# User Stories

[Home](home) · [Planning and Schedule](Planning-and-Schedule)

The full story catalogue is maintained in [Activity-1-User-Stories.md](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity-1-User-Stories.md). The [work plan](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/blob/59de4e66120654605150c8baad4ca49dea39224f/Activity_1/1.2.md) maps story IDs to tasks, dependencies, estimates and issue references.

## Delivered player journeys

| Story area | Player goal | Example acceptance evidence |
| --- | --- | --- |
| F1 Movement | Move and dash without passing through walls | Collision and player movement tests; walk against an arena wall |
| F2 Maps | Travel to the intended destination and understand location | Quest-gated portal tests; select Training and enter portal |
| F3 Bosses | Fight distinct encounters and recover after defeat | Boss trait/phase tests; defeat-return-retry scenario |
| F4 Inventory | Store, equip and use owned items | Equipment tests; chest-to-bag transfer then equip |
| F5 Combat | Use classes and elemental interactions consistently | Class ability/reaction/occlusion tests; Mage element cycle |
| F6 Persistence | Resume progress after restarting | Save snapshot tests; manual save/app restart/load |
| F7 NPCs | Understand and choose services through dialogue | Quest/portal service tests; NPC choice navigation |

Later refinements include merchant upgrades, potion replenishment, save management, unified menu controls, Training diagnostics and debug tools. These extend the implemented journeys; they should not be retroactively described as requirements planned before their actual introduction.

## Scope changes

The architecture records achievements and NPC following as dropped, general crafting/enchanting as reduced to merchant upgrades, and guided tutorial flow as reduced to NPC/Notice Board guidance. The coordinate HUD was implemented during development and later removed during presentation cleanup. Refer to [Known Issues and Future Work](Known-Issues-and-Future-Work) for current limits rather than treating all original stories as delivered unchanged.
