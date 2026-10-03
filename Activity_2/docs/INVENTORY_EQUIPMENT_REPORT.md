# Inventory, equipment, potions and upgrades

Reviewed against `7fea86a` on 2026-10-02. Integration was implemented by Hao Liu (`u7998420`)
after the inventory/UI prototypes; later team changes include P use and HUD feedback.
Sources: [ItemCatalog](../core/src/main/java/au/edu/anu/comp2120/inventory/ItemCatalog.java),
[Equipment](../core/src/main/java/au/edu/anu/comp2120/inventory/Equipment.java),
[GearUpgrades](../core/src/main/java/au/edu/anu/comp2120/inventory/GearUpgrades.java).

## Obtain, equip and store items

A new Player owns an empty bag and Equipment backed by that bag. The separate Hub chest starts
with Guardian hood, coat, gloves, boots and three health potions. Weapon Merchant at (650, 250)
grants the Lightning staff once through dialogue. Items and equipment persist across travel.

B opens the bag. Select with W/S or arrows and use E/Enter or right-click to equip/unequip gear
or drink a potion. Left-click selects a row. Q/R or PageUp/PageDown changes pages. Rows show
effects and worn state; the footer shows attack/reduction percentages and armour-set progress.
Gear is inspected here; no top-right weapon or coordinate HUD is displayed.

E near Storage Chest opens two columns. A/D or Left/Right selects a column, W/S or arrows a row,
and E/Enter or right-click transfers the entire stack. B switches from storage to bag so items
can be equipped. Equipped items remain counted in the bag. Moving the last carried unit of worn
gear into the chest removes its benefit; leaving another unit carried retains it. Transfers do
not equip an item automatically. Bag/chest panels pause combat and consume their closing frame.

## Current item values

Values come from ItemCatalog and Equipment, before elemental/status effects.

| Item/effect | Base | +1 | +2 |
| --- | --- | --- | --- |
| Lightning staff attack | +20% | +25% | +30% |
| Guardian hood maximum HP | +20 HP | +30 HP | +40 HP |
| Guardian coat damage reduction | 8% | 10% | 12% |
| Guardian gloves K/L/O cooldown reduction | 10% | 20% | 30% |
| Guardian boots ordinary movement speed | +10% | +20% | +30% |

Two worn Guardian armour pieces add 5 percentage points of damage reduction; four add 10
percentage points of attack. Armour/set reduction is additive and capped at 60%. Weapon/set
attack is additive. Equipment modifiers enter shared combat settlement rather than a separate
inventory damage engine. The staff is usable by all three classes; its name does not force
all attacks to become Lightning.

Hood capacity changes preserve living HP proportion through Health's capacity API and do not
revive a dead player. Gloves affect K/L/O, not J, and do not retroactively recalculate remaining
cooldowns. Boots affect ordinary walking rather than class dash/thrust constants. Upgraded item
IDs carry their own level through bag, worn slot, chest and save restoration.

## Potions and supplies

One potion heals up to 30 HP, capped at the current equipment-adjusted maximum. Full-health,
dead or empty-bag use is refused without consumption. P is edge-triggered during ordinary play,
including Boss battles; holding it does not repeatedly consume. Bag use follows the same rules.
Chest stock cannot be drunk directly and modal panels own input before P sampling.

Every class quickbar displays a procedural P bottle and carried quantity: grey at zero,
`xN` through 999, then `999+`. The bag retains the exact count. Mage also retains its element
slot. Both P and bag use report actual healing and remaining stock, or a specific refusal,
in a four-second toast. HUD formatting does not cap the underlying stack.

The merchant offers free potion replenishment to a combined bag-plus-chest total of three.
It adds only the deficit to the bag. Hiding potions in the chest therefore does not create
extra supplies. This service remains available after the one-time staff dialogue.

## Upgrades and rewards

Merchant upgrades cover the weapon and four armour slots. The candidate is the worn item,
otherwise the highest-level carried item in that slot. Gear stored only in the chest must first
be moved to the bag. Preview/confirmation names the item and effect before material consumption.

Each step costs one `sentinel-core`, consumed from the bag before the chest. Maximum level is
+2. The operation validates ownership, distinct stores, material stock and destination-stack
capacity before mutation. It replaces one carried item with the next variant and keeps worn
gear equipped. Debug freeupgrade bypasses cost only, preserving all other guards.

An admitted Boss success supplies its one-time reward through EncounterFlow and InventoryRewards.
The current shared reward is one sentinel core. Collection checks integer-stack capacity before
claiming; overflow leaves the reward pending. Failure/Training gives no campaign reward, and
repeat polling cannot duplicate a settled reward. A fresh accepted rematch can earn another core.

Bag/chest/equipment, upgraded identities and pending rewards are saved together. See
[Save/load](SAVE_LOAD_REPORT.md) for snapshot validation and independent timeline behavior.
A separate recipe-based crafting/forging/enchanting system was reduced to these merchant upgrades
because it was not a core feature.

## Verification

Run from Activity_2:

```sh
./gradlew core:test --tests 'au.edu.anu.comp2120.inventory.*' \
  --tests '*MerchantServicesTest' --tests '*PotionGameplayTest' \
  --tests '*PotionFeedbackTest' --tests '*GameSavesTest'
```

Tests cover stack operations/overflow, equipment ownership and reconciliation, bonuses/capacity,
movement/cooldown effects, whole-store material accounting, upgrade guards, potion eligibility,
edge input, one-time rewards and save restoration. The [tester guide](TEST_COVERAGE_REVIEW.md)
records actual CI execution and distinguishes these assertions from visual checks.

[Native checks on 2026-10-03](PLAYTEST_REPORT.md) verified chest-to-bag transfer, four armour
slots/bonuses, staff claim and duplicate guard, missing-material upgrade rejection, full-health
P feedback without consumption, and the first real victory core restored from AUTO. The
follow-up verified five cores after all five wins (Wind completed by the user without debug). Screenshots show
the equipped bag and reward. Still verify mouse/paging/tiny-window rows, damaged-player healing,
successful upgrades and upgraded worn-item display.
