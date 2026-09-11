package au.edu.anu.comp2120;

import java.util.Objects;

/**
 * External permission for one accepted challenge, not a Quest or its progress state.
 * Use a new encounter ID for each acceptance, including repeat quests on the same map.
 * @param id unique acceptance ID within this gameplay session
 * @param mapId target battle map
 * @param spawnId target player spawn
 * @param portalId associated registered Hub portal
 * @author Daoyan Zhu (u7782042)
 */
public record ActiveEncounter(String id, String mapId, String spawnId, String portalId) {
    /**
     * Validates identifiers; the integration boundary validates their world references.
     * @param id unique acceptance ID
     * @param mapId battle map ID
     * @param spawnId player spawn ID
     * @param portalId Hub portal ID
     * @throws NullPointerException if an identifier is null
     * @throws IllegalArgumentException if an identifier is blank
     */
    public ActiveEncounter {
        for (String value : new String[] {id, mapId, spawnId, portalId}) {
            if (Objects.requireNonNull(value, "encounter identifier").isBlank()) {
                throw new IllegalArgumentException("Encounter identifiers must not be blank");
            }
        }
    }
}
