package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.combat.Combatant;
import au.edu.anu.comp2120.combat.Faction;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.MapCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the coordinator resolves the player's queued displacement and honours control locks.
 * Guardian traits and elemental reactions both latch these effects; only this owner applies them.
 * @author Daoyan Zhu (u7782042)
 */
final class PlayerControlEffectsTest {
    /** Real session on the open Hub, where no encounter interferes with the observation. */
    private final GameSession session = new GameSession(MapCatalog.createMaps(), MapCatalog.HUB, 28, 28, 180);
    /** Production-shaped coordinator without graphics. */
    private final CombatDemo demo = new CombatDemo(session);

    /** Creates an isolated fixture. */
    PlayerControlEffectsTest() {
        demo.update(0, PlayerCommand.NONE, false);
    }

    /** A queued push moves the player through swept collision and is consumed exactly once. */
    @Test
    void queuedPushMovesThePlayerOnceThroughCollision() {
        session.getPlayer().enterArea(session.getCurrentMap(), 300, 200);
        playerActor().getStatus().applyPush(1, 0, 60);
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(360, session.getPlayer().getX(), 1e-9);
        assertEquals(200, session.getPlayer().getY(), 1e-9);
        assertEquals(0, playerActor().getStatus().getKnockbackX());
        assertEquals(new CollisionBox(session.getPlayer().getX() + GameConfig.COMBAT_HURTBOX_INSET,
                session.getPlayer().getY() + GameConfig.COMBAT_HURTBOX_INSET,
                session.getPlayer().getBounds().width() - 2 * GameConfig.COMBAT_HURTBOX_INSET,
                session.getPlayer().getBounds().height() - 2 * GameConfig.COMBAT_HURTBOX_INSET),
                playerActor().getHurtbox(), "The pushed body must carry its hurtbox with it");
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(360, session.getPlayer().getX(), 1e-9);
    }

    /** A push into the map boundary is clamped by the existing collision resolver, not the status. */
    @Test
    void pushCannotLeaveTheWalkableWorld() {
        session.getPlayer().enterArea(session.getCurrentMap(), 20, 200);
        playerActor().getStatus().applyPush(-1, 0, 400);
        demo.update(0, PlayerCommand.NONE, false);
        assertEquals(0, session.getPlayer().getX(), 1e-9);
        assertTrue(session.getCurrentMap().isWalkable(session.getPlayer().getBounds()));
    }

    /** Freeze and stagger suppress movement and new abilities, then release the player again. */
    @Test
    void controlLockSuppressesInputUntilItExpires() {
        for (boolean freeze : new boolean[] {true, false}) {
            for (int idle = 0; idle < 30; idle++) {
                demo.update(0.1, PlayerCommand.NONE, false);
            }
            assertTrue(demo.getActiveAttacks().isEmpty(), "Fixture must start from a quiet arena");
            session.getPlayer().enterArea(session.getCurrentMap(), 300, 200);
            if (freeze) {
                playerActor().getStatus().applyFreeze(0.5);
            } else {
                playerActor().getStatus().applyStagger(0.5);
            }
            PlayerCommand run = new PlayerCommand(1, 0, true, false, false, false);
            demo.update(0.1, run, false);
            assertEquals(300, session.getPlayer().getX(), 1e-9, "Locked players must not move");
            assertTrue(demo.getActiveAttacks().isEmpty(), "Locked players must not start new attacks");
            for (int i = 0; i < 5; i++) {
                demo.update(0.1, PlayerCommand.NONE, false);
            }
            assertFalse(playerActor().getStatus().blocksActions());
            demo.update(0.1, run, false);
            assertTrue(session.getPlayer().getX() > 300, "Movement must resume once the lock expires");
        }
    }

    /**
     * Finds the canonical registered player adapter without constructing an alias receiver.
     * @return current player combatant
     */
    private Combatant playerActor() {
        return demo.getCombatants().stream().filter(actor -> actor.getFaction() == Faction.PLAYER)
                .findFirst().orElseThrow();
    }
}
