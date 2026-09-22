package au.edu.anu.comp2120.combat;

/** Optional equipment values read by the shared damage pipeline, without inventory dependencies.
 * @author Hao Liu (u7998420)
 */
public interface EquipmentModifiers {
    /** Reads equipment attack amplification.
     * @return finite multiplier of at least one
     */
    double equipmentAttackMultiplier();

    /** Reads equipment damage reduction.
     * @return finite multiplier between zero and one
     */
    double equipmentDefenceMultiplier();
}
