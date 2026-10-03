# Automated game tester and verification

Tester instructions updated on 2026-10-03 for the additional coverage pass based on
`4030a7b`. Historical results below retain their original revisions and dates.
See [Activity 3.3 — automated testing and coverage](../../Activity_3/3.3/REPORT.md) for the
test scenarios, final measured coverage and archived execution evidence.

## What the tester is

The automated game tester is the set of JUnit gameplay scenarios that drive the same
GameSession, PlayerCommand, TravelSystem, EncounterFlow, PlayerAbilities and CombatSystem
used by the game. They submit commands/actions, advance bounded simulation frames, and
assert resulting state: positions, HP, admission, rewards, map transitions and saved progress.

These higher-level scenarios are distinct from normal unit tests for individual geometry,
parsing or arithmetic methods, although both use the Gradle/JUnit runner. There is no separate
native window-driving executable or standalone smoke-test application. The console is a manual
debug tool; it does not assert outcomes or replace the tester.

`ScreenFlowTest` also runs real screens with injected keyboard/pointer events, real
font/image decoding and recording GPU/audio devices. It covers menus, settings,
NPCs, bag/chest actions, save/load, Training combat and the Hall/Credits sequence.
These checks validate application state and device calls, not native pixels or sound.

Some fixtures arrange an initial position or directly settle a Boss for a focused persistence
check. Other scenarios use real attacks to complete fights. A direct death fixture must not
be described as a full mouse/keyboard playthrough.

## Dependencies and commands

Use JDK 25 and the checked-in Gradle wrapper. Full setup is in the [game README](../README.md).
Run the following from `Activity_2`. On Windows substitute `./gradlew.bat`.

Full build, both test modules and strict JavaDoc audit (same command as CI):

```sh
./gradlew clean build javadocAudit :core:jacocoTestReport --no-daemon
```

Force both test tasks to execute again:

```sh
./gradlew test --rerun-tasks --no-daemon
```

Run the higher-level gameplay scenarios separately from the complete unit suite:

```sh
./gradlew core:test --rerun-tasks --no-daemon \
  --tests '*CoreGameplayIntegrationTest' \
  --tests '*QuestPortalFlowTest' \
  --tests '*BossQuestFlowTest' \
  --tests '*PortalGuideTrainingTest' \
  --tests '*PlayerDefeatTest' \
  --tests '*MemorialHallTest' \
  --tests '*EquipmentIntegrationTest' \
  --tests '*PotionGameplayTest' \
  --tests '*GameSavesTest'
```

Run movement/wall and illegal-operation checks:

```sh
./gradlew core:test --rerun-tasks --no-daemon \
  --tests '*PlayerTest' --tests '*CollisionSweepTest' \
  --tests '*WorldOcclusionTest' --tests '*CombatOcclusionTest' \
  --tests '*ProjectileTest' --tests '*TravelSystemTest' \
  --tests '*GearUpgradesTest' --tests '*GameSavesTest'
```

JUnit reports failures through Gradle's nonzero exit status and assertion output. Passing
runs end with `BUILD SUCCESSFUL`. HTML reports are in `core/build/reports/tests/test/index.html`
and `lwjgl3/build/reports/tests/test/index.html`; XML reports are under each module's
`build/test-results/test/`. A filtered run reports only its selected cases, not the full-suite
total. Parameterized test invocations are counted separately. Tests use substitutes for
LibGDX input/audio where appropriate and temporary directories for save-file scenarios.

## Command/action-to-state examples

Test paths in the table are relative to `core/src/test/java/au/edu/anu/comp2120/`.

| Scenario | Input/action | Expected state asserted by the tester |
| --- | --- | --- |
| Admitted movement/travel | Prepare permission/route, then submit movement frames into a portal | Correct named spawn, same Player, correct collision world, one Boss or one Training target, no immediate return bounce |
| Class battle completion | Select each class and A/B mode; arrange starting range; issue normal ability commands | Real Boss death, Hub return, one reward and closed successful permission |
| Invalid admission | Enter without matching permission or attempt a locked route | Rejection/no unauthorized encounter |
| Defeat and retry | Deliver lethal damage, including simultaneous Player/Boss death | Failure priority, full-HP Hub recovery, no victory reward/autosave, retained retry task |
| Wall movement | Move/dash across a thin obstacle or beyond bounds in a test map | Body stays in walkable space; no tunnelling |
| Attack obstruction | Fire projectiles or submit fixed attacks across explicit walls | Wall prevents delivery/attachment; equal-time projectile wall hit wins |
| Potion use | Press P once/hold it, use bag stock, test full/dead/empty states | One eligible consumption, capped healing, refusals without consumption, no chest-stock consumption |
| Upgrade rejection | Request unowned/capped gear or insufficient material | No item/material mutation; debug free upgrade still respects ownership/capacity/+2 |
| Safe snapshot round trip | Save, mutate state, restore selected slot | Position/HP/class/equipment/chest/quest/win counts restored, other slot unchanged |
| Illegal or corrupt load | Load during unsafe play or use malformed/inconsistent snapshot | Refusal before partial live mutation |
| Debug victory | Enable cheat, settle success and request save | Reward/quest settlement still occurs, normal save files remain unchanged |
| Ending | Complete five distinct targets and interact in Memorial Hall | Exactly five stones and one ending consumption; repeat wins do not add stones |

