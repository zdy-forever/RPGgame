package au.edu.anu.comp2120.save;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapType;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Saved safe-map position and living player health, independent of graphics.
 * Class choices, abilities and quest progress belong to other snapshot components.
 * @param mapId registered map identity
 * @param x bottom-left horizontal position
 * @param y bottom-left vertical position
 * @param hp saved positive HP
 * @param maxHp capacity used to detect incompatible player configuration
 * @author Hao Liu (u7998420)
 */
public record LocationSnapshot(String mapId, double x, double y, int hp, int maxHp) {
    /** Complete namespace keys; missing or extra location fields indicate an invalid save. */
    private static final Set<String> KEYS = Set.of("location.map", "location.x", "location.y",
            "location.hp", "location.maxHp");

    /** Validates scalar values without changing live gameplay.
     * @param mapId nonblank map identity
     * @param x finite coordinate
     * @param y finite coordinate
     * @param hp positive saved HP
     * @param maxHp positive capacity no smaller than hp
     * @throws IllegalArgumentException if a value is invalid
     */
    public LocationSnapshot {
        if (mapId == null || mapId.isBlank() || !Double.isFinite(x) || !Double.isFinite(y)
                || x < 0 || y < 0 || hp <= 0 || maxHp < hp) {
            throw new IllegalArgumentException("Invalid saved location or health");
        }
    }

    /** Captures a living player in a safe map, after the coordinator checks active actions.
     * @param session current game session
     * @return validated location and HP
     * @throws NullPointerException if session is null
     * @throws IllegalArgumentException if the current map or player is not saveable
     */
    public static LocationSnapshot capture(GameSession session) {
        var player = session.getPlayer();
        var result = new LocationSnapshot(session.getCurrentMap().getId(), player.getX(), player.getY(),
                player.getHealth().getCurrentHp(), player.getHealth().getMaxHp());
        result.validateFor(session);
        return result;
    }

    /** Checks compatibility, destination map role and full-body collision before any mutation.
     * The caller must also validate all other components before restoring the full game.
     * @param session destination session
     * @throws NullPointerException if session is null
     * @throws IllegalArgumentException if the save cannot be applied to this session
     */
    public void validateFor(GameSession session) {
        validateFor(session, session.getPlayer().getHealth().getMaxHp());
    }

    /** Validates against capacity computed from the saved equipment before changing live state.
     * @param session destination session
     * @param expectedMaxHp saved equipment capacity
     * @throws NullPointerException if session is null
     * @throws IllegalArgumentException if the map is missing or unsafe, the body is blocked,
     *         or the saved health capacity differs from expectedMaxHp
     */
    public void validateFor(GameSession session, int expectedMaxHp) {
        var map = session.getMap(mapId);
        var player = session.getPlayer();
        var bounds = player.getBounds();
        if ((map.getType() != MapType.HUB && map.getType() != MapType.MEMORIAL)
                || maxHp != expectedMaxHp
                || !map.isWalkable(new CollisionBox(x, y, bounds.width(), bounds.height()))) {
            throw new IllegalArgumentException("Saved map, position or health capacity is incompatible");
        }
    }

    /** Restores exact coordinates and HP after validating this entire component.
     * This is a low-level operation: the coordinator must first enforce safe loading,
     * close UI and clear transient combat actions. It does not write either save slot.
     * @param session session to update on the game-loop thread
     * @throws NullPointerException if session is null
     * @throws IllegalArgumentException if validation fails, without changing live state
     */
    public void restore(GameSession session) {
        validateFor(session);
        session.restoreLocation(mapId, x, y);
        session.getPlayer().getHealth().restoreSavedHp(hp);
    }

    /** Encodes this component for either independent save slot.
     * @return immutable fields
     */
    public Map<String, String> toFields() {
        return Map.of("location.map", mapId, "location.x", Double.toString(x),
                "location.y", Double.toString(y), "location.hp", Integer.toString(hp),
                "location.maxHp", Integer.toString(maxHp));
    }

    /** Parses strict location fields while allowing other snapshot namespaces.
     * @param fields decoded file fields
     * @return scalar-validated component; call validateFor to check the destination session
     * @throws NullPointerException if fields is null
     * @throws IllegalArgumentException if location fields are missing or malformed
     */
    public static LocationSnapshot fromFields(Map<String, String> fields) {
        Objects.requireNonNull(fields, "fields");
        for (String key : KEYS) {
            if (fields.get(key) == null) { throw new IllegalArgumentException("Missing " + key); }
        }
        for (String key : fields.keySet()) {
            if (key == null || (key.startsWith("location.") && !KEYS.contains(key))) {
                throw new IllegalArgumentException("Unknown location field: " + key);
            }
        }
        return new LocationSnapshot(fields.get("location.map"), Double.parseDouble(fields.get("location.x")),
                Double.parseDouble(fields.get("location.y")), Integer.parseInt(fields.get("location.hp")),
                Integer.parseInt(fields.get("location.maxHp")));
    }
}
