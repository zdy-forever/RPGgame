# Inventory and equipment integration

Implemented by Hao Liu (u7998420) on `feature/inventory-equipment`.

## Integration status

Inventory/equipment implementation: `21de2898`; merged into the team repository through
!38, merge commit `b3ac158b`. Later save/load integration persists bag, chest and equipped
slots together; see [Save/load](SAVE_LOAD_REPORT.md). The P potion shortcut was added by
the subsequent team change `721f9e6`, using the same potion rules.

## Behaviour

- The existing player now owns the bag and equipment, preserving their identity across map changes.
  GameScreen reuses that bag, the existing merchant reward, and the existing chest interface.
- B opens the bag. Right-click a wearable to equip/unequip it, or a health potion to use it.
  Left/Right changes pages; B or Escape closes it. Rows show item effects and `[Worn]` status.
- P uses one health potion directly during gameplay, including Boss battles, without opening the bag.
  It triggers once per press, not continuously while held; it is disabled while a modal panel is open.
  The existing bag right-click action remains available. Both paths use the same health/ownership checks.
- The bag footer shows total attack/reduction percentages and guardian set progress.
- E near the Storage Chest opens it. Right-click transfers the entire selected stack.
  Left/Right pages both columns; E or Escape closes it. B closes storage and opens the bag
  for equipping. The footer explicitly distinguishes transfer from equip. Moving the last owned unit of worn gear
  to the chest removes its equipment benefit. Equipped items remain part of bag counts.
- The initial chest contains four guardian armour pieces and three health potions. These are
  provisional starter contents so the feature can be exercised without a new crafting system.
- Weapon Merchant at Hub (650,250) grants the Lightning staff once: E opens/advances the
  dialogue and collects the reward. Close dialogue, press B and right-click the staff to equip.
- The WEAPON HUD shows only the weapon slot. `No weapon equipped` does not mean the bag is
  empty or that armour failed to equip; check `[Worn]` rows and bag totals for armour.
- Settled Boss rewards are consumed from EncounterFlow's existing outbox on the game-loop thread.
  Overflow is checked before claiming. Unaccepted rewards remain pending; repeated polling cannot
  duplicate them. Existing material IDs, quantities and quest ownership remain unchanged.
- The shared CombatSystem applies equipment after reaction/physical modifiers and floors once.
  This covers direct, projectile and reaction damage through the same pipeline. No equipment
  means multipliers of one, retaining previous combat behaviour. Equipment is read at hit settlement.
- A potion heals up to 30 HP and consumes exactly one unit only for a living, injured player.
  It neither revives a dead player nor consumes a potion at full health.
- Inventory additions reject integer overflow without changing the previous stack quantity.

## Provisional balance and boundaries

The project documents describe equipment slots and set bonuses, but do not supply final values.
These initial values are centralized in ItemCatalog and Equipment for review:

| Item / set | Effect |
| --- | --- |
| Lightning Staff | +20% attack damage |
| Guardian hood | 4% damage reduction |
| Guardian coat | 8% damage reduction |
| Guardian gloves | 4% damage reduction |
| Guardian boots | 4% damage reduction |
| Two guardian pieces | Additional 5% damage reduction |
| Four guardian pieces | Additional 10% attack damage; retains two-piece bonus |

Reduction is capped at 60%. The full current set provides 25% reduction.
Slots are weapon, head, body, hands and feet. Wearables are not profession-restricted in this version;
existing class/element/skill selection and character artwork remain unchanged.

The original inventory change introduced no crafting, enchanting or new NPC. The existing
text/shape UI is reused; equipment artwork and final balance remain future polish.
Save/load now restores bag, chest, worn slots and NPC reward flags together, replacing initial
contents rather than adding starter items again. Equipment bonuses are recomputed from restored
ownership and slots, not trusted as independent saved totals.

## Files

Added production files:

- inventory/EquipmentSlot.java
- inventory/ItemCatalog.java
- inventory/Equipment.java
- inventory/InventoryRewards.java
- combat/EquipmentModifiers.java