Concrete production scenario examples are in
[CoreGameplayIntegrationTest](../core/src/test/java/au/edu/anu/comp2120/screen/CoreGameplayIntegrationTest.java):
`admissionByMovementUsesCurrentMapCollisionAndOneTarget` and
`actualCombatCompletesOneRewardAndClosesPermission`.
The former sends `new PlayerCommand(1, 0, false, false, false, false)` in 0.1-second frames
and asserts arrival/map/target state. The latter covers all six class/ultimate combinations
using ordinary ability commands; it arranges starting position but does not set either actor's HP.

[GameplayIntegrationTest](../core/src/test/java/au/edu/anu/comp2120/screen/GameplayIntegrationTest.java)
also uses an unrestricted component harness and direct travel fixtures. Its shortcuts are
not production controls; production permission tests live in CoreGameplayIntegrationTest and
the quest/portal suites. Built-in arenas are open, so wall scenarios deliberately construct
obstacles instead of claiming visible campaign walls exist.

## Suite map

| Area | Representative test classes |
| --- | --- |
| World/movement/navigation | PlayerTest, GameMapTest, MapCatalogTest, CollisionSweepTest, AStarPathfinderTest, EnemyNavigationTest |
| Production travel/outcomes | CoreGameplayIntegrationTest, QuestPortalFlowTest, BossQuestFlowTest, PlayerDefeatTest, MemorialHallTest |
| Health/combat/visibility | HealthTest, HealthCapacityTest, CombatSystemTest, ProjectileTest, CombatOcclusionTest, WorldOcclusionTest |
| Classes and reactions | PlayerAbilitiesTest, FinalClassSkillsTest, ElementClassIntegrationTest, ReactionCompletionTest, CombatCapabilitiesTest, PlayerControlEffectsTest |
| Boss mechanics | BossTest, BossAttackTest, BossTraitTest, BossScalingTest |
| Items/services | InventoryTest, EquipmentIntegrationTest, GearUpgradesTest, PotionSupplyTest, MerchantServicesTest, PotionGameplayTest |
| Save parsing/restoration | SaveFileTest, SaveStoreTest, InventorySnapshotTest, LocationSnapshotTest, SaveSelectionTest, GameSavesTest |
| Menus/settings/objectives | MainMenuScreenTest, GameSettingsTest, GameScreenObjectiveTest, MenuKeysTest, SaveMenuTest, CreditsDialogueTest |
| Training/debug/presentation models | PortalGuideTrainingTest, CombatDemoTest, DebugOptionsTest, DebugConsoleTest, HudLayoutTest, PotionFontLayoutTest, PotionFeedbackTest, StatusLabelsTest |
| Audio and packaged resources | BackgroundMusicTest, MusicVolumeTest, ArtIntegrationTest, desktop RuntimeAssetsTest |

Font-metric and layout assertions do not capture GPU output. Fake Music verifies selection,
looping/gain/lifecycle calls without proving the actual audio device plays correctly.
RuntimeAssetsTest checks packaged resource access, not visual appearance.

## Measured core coverage

JaCoCo now reports all core production classes, including rendering code, without
custom exclusions. From `Activity_2`, regenerate the complete core measurement with:

```sh
./gradlew :core:test :core:jacocoTestReport --rerun-tasks --no-daemon
```

Open `core/build/reports/jacoco/test/html/index.html`; the corresponding XML is
`core/build/reports/jacoco/test/jacocoTestReport.xml`. Filtered test runs cover only their
selected scenarios, so rerun the full command before comparing results.
[Activity 3.3](../../Activity_3/3.3/REPORT.md) records the final measurement and regression/integration
scenarios. CI retains JUnit and coverage reports; manual input, graphics and sound still
need separate checks.

