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
            new Gear("guardian_hood", "Guardian hood", EquipmentSlot.HEAD, 0, 4, "guardian"),
            new Gear("guardian_coat", "Guardian coat", EquipmentSlot.BODY, 0, 8, "guardian"),
            new Gear("guardian_gloves", "Guardian gloves", EquipmentSlot.HANDS, 0, 4, "guardian"),
            new Gear("guardian_boots", "Guardian boots", EquipmentSlot.FEET, 0, 4, "guardian"));

    /** Prevents catalogue construction. */
    private ItemCatalog() { }

    /** Finds an equippable item without accepting arbitrary weapon-prefixed material IDs.
     * @param id stable item ID
     * @return matching definition, or empty for non-equipment
     */
    public static Optional<Gear> gear(String id) {
        return GEAR.stream().filter(item -> item.id().equals(id)).findFirst();
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
        return gear(id).map(item -> item.slot() == EquipmentSlot.WEAPON
                ? "ATK +" + item.attackPercent() + "%"
                : item.slot() + " DEF " + item.defencePercent() + "%").orElse("Material");
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
                       int attackPercent, int defencePercent, String setId) { }
}
