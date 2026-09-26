package au.edu.anu.comp2120.inventory;

import java.util.List;
import java.util.Optional;

/** Central item definitions and initial balance, independent of rendering.
 * Unknown reward IDs remain valid stackable materials in Inventory.
 * @author Hao Liu (u7998420)
 */
public final class ItemCatalog {
    /** Amount restored by one potion, capped by existing maximum health. */
    public static final int POTION_HEAL = 30;
    /** Stable consumable identity. */
    public static final String POTION = "health_potion";
    /** Immutable wearable catalogue; percentages are additive before combat multiplication. */
    private static final List<Gear> GEAR = List.of(
            new Gear("weapon_lightning_staff", "Lightning staff", EquipmentSlot.WEAPON, 20, 0, ""),
            new Gear("guardian_hood", "Guardian hood", EquipmentSlot.HEAD, 0, 0, "guardian"),
            new Gear("guardian_coat", "Guardian coat", EquipmentSlot.BODY, 0, 8, "guardian"),
            new Gear("guardian_gloves", "Guardian gloves", EquipmentSlot.HANDS, 0, 0, "guardian"),
            new Gear("guardian_boots", "Guardian boots", EquipmentSlot.FEET, 0, 0, "guardian"));

    /** Prevents catalogue construction. */
    private ItemCatalog() { }

    /** Finds an equippable item without accepting arbitrary weapon-prefixed material IDs.
     * @param id stable item ID
     * @return matching definition, or empty for non-equipment
     */
    public static Optional<Gear> gear(String id) {
        var base = GEAR.stream().filter(item -> item.id().equals(id)).findFirst();
        if (base.isPresent() || id == null) { return base; }
        for (Gear item : GEAR) {
            for (int level = 1; level <= 2; level++) {
                if ((item.id() + "+" + level).equals(id)) {
                    return Optional.of(new Gear(id, item.name() + " +" + level, item.slot(),
                            item.attackPercent() + (item.slot() == EquipmentSlot.WEAPON ? 5 * level : 0),
                            item.defencePercent() + (item.slot() == EquipmentSlot.BODY ? 2 * level : 0), item.setId()));
                }
            }
        }
        return Optional.empty();
    }

    /** Lists all supported wearable definitions.
     * @return immutable catalogue
     */
    public static List<Gear> equipment() { return GEAR; }

    /** Formats an item for the existing text UI.
     * @param id stable non-null item ID
     * @return readable label
     */
    public static String name(String id) {
        return gear(id).map(Gear::name).orElse(id.replace('_', ' ').replace('-', ' '));
    }

    /** Supplies concise effects for an inventory row.
     * @param id stable item ID
     * @return effect description or material label
     */
    public static String effect(String id) {
        if (POTION.equals(id)) { return "Heal " + POTION_HEAL; }
        return gear(id).map(item -> switch (item.slot()) {
            case WEAPON -> "ATK +" + item.attackPercent() + "%";
            case HEAD -> "Max HP +" + item.healthBonus();
            case BODY -> "DEF " + item.defencePercent() + "%";
            case HANDS -> "Skill CDR " + item.cooldownPercent() + "%";
            case FEET -> "Move speed +" + item.speedPercent() + "%";
        }).orElse("Material");
    }

    /** Immutable internal catalogue entry.
     * @param id stable inventory identity
     * @param name display label
     * @param slot occupied wearable position
     * @param attackPercent added damage percentage
     * @param defencePercent added damage reduction percentage
     * @param setId armour set identity, empty for no set
     */
    public record Gear(String id, String name, EquipmentSlot slot,
                       int attackPercent, int defencePercent, String setId) {
        /** Reads the bounded enhancement encoded in this catalogue identity.
         * @return enhancement level
         */
        private int level() { return id.endsWith("+2") ? 2 : id.endsWith("+1") ? 1 : 0; }
        /** Returns headgear health capacity.
         * @return added maximum HP
         */
        public int healthBonus() { return slot == EquipmentSlot.HEAD ? 20 + 10 * level() : 0; }
        /** Returns glove cooldown reduction for K/L/O.
         * @return reduction percentage
         */
        public int cooldownPercent() { return slot == EquipmentSlot.HANDS ? 10 + 10 * level() : 0; }
        /** Returns boot ordinary movement amplification.
         * @return speed percentage
         */
        public int speedPercent() { return slot == EquipmentSlot.FEET ? 10 + 10 * level() : 0; }
    }
}