Modified production files:

- inventory/Inventory.java: overflow-safe stack addition and documentation.
- player/Player.java: persistent bag/equipment, shared combat modifier interface and potion use.
- combat/CombatSystem.java: equipment multipliers inside the existing damage calculation.
- screen/GameScreen.java: bag binding, reward adapter, starter chest, equipment/potion input,
  effects display, paging and panel sizing.

Added tests: core/src/test/java/au/edu/anu/comp2120/inventory/EquipmentIntegrationTest.java.
New and modified Java declarations include JavaDoc; existing authors are retained.

## Original verification (2026-09-23)

These results describe the original inventory integration, not the current full-suite status.
Subsequent team and save/load changes supersede that baseline; do not carry the historical
11 failures forward as a claim about the current checkout.

The repository requests JDK 25, which is absent locally. Verification used the installed JDK 26
with a temporary `/tmp/inventory-java26.gradle` init script that overrides only the local toolchain
selection after project evaluation. The repository's build configuration and Java 17 release target
were not changed. A plain Gradle invocation still requires the configured JDK 25.

Commands run from Activity_2, each with `--init-script /tmp/inventory-java26.gradle`:

- `./gradlew core:compileJava lwjgl3:compileJava`: passed.
- `./gradlew core:test --tests '*EquipmentIntegrationTest'`: 7 passed.
- `./gradlew test javadocAudit`: test task failed on existing baseline failures.
  Core: 1,080 tests, 11 failures, zero errors/skips. Desktop: 4 tests passed.
- `./gradlew javadocAudit`: independently passed both strict audits.
- `./gradlew lwjgl3:run`: desktop process started and remained running. The native UI tool could
  not identify its Java window, so visual/mouse interaction acceptance is not claimed.
- `git diff --check`: passed.

The five failing test classes were rerun on a temporary snapshot of the original HEAD, with the
same JDK override and unchanged assets: 124 tests, the same 11 failures. XML comparison confirmed
identical failing test names and messages. The original problems are missing assets/art_resources
and stale Boss/combat/defeat expectations in ArtIntegrationTest, BossDemoTest, CombatDemoTest,
CoreGameplayIntegrationTest and PlayerDefeatTest. They were not bypassed or rewritten here.

## Manual regression checklist

1. Talk to the existing weapon merchant and receive the staff.
2. Open the chest, take armour and potions, then close it.
3. Open B, right-click the staff and armour. Confirm worn labels and attack/defence totals;
   check two- and four-piece thresholds by toggling pieces.
4. Compare identical attacks against the training dummy with the staff equipped and unequipped.
5. Store a worn item in the chest; reopen the bag and confirm its bonus has disappeared.
6. Use a potion while injured; confirm healing and quantity. Try again at full HP: no consumption.
7. Complete a Boss encounter; confirm its material enters the bag once and survives travel.
8. Resize the window and use Left/Right pages; confirm displayed rows match right-click targets.

The original feature is merged. The later storage-help and empty-weapon-label refinement
passed core compilation and the core JavaDoc audit on 2026-09-25. Its visual regression
check remains to be performed; compiling does not establish mouse/layout acceptance.


### Repeatable potion supplies

Talk to the **Weapon Merchant** in the Hub to open his service menu. Use Up/Down to
select **Claim Lightning Staff** or **Replenish potions**, E to confirm, and Esc to leave.
The weapon row shows Available/Claimed from the existing saved reward flag. The potion
row shows bag, chest and combined quantities, plus the shortfall or Full status. Statuses
refresh immediately and the menu stays open after each service.

Weapon collection is once-only and does not replenish potions. Potion supplies do not
claim the weapon: if the combined total is below three, only the shortfall is added to
the bag. Stored potions and any surplus are retained. Repeated requests cannot accumulate
extra potions. Opening or closing the menu grants nothing. Save manually to persist changes;
neither service automatically writes a save slot.
