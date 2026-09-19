package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.GameSession;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.player.PlayerCommand;
import au.edu.anu.comp2120.world.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless art-catalogue, random-practice and expanded-world regression checks.
 * No texture loading, graphics context, damage reimplementation or probabilistic assertions.
 * @author Daoyan Zhu (u7782042)
 */
final class ArtIntegrationTest {
    /** Supports both root-directory IDE runs and Gradle's core test working directory. */
    private static final Path ASSETS = Files.isDirectory(Path.of("assets")) ? Path.of("assets") : Path.of("..", "assets");

    /** Creates isolated test fixtures with no graphics. */
    ArtIntegrationTest() { }

    /**
     * Every relocated runtime PNG has valid manifest dimensions and a divisible horizontal strip.
     * @throws IOException if the asset catalogue or an image cannot be read
     */
    @Test void manifestCoversEveryRemainingPng() throws IOException {
        List<String> rows = Files.readAllLines(ASSETS.resolve("art/ASSET_MANIFEST.csv"));
        var listed = new HashSet<Path>();
        for (String row : rows.subList(1, rows.size())) {
            String[] fields = row.split(",");
            Path file = ASSETS.resolve(fields[0]);
            assertTrue(listed.add(file), "Duplicate manifest path: " + file);
            var image = ImageIO.read(file.toFile());
            assertNotNull(image, fields[0]);
            assertEquals(Integer.parseInt(fields[2]), image.getWidth(), fields[0]);
            assertEquals(Integer.parseInt(fields[3]), image.getHeight(), fields[0]);
            assertTrue(ArtAssets.frameCount(fields[0], image.getWidth(), image.getHeight()) > 0);
        }
        try (var files = Files.walk(ASSETS.resolve("art"))) {
            assertEquals(listed, new HashSet<>(files.filter(p -> p.toString().endsWith(".png")).toList()));
        }
        assertEquals(220, listed.size());
    }

    /**
     * The import staging directory is empty and only the two requested map pictures are excluded.
     * @throws IOException if staging cannot be listed
     */
    @Test void stagingEmptyAndHubGameplayRetained() throws IOException {
        try (var contents = Files.list(ASSETS.resolve("art_resources"))) { assertEquals(0, contents.count()); }
        assertFalse(Files.exists(ASSETS.resolve("art/maps/hub/hub_main.png")));
        assertFalse(Files.exists(ASSETS.resolve("art/maps/hub/achievement_corridor.png")));
        assertTrue(MapCatalog.createMaps().stream().anyMatch(map -> map.getType() == MapType.HUB));
    }

    /** Static wide projectiles and backgrounds are not mistakenly sliced into animation frames. */
    @Test void horizontalFrameLayoutIsExplicit() {
        assertEquals(4, ArtAssets.frameCount("art/player/mage/idle/idle_down_4f.png",672,216));
        assertEquals(6, ArtAssets.frameCount("art/portal/portal_loop.png",1536,272));
        assertEquals(5, ArtAssets.frameCount("art/skills/archer/arrow_rain_start.png",1280,256));
        assertEquals(1, ArtAssets.frameCount("art/projectiles/archer/arrow.png",160,80));
        assertEquals(1, ArtAssets.frameCount("art/maps/battle/fire_arena.png",640,480));
        assertThrows(IllegalArgumentException.class, () -> ArtAssets.frameCount("bad_4f.png",101,100));
        assertThrows(IllegalArgumentException.class, () -> ArtAssets.frameCount("bad.png",0,100));
        assertThrows(NullPointerException.class, () -> ArtAssets.frameCount(null,100,100));
    }

    /** A seed repeats selections exactly, with two distinct random draws and no null states. */
    @Test void practiceAppearanceIsSeededAndIndependent() {
        Random expected = new Random(7);
        Random actual = new Random(7);
        for (int i=0;i<50;i++) {
            assertEquals(new TrainingAppearance(BossChallenge.values()[expected.nextInt(5)],
                    BossChallenge.values()[expected.nextInt(5)]),TrainingAppearance.choose(actual));
        }
        assertThrows(NullPointerException.class, () -> TrainingAppearance.choose(null));
        assertThrows(NullPointerException.class, () -> new TrainingAppearance(null,BossChallenge.FIRE));
    }

    /** R resets score, not scenery; damage cannot kill/move the dummy or create a hostile Boss. */
    @Test void randomPracticeKeepsStationaryDamageScoringRules() {
        var session = new GameSession(MapCatalog.createMaps(),MapCatalog.HUB,28,28,180);
        var demo = new CombatDemo(session);
        assertTrue(demo.getTrainingAppearance().isEmpty());
        session.switchMap(MapCatalog.TRAINING);
        demo.update(0,PlayerCommand.NONE,false);
        var appearance = demo.getTrainingAppearance().orElseThrow();
        var dummy = demo.getTrainingDummy().orElseThrow();
        var bounds = dummy.getCombatant().getHurtbox();
        dummy.takeDamage(123);
        assertEquals(123,dummy.getTotalDamage());
        for (int i=0;i<100;i++) { demo.update(0.05,PlayerCommand.NONE,false); }
        assertEquals(bounds,dummy.getCombatant().getHurtbox());
        assertTrue(dummy.isAlive());
        assertTrue(demo.getBoss().isEmpty());
        assertTrue(demo.getActiveAttacks().isEmpty());
        demo.update(0,PlayerCommand.NONE,true);
        assertEquals(appearance,demo.getTrainingAppearance().orElseThrow());
        assertEquals(0,demo.getTrainingDummy().orElseThrow().getTotalDamage());
        session.switchMap(MapCatalog.HUB);
        demo.update(0,PlayerCommand.NONE,false);
        assertTrue(demo.getTrainingAppearance().isEmpty());
    }

    /** Every shipped map supports scrolling in both axes and keeps valid spawns/open boundaries. */
    @Test void enlargedMapsGiveTheCameraRoomToScroll() {
        for (GameMap map : MapCatalog.createMaps()) {
            assertEquals(map.getType() == MapType.HUB ? 1080 : 1600,map.getBounds().width());
            assertEquals(map.getType() == MapType.HUB ? 400 : 1200,map.getBounds().height());
            var left = CameraFollow.viewFor(new CollisionBox(0,0,28,28),map.getBounds(),640,360);
            var right = CameraFollow.viewFor(new CollisionBox(map.getBounds().right()-28,
                    map.getBounds().top()-28,28,28),map.getBounds(),640,360);
            assertTrue(right.x()>left.x()); assertTrue(right.y()>left.y());
            assertTrue(right.right()<=map.getBounds().right()); assertTrue(right.top()<=map.getBounds().top());
            assertTrue(map.getObstacles().isEmpty());
        }
    }
}
