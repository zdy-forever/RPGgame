package au.edu.anu.comp2120.npc;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.player.PlayerAbilities;
import au.edu.anu.comp2120.player.PlayerClass;
import au.edu.anu.comp2120.player.PlayerClasses;
import au.edu.anu.comp2120.player.UltimateMode;
import au.edu.anu.comp2120.world.MapType;
import java.util.Objects;

/**
 * Hub NPC choices delegating class and ultimate selection to the existing ability controller.
 * Does not replace the player, heal, reset cooldowns or redefine J/K/L skills.
 * @author Daoyan Zhu (u7782042)
 */
public final class ClassTrainer {
    /** Persistent session containing the player being configured. */
    private final GameSession session;
    /** That session's existing ability controller, including busy/dead/selection guards. */
    private final PlayerAbilities abilities;

    /**
     * Binds the NPC to the session and its existing ability controller.
     * @param session current gameplay session
     * @param abilities controller belonging to this session's player
     * @throws NullPointerException if either dependency is null
     */
    public ClassTrainer(GameSession session, PlayerAbilities abilities) {
        this.session = Objects.requireNonNull(session, "session");
        this.abilities = Objects.requireNonNull(abilities, "abilities");
    }

    /**
     * Five choices offered by the NPC; mode choices apply to the currently selected profession.
     * @author Daoyan Zhu (u7782042)
     */
    public enum Choice {
        /** Select the bow profession. */
        ARCHER,
        /** Select the magic profession. */
        MAGE,
        /** Select the sword profession. */
        KATANA,
        /** Select the current profession's first ultimate. */
        MODE_A,
        /** Select the current profession's second ultimate. */
        MODE_B
    }

    /**
     * Applies one selection in a living Hub, preserving all existing ability restrictions.
     * Class changes use the catalogue definition, leaving remembered per-class O choices intact.
     * @param choice requested profession or O mode
     * @return true if changed; false if unchanged, busy, dead or outside Hub
     * @throws NullPointerException if choice is null
     */
    public boolean select(Choice choice) {
        Objects.requireNonNull(choice, "choice");
        if (session.getCurrentMap().getType() != MapType.HUB || !session.getPlayer().isAlive()) {
            return false;
        }
        return switch (choice) {
            case ARCHER -> abilities.selectClass(PlayerClasses.get(PlayerClass.ARCHER));
            case MAGE -> abilities.selectClass(PlayerClasses.get(PlayerClass.MAGE));
            case KATANA -> abilities.selectClass(PlayerClasses.get(PlayerClass.KATANA));
            case MODE_A -> abilities.selectUltimateMode(UltimateMode.A);
            case MODE_B -> abilities.selectUltimateMode(UltimateMode.B);
        };
    }

    /**
     * Provides a readable option label including the current class's actual ultimate name.
     * @param choice menu option
     * @return label suitable for the existing built-in font
     * @throws NullPointerException if choice is null
     */
    public String label(Choice choice) {
        return switch (Objects.requireNonNull(choice, "choice")) {
            case ARCHER -> "Class: Archer";
            case MAGE -> "Class: Mage";
            case KATANA -> "Class: Katana";
            case MODE_A -> "O mode A: " + switch (session.getPlayer().getPlayerClass()) {
                case ARCHER -> "Arrow Rain";
                case MAGE -> "Large Magic Circle";
                case KATANA -> "Watermoon Stance";
            };
            case MODE_B -> "O mode B: " + switch (session.getPlayer().getPlayerClass()) {
                case ARCHER -> "Untargetable";
                case MAGE -> "Rapid Element Barrage";
                case KATANA -> "Helmbreaker";
            };
        };
    }
}
