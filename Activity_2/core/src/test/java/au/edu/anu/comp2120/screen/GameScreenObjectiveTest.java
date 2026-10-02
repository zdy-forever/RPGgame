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

    /** Tutorial step yields the matching plain-English line. */
    @Test
    void tutorialStepProducesEnglishLine() {
        List<String> tasks = GameScreen.buildObjectiveTasks("hub_guide", "Old Sage", false, "");
        assertEquals(List.of("Talk to Old Sage for the tutorial"), tasks);
    }

    /** Each onboarding NPC maps to its own English line. */
    @Test
    void everyStepHasEnglishLine() {
        assertEquals(List.of("Talk to Class Master to choose a class"),
                GameScreen.buildObjectiveTasks("class_master", "Class Master", false, ""));
        assertEquals(List.of("Talk to Quest Keeper to receive a quest"),
                GameScreen.buildObjectiveTasks("quest_keeper", "Quest Keeper", false, ""));
        assertEquals(List.of("Talk to Portal Guide to go to the battlefield"),
                GameScreen.buildObjectiveTasks("portal_guide", "Portal Guide", false, ""));
        assertEquals(List.of("Talk to Someone"),
                GameScreen.buildObjectiveTasks("unknown_step", "Someone", false, ""));
    }

    /** Active uncompleted boss adds a Defeat line. */
    @Test
    void activeBossAddsDefeatLine() {
        List<String> tasks = GameScreen.buildObjectiveTasks(null, "", true, "Fire Demon");
        assertEquals(List.of("Defeat Fire Demon"), tasks);
    }

    /** Tutorial step and boss combine into two lines. */
    @Test
    void stepAndBossCombine() {
        List<String> tasks = GameScreen.buildObjectiveTasks("hub_guide", "Old Sage", true, "Fire Demon");
        assertEquals(List.of("Talk to Old Sage for the tutorial", "Defeat Fire Demon"), tasks);
    }

    /** Nothing active falls back to the free-explore line. */
    @Test
    void noObjectiveFallsBackToFreeExplore() {
        List<String> tasks = GameScreen.buildObjectiveTasks(null, "", false, "");
        assertEquals(List.of("Free explore: leave via Portal Guide"), tasks);
    }

    /** Regression guard: with realistic English data, no line contains Chinese (tofu-box) characters. */
    @Test
    void linesContainNoCjkCharacters() {
        List<String> tasks = GameScreen.buildObjectiveTasks("hub_guide", "Old Sage", true, "Fire Sentinel");
        for (String line : tasks) {
            assertFalse(line.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN),
                    "Unexpected CJK in objective line: " + line);
        }
    }
}
