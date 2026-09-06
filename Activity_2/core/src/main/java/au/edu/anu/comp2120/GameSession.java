package au.edu.anu.comp2120;

import au.edu.anu.comp2120.player.Player;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.GameMap;
import au.edu.anu.comp2120.world.SpawnPoint;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Coordinates the current map and a persistent player instance on the game-loop thread.
 * Map lookup and full-body spawn validation finish before a transition is committed.
 * World classes remain independent of concrete player types and screens.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class GameSession {
    /** Immutable catalogue, indexed by unique map ID. */
    private final Map<String, GameMap> maps;
    /** Currently active map and source of player collision data. */
    private GameMap currentMap;
    /** Player instance retained through all map changes. */
    private final Player player;
    /** Changes after every successful transition, including re-entry to the same map/spawn. */
    private long transitionVersion;

    /**
     * Registers maps and enters the initial map's default spawn.
     *
     * @param maps available map definitions with unique IDs
     * @param initialMapId initial map ID
     * @param playerWidth positive player collision width
     * @param playerHeight positive player collision height
     * @param playerSpeed positive finite speed in world units per second
     * @throws NullPointerException if a required value or map entry is null
     * @throws IllegalArgumentException if IDs repeat, the initial map is missing, or spawning is invalid
     */
    public GameSession(Collection<GameMap> maps, String initialMapId,
            double playerWidth, double playerHeight, double playerSpeed) {
        Objects.requireNonNull(maps, "maps");
        Map<String, GameMap> registered = new LinkedHashMap<>();
        for (GameMap map : maps) {
            Objects.requireNonNull(map, "map");
            if (registered.putIfAbsent(map.getId(), map) != null) {
                throw new IllegalArgumentException("Duplicate map ID: " + map.getId());
            }
        }
        this.maps = Map.copyOf(registered);
        currentMap = getMap(initialMapId);
        SpawnPoint spawn = currentMap.getSpawnPoint(currentMap.getDefaultSpawnId());
        player = new Player(new CollisionBox(spawn.x(), spawn.y(), playerWidth, playerHeight),
                playerSpeed, currentMap);
    }

    /**
     * Returns the map whose collisions currently govern player movement.
     *
     * @return active map
     */
    public GameMap getCurrentMap() {
        return currentMap;
    }

    /**
     * Returns the player for input and rendering; use switchMap for coordinated map changes.
     *
     * @return persistent player instance
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Exposes a transition stamp for arrival guards without adding observers or callbacks.
     *
     * @return stamp incremented only after successful transitions; compare for inequality
     */
    public long getTransitionVersion() {
        return transitionVersion;
    }

    /**
     * Looks up a registered map without changing the session.
     *
     * @param mapId stable map ID
     * @return registered map
     * @throws NullPointerException if mapId is null
     * @throws IllegalArgumentException if mapId is not registered
     */
    public GameMap getMap(String mapId) {
        GameMap map = maps.get(Objects.requireNonNull(mapId, "map ID"));
        if (map == null) {
            throw new IllegalArgumentException("Unknown map: " + mapId);
        }
        return map;
    }

    /**
     * Enters a map at its default spawn, including re-entry to the current map.
     *
     * @param mapId destination map ID
     * @throws NullPointerException if mapId is null
     * @throws IllegalArgumentException if the map is missing or its spawn cannot fit the player
     */
    public void switchMap(String mapId) {
        GameMap target = getMap(mapId);
        switchMap(mapId, target.getDefaultSpawnId());
    }

    /**
     * Enters a named spawn. Failure preserves the current map, player position, and collisions.
     * This is an explicit transition primitive, not portal, quest, or teleport-trigger logic.
     *
     * @param mapId destination map ID
     * @param spawnId destination spawn ID
     * @throws NullPointerException if an ID is null
     * @throws IllegalArgumentException if an ID is missing or the full body cannot fit at the spawn
     */
    public void switchMap(String mapId, String spawnId) {
        GameMap target = getMap(mapId);
        SpawnPoint spawn = target.getSpawnPoint(spawnId);
        player.enterArea(target, spawn.x(), spawn.y());
        currentMap = target;
        transitionVersion++;
    }
}
