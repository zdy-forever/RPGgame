package au.edu.anu.comp2120.screen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies the objective tracker text built by {@link GameScreen#buildObjectiveTasks}.
 * Acts as a regression guard for the tofu-box fix: every line must stay plain English.
 *
 * @author Jiaqi Chen (u7851266)
 */
final class GameScreenObjectiveTest {

    /** Creates the test instance. */
    GameScreenObjectiveTest() { }

    /** Tutorial step yields the matching plain-English primary line. */
    @Test
    void tutorialStepProducesEnglishLine() {
        List<String> tasks = GameScreen.buildObjectiveTasks("hub_guide", "Old Sage", false, "", false, false, false);
        assertEquals(List.of("Talk to Old Sage for the tutorial"), tasks);
    }

    /** Each onboarding NPC maps to its own English line. */
    @Test
    void everyStepHasEnglishLine() {
        assertEquals(List.of("Talk to Class Master to choose a class"),
                GameScreen.buildObjectiveTasks("class_master", "Class Master", false, "", false, false, false));
        assertEquals(List.of("Talk to Quest Keeper to choose a Boss"),
                GameScreen.buildObjectiveTasks("quest_keeper", "Quest Keeper", false, "", false, false, false));
        assertEquals(List.of("Talk to Portal Guide to go to the battlefield"),
                GameScreen.buildObjectiveTasks("portal_guide", "Portal Guide", false, "", false, false, false));
        assertEquals(List.of("Talk to Someone"),
                GameScreen.buildObjectiveTasks("unknown_step", "Someone", false, "", false, false, false));
    }

    /** A boss already chosen makes the defeat goal primary and the Portal Guide the side task. */
    @Test
    void chosenBossMakesDefeatPrimaryAndPortalSide() {
        assertEquals(List.of("Defeat Fire Demon", "Talk to Portal Guide to enter the battle"),
                GameScreen.buildObjectiveTasks("quest_keeper", "Quest Keeper", true, "Fire Demon", false, false, false));
    }

    /** Active boss at the Portal Guide still shows the Portal Guide side task (defeat is primary). */
    @Test
    void activeBossAtPortalGuideStillShowsPortalSide() {
        assertEquals(List.of("Defeat Fire Demon", "Talk to Portal Guide to enter the battle"),
                GameScreen.buildObjectiveTasks("portal_guide", "Portal Guide", true, "Fire Demon", false, false, false));
    }

    /** Once all guardians fall, Old Sage points to the Memorial Hall path. */
    @Test
    void allDefeatedPointsToMemorial() {
        assertEquals(List.of("Talk to Old Sage to learn the Memorial Hall path"),
                GameScreen.buildObjectiveTasks("hub_guide", "Old Sage", false, "", true, false, false));
    }

    /** Once the Memorial Hall path is revealed, the marker moves to the Portal Guide. */
    @Test
    void revealedPathPointsToPortalGuide() {
        assertEquals(List.of("Talk to Portal Guide and choose Memorial Hall"),
                GameScreen.buildObjectiveTasks("hub_guide", "Old Sage", false, "", true, true, false));
    }

    /** A recorded Hall ending replaces stale onboarding, portal and boss directions. */
    @Test
    void endingReplacesTasksWithFreeExploration() {
        for (boolean revealed : new boolean[] {false, true}) {
            assertEquals(List.of("feel free to explore this game!"),
                    GameScreen.buildObjectiveTasks("portal_guide", "Portal Guide", true,
                            "Fire Sentinel", true, revealed, true));
        }
    }

    /** Active uncompleted boss makes defeat primary and Portal Guide the side task. */
    @Test
    void activeBossAddsDefeatAndPortalGuideLine() {
        List<String> tasks = GameScreen.buildObjectiveTasks(null, "", true, "Fire Demon", false, false, false);
        assertEquals(List.of("Defeat Fire Demon", "Talk to Portal Guide to enter the battle"), tasks);
    }

    /** Active boss always beats an unfinished onboarding step; defeat is primary. */
    @Test
    void stepAndBossCombine() {
        List<String> tasks = GameScreen.buildObjectiveTasks("hub_guide", "Old Sage", true, "Fire Demon", false, false, false);
        assertEquals(List.of("Defeat Fire Demon", "Talk to Portal Guide to enter the battle"), tasks);
    }

    /** Onboarding done with no boss chosen points back to the Quest Keeper. */
    @Test
    void betweenBossesPointsToQuestKeeper() {
        List<String> tasks = GameScreen.buildObjectiveTasks(null, "", false, "", false, false, false);
        assertEquals(List.of("Talk to Quest Keeper to choose a Boss"), tasks);
    }

    /** Regression guard: with realistic English data, no line contains Chinese (tofu-box) characters. */
    @Test
    void linesContainNoCjkCharacters() {
        List<String> tasks = GameScreen.buildObjectiveTasks("hub_guide", "Old Sage", true, "Fire Sentinel", false, false, false);
        for (String line : tasks) {
            assertFalse(line.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN),
                    "Unexpected CJK in objective line: " + line);
        }
    }
}
