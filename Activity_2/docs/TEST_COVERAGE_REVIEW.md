# Test coverage and documentation review — 2026-09-30

## Status and evidence boundary

This is a source/test/documentation audit of the current working tree, not a coverage-percentage
measurement or a claim that all code paths have tests. No production behavior is changed by this
review. Existing tests are retained; old branch results and contribution attributions are not rewritten.

Attempted from the repository root:

```sh
./Activity_2/gradlew -p Activity_2 test
```

It exited before Gradle started because the sandbox could not open the wrapper cache's `.zip.lck`
file under the user's Gradle directory (`Operation not permitted`). An elevated request was submitted
from Activity_2 for:

```sh
./gradlew test javadocAudit
```

That request is awaiting authorization. **There is no current passing test total, compilation result
or strict JavaDoc result.** Prior successful runs in other reports remain dated historical evidence.
All changed/new test declarations have JavaDoc in source. Once authorized, rerun the command above;
it includes core and desktop tests plus private/test JavaDoc. Do not delete tests to obtain green results.

## Newly filled gaps in this review

| Gap | Added test | Verification performed by the test |
| --- | --- | --- |
| Saved win counts might restore as data but not affect new encounters | `save/GameSavesTest.restoredWinsScaleOnlyTheMatchingGuardian` | Five guardian cases, each loading both AUTO and MANUAL after two victories; unrelated counts remain zero, the next Boss has scaled/full HP and phase one. |
| Icon rectangles fitting does not guarantee real text fits | `screen/PotionFontLayoutTest` | Reads LibGDX's actual default font metrics; measures x0–x999 and 999+ at five window aspect/size combinations. Texture coordinates are skipped; this is not GPU screenshot testing. |
| Combined-key tests do not independently prove every advertised menu alias | `screen/MenuKeysTest` | Every direction/confirm alias separately; paging same-direction aliases step once, opposite directions cancel, P/J/K/L/O do not become menu actions. |

These add 12 JUnit invocations including parameterized cases; each invocation may contain multiple
assertions/scenarios. This is a source-level count, not an executed passing count.

## Recent regression coverage retained

- `player/PotionGameplayTest`: all classes, effective HP boundaries, no held-P repeated consumption,
  stock isolation, map transitions, equipped maximum HP and unchanged class/cooldown/equipment state.
- `screen/PotionFeedbackTest`: actual healing, exact remaining stock and distinct refusal reasons;
  no double-consumption when stock reaches zero.
- `screen/SaveMenuTest`: non-modal potion toast expires after four seconds without healing again;
  existing menu input, safe-state, confirmation, deletion/restart and volume checks remain.
- `screen/HudLayoutTest`: centered potion/optional-element slots, scale cap, visibility/recovery,
  compact count thresholds and negative-count rejection.
- `save/GameSavesTest`: manual/restart potion quantities, victory autosave and defeat/retry without refunds.
- `screen/PortalGuideTrainingTest`: current NPC/skill instructions and save/load onboarding in both
  Hub Old Sage and Notice Board; removed U/1/2/3 travel guidance stays absent.

## Broader suite map

The existing suite already exercises the following areas; this map identifies relevant tests rather
than implying perfect line/branch coverage or native-window acceptance.

| Area | Representative tests |
| --- | --- |
| Movement/world/navigation | PlayerTest, GameMapTest, CollisionSweepTest, AStarPathfinderTest, EnemyNavigationTest |
| Travel/admission/outcomes | TravelSystemTest, QuestPortalFlowTest, CoreGameplayIntegrationTest, PlayerDefeatTest, BossQuestFlowTest |
| Shared combat/health/occlusion | CombatSystemTest, HealthTest, HealthCapacityTest, ProjectileTest, CombatOcclusionTest, WorldOcclusionTest |
| Classes/timing/statuses | PlayerAbilitiesTest, FinalClassSkillsTest, ElementClassIntegrationTest, PlayerControlEffectsTest |
| Elements/derived safety | ElementRulesTest, ElementCombatTest, ReactionCompletionTest, CombatCapabilitiesTest |
| Boss/traits/scaling | BossTest, BossAttackTest, BossTraitTest, BossScalingTest |
| Inventory/merchant/upgrades | InventoryTest, EquipmentIntegrationTest, GearUpgradesTest, PotionSupplyTest, MerchantServicesTest |
| Save parsing/atomicity/restoration | SaveStoreTest, InventorySnapshotTest, LocationSnapshotTest, SaveMetadataTest, GameSavesTest |
| UI/audio/assets | DialogueLayoutTest, HudLayoutTest, DebugConsoleTest, SaveMenuTest, BackgroundMusicTest, MusicVolumeTest, ArtIntegrationTest, desktop RuntimeAssetsTest |

## Gaps that still require native/manual QA

1. At 640x360, 960x540 and 1920x1080, inspect all three class bars: P bottle, zero/ordinary/999+
   counts, Mage element and cooldown labels. At widths/heights below detail thresholds the bar hides;
   resizing back must restore it. Font metrics do not prove filtering, clipping, HiDPI or GPU state.
2. Press P while injured, full and empty; confirm visible four-second feedback and one consumption.
   Hold P; ensure no repeated use. Open/close bag, dialogue, map, console and Esc menu; their input
   must not leak to movement, attacks or potion use on the consumed frame. Unit tests of adapters
   are not full GameScreen keyboard/pointer integration tests.
3. Read Old Sage/Notice Board pages at small windows; confirm wrapping and menu footer separation.
   Follow the save instructions using both Esc menu and platform-appropriate Fn+F5/Fn+F9.
4. Complete a real Boss victory, inspect automatic-save feedback, restart and load; verify rematch
   difficulty and inventory. Automated fixtures settle Boss death directly, not through a full fight.
5. Listen during Hub/Memorial travel and volume changes. Fake audio players do not validate devices.

No screenshots or manual acceptance are claimed for this review. Do not launch an indefinitely
blocking computer-use call or treat waiting for permission as completed work.

## Documentation audit

Completed read-only checks (rechecked 2026-10-01): all 89 local file links in the 17 Markdown files resolve;
the art directory contains 232 PNGs and its manifest contains 233 lines including the header.
This link check verifies target files, not Markdown heading anchors or external websites.

Reviewed Activity_2's project Markdown documents and `assets/art/README.txt`; build outputs and
dependency caches are excluded. The game README and Architecture now describe current potion HUD,
modal input, save teaching and Training behavior. Save/inventory/UI/art reports link to this execution
status. Older combat, defeat, quest, class and element reports explicitly separate historical
prototypes from current APIs. Asset documentation distinguishes imported raster images from
procedural UI and corrects the pack's current count to 232 PNGs (233 CSV lines including the header).
Audio behavior and attribution are unchanged. Existing authors and historical commit/MR references
are preserved, not newly verified or invented by this audit.

## Inspection cleanup — 2026-10-01

Removed redundant GameScreen input branches, the unused Boss import, and already-disabled
coordinate/weapon renderers with their unused weapon cache. Existing modal early returns remain.
NPC label placement keeps paired integer steps above/below; no floating-point division is introduced.
Removed two unused test IOException declarations and their obsolete JavaDoc tags. The deliberate
null-player contract check is now its own test, with only that test's IDE data-flow warning suppressed.
Asset documentation uses repository-relative path text for the two files outside IDEA's assets
resource root. The 89 remaining local Markdown links resolve and `git diff --check` passes.
The full `./gradlew test javadocAudit` attempt was again blocked at the wrapper cache lock;
the elevated retry awaits authorization. Compilation, test success and a fresh IDEA inspection
are not claimed for this cleanup.