The final local full build on 3 October 2026 passed **1,449 core and 4 desktop tests**,
with zero failures/errors/skips and both strict JavaDoc audits successful. All-core
coverage was **5,681/5,819 lines (97.63%)**, **3,843/4,336 branches (88.63%)** and
**36,176/37,097 instructions (97.52%)**. This is local execution evidence, not a new
remote pipeline result. [Final raw XML archive](../../Activity_3/3.3/coverage-evidence.zip).

## Historical CI result

The GitLab report inspected on **2026-10-02** for commit
`1a2c60c2b7602e2501d86ac380c364b1d704b175` reports **1,302 passed, 0 failures,
0 errors and 0 skipped tests** in
[pipeline #1052082](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/pipelines/1052082).
[Build job #7716451](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/jobs/7716451)
ran `./gradlew clean build javadocAudit --no-daemon` in Activity_2 with the Docker executor,
`eclipse-temurin:25-jdk` and Gradle 9.7.1. Both modules' tests and JavaDoc audits completed;
the log ends `BUILD SUCCESSFUL`. This establishes expected results for the executed automated
assertions at that source revision.

A Git comparison confirms that `1a2c60c..7fea86a` has only Markdown changes under Activity_2,
with no source/assets/build or root CI changes. The later working-tree Training HUD fix is
a source change; its separate local verification is recorded below, not attributed to this CI run.
The CI result belongs to `1a2c60c`; it is not a new execution at `7fea86a`. The earlier local
wrapper-cache lock failure was not a successful test run and no approval request remains
pending for it. The newer local coverage run is recorded separately in Activity 3.3.

The [Wiki tester page](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/wikis/game-tester)
contains the previously captured CI screenshot. Passing all tests is not a measured line/branch
coverage percentage, proof that every path is tested, or a native end-to-end playthrough.

## Native execution record

On **2026-10-03**, the current native game at `1ae3c78` was launched and exercised through
the desktop UI. The [dated playtest report](PLAYTEST_REPORT.md) records environment,
position-only fixtures, steps, expected/actual results, observed issues and 41 real screenshots.
Verified scenarios include New Game, keyboard settings/fullscreen, manual save/load/cancel,
storage/equipment, full-health potion rejection, merchant guards, Fire quest/portal entry,
all five guardian wins, reward/autosave restoration, relaunch/Continue, Training score/reset,
Wind defeat/return, Hall guidance/ending and all five Credits pages. The user completed Wind
without debug features; the agent performed the other recorded UI checks. These results are
independent of the earlier CI report.

The session reached the ending using disclosed travel-position fixtures and user assistance
for Wind. It does not establish exhaustive gameplay acceptance or a fresh full Gradle test execution. The Training overlap was subsequently fixed and verified in a freshly compiled native app;
off-screen Mage targeting remains an observed balance issue. The
[fix verification](PLAYTEST_REPORT.md#training-overlap-fix-and-verification) records 83 passing
selected JUnit tests and strict JavaDoc for the changed file using local JDK tools. This was
not a full Gradle-suite run.

## Manual acceptance still needed

Use the following remaining checks alongside the [executed N01–N16 results](PLAYTEST_REPORT.md#executed-checks):

1. New Game, Continue and title settings/relaunch were checked in N01/N02/N10. Still compare
   Esc session volume with title preferences and test physical mouse control.
2. At 640 × 360, 960 × 540, 1920 × 1080 and a smaller window, check all three class bars,
   potion counts, objective wrapping, dialogue and resized mouse hit areas. Detailed HUD should
   hide/reappear at its threshold. Hall minimap/objective hiding and Sage marker were observed
   at 960 × 540; still check their behavior across the resize matrix.
3. Exercise NPC, bag/chest, map, console and pause menu with keyboard and mouse. Opening/closing
   frames must not also move, attack or use P. Check potion feedback and held-key behavior.
4. Five wins, Wind defeat/retry, Hall ending and Credits were checked in N07–N16. Still test
   all class/guardian matchups, detailed warning/trait behavior, rematches and exact defeat
   preservation boundaries. The fixed-position Mage approach does not cover normal dodging balance.
5. Verify the Training upward shot, armour mitigation, five-second recovery and stable R reset;
   pause mid-recovery to check simulation time stops.
6. Listen across map changes, volume changes, lifecycle pause/resume and restart on a real device.
7. Current title/settings, Hub, battle-state, Training, inventory and save screenshots are in
   [the local gallery](ART_INTEGRATION.md#native-screenshot-gallery). Still capture visible
   representative attacks/traits for the remaining guardians and classes. Hall ending and all
   Credits pages are now captured. The Wiki is not updated.

Record actual outcomes and defects after executing these checks; unchecked items remain evidence
gaps, not fabricated passes or automatic declarations that the features are broken.
