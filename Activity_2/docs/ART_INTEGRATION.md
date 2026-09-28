# Art integration (2026-09-19)

## Resource organization

- Imported `assets/art_resources/art/` to `assets/art/`, preserving category-relative paths.
- Moved the pack README and manifest into `assets/art/`.
- Removed only `maps/hub/hub_main.png` and `maps/hub/achievement_corridor.png` from runtime assets.
  Recoverable originals are in `/Users/zdy/.Trash/comp6120-unused-map-art-20260919/`.
- The original pack's remaining **220 PNGs** matched their pre-move SHA-256 hashes.
- Six additional Memorial Hall images were renamed into `art/memorial/`: `magic_circle.png` and
  `fire_stone.png`, `water_stone.png`, `lightning_stone.png`, `ice_stone.png`, `wind_stone.png`.
  The manifest retains each original UUID filename and 1254 x 1254 dimensions.
- Five Mage elemental bolt images are copied unchanged into `art/projectiles/boss/*_bolt.png`.
  Boss rendering doubles both dimensions, without resampling
  the files or modifying the Mage copies.
- The supplied exclamation was renamed to `art/boss/common/attack_warning.png` without changing
  its bytes. The manifest preserves its original UUID filename and now lists **232 PNGs**.
- `assets/art_resources/` remains empty. Existing assets outside that import were not overwritten.
- Hub gameplay, NPCs, quest acceptance, class selection and return-to-Hub are retained.

## Runtime boundary

The desktop module declares the shared `assets/` directory as main resources. Gradle copies it into
the runtime classpath and desktop JAR, which is included in the generated distribution. Internal paths
remain `art/...`; there is no hard-coded machine path or required working-directory override. Reload
the Gradle project in IDEA after this build-configuration change. Generated macOS distribution scripts
also carry the first-thread/native-access JVM options. RuntimeAssetsTest checks LibGDX's actual
internal-file lookup outside the asset directory, including every manifest image.

`ArtAssets` lazily owns textures, uses nearest-neighbour filtering, splits horizontal animation strips
and disposes its GPU cache. Filename frame counts distinguish character strips; documented 256-pixel
frames apply to portal/VFX/skill strips. Wide static arrows and map images are not accidentally sliced.

`SceneArt` reads existing positions, ability states, accepted cooldown changes, attacks, elemental
attachments, fields and Boss state. It does not advance combat or use sprite dimensions for collisions.
`PlaceholderRenderer` retains its historical name and camera/procedural-Hub responsibility, composing
the new presentation. `GameScreen` supplies existing observations and the capped HUD policy.

`CombatSystem` adds only a bounded (128) `HitFeedback` mailbox containing immutable geometry/element/
reaction/block observations after confirmed settlement. Reading it cannot deliver damage. Draining
consumes it once; scope clear removes it. No global event bus, art paths or class-specific damage rules
were introduced. Old-world visual effects, references, portal transitions and animation state reset
on travel. The separate short outcome HUD replay contains only an immutable BattleResult/class;
it does not retain Boss objects, delay travel, change Quest state or consume material rewards.

## What is displayed

- Three classes: directional idle/walk/J/Dash, K and both O modes, hit feedback and follow-through.
- Five guardian appearances: idle/chase, existing sweep/slam/heavy attack pictures, phase/hit pictures.
- Five elemental arena backgrounds. Their painted border is outside playable geometry; it does not
  introduce hidden walls after removal of the temporary test obstacles.
- Enabled/disabled portal, activation/loop/close; NPC, prop and decoration images.
- Shared arrow/elemental-bolt flight orientation, melee effects, fixed Arrow Rain/Magic Circle visuals.
- Element attachments, confirmed reactions, persistent ground fields and generic status overlays/icons.
- Class, J/K/L/O and selected Mage-element icons. Cooldown numbers/dimming use the existing controller.
  Icons obey HudLayout's size cap and detail-hide threshold; they never change weapon/inventory state.
- Success/failure displays a large **three-second non-blocking Hub banner**, with gold victory/red
  failure text, a dark bordered panel and death/defeat/disappear artwork. It scales down for smaller
  windows and stays visible independently of hidden skill icons or open menus. UI elapsed time, not
  paused gameplay time, controls expiry. Auto-return and full-HP recovery remain immediate. This is not
  a corpse left in Hub or a second drop/claim operation; the reward payload still uses existing tuning.

### Honest coverage limit

Bosses now emit their arena's elemental bolt through the shared projectile pipeline while pursuing.
The older `art/projectiles/boss/*_projectile.png` pack images remain archived runtime assets; the
requested Mage-derived `*_bolt.png` copies are the flight artwork actually displayed.
Melee attacks have a fixed red hitbox warning and accelerating exclamation flashes during a
0.8-second preparation. The supplied image is `art/boss/common/attack_warning.png`, rendered
above the Boss with its square source aspect ratio preserved; the geometric fallback is removed.
Animation selection is presentation-only; short gameplay windows can finish before all frames play.
Each guardian now owns one signature mechanic and its own movement speed through `BossTrait`, so the
five fights differ in play, but they still share one pattern set and one sprite pipeline: distinct
per-guardian movesets, art and audio cues are still separate work, as are balance and final UI
polish. See [Boss trait report](BOSS_TRAIT_REPORT.md). That original art import added no audio; the later three-track BGM integration is
documented in `assets/audio/README.md`. The original pack's provenance filenames are retained in its manifest.

## Larger maps and practice

Memorial Hall is a separate 640 x 360 world-unit room matching the camera's one-screen view.
Its floor uses simple geometry; the imported five-slot circle sits on the right. Guardian completion
lights the corresponding stone at its socket, while unearned stones remain dim. Old Sage appears
on the hall's left side for progress/ending dialogue and separately in Hub at (184, 221) as a
beginner guide, using the same existing artwork. The return portal sits at (32, 32); arrival at (100, 48) is outside
its trigger, preserving the existing arrival protection. These pictures do not change collision.
The Hall-only minimap is hidden and the circle fits the viewport, keeping every stone visible; the legacy weapon panel is now disabled.
Elsewhere the minimap and the expanded `[M]` map mark a living Boss with a red square, drawn after the
NPC markers and before the green player marker so neither hides the other.

Battle/training maps use centralized **1600 x 1200** world bounds; Hub uses **1080 x 400**.
The **640 x 360** camera follows/clamps normally, with room to scroll in both axes.
Hub positions (bottom-left world coordinates): Storage Chest (590, 120), Quest Keeper (347, 240),
Weapon Merchant (650, 250), forge (690, 250), Portal Guide (765, 245), quest portal (890, 245).
The merchant/forge offset is preserved; the portal is 125 units right of its guide. Hub's `north` spawn is now (64, 300)
to keep its full player body inside the shorter map. Spawn IDs and empty obstacle lists are unchanged.
Boss and training-target collision bodies spawn centred on the current map (800, 600 in built-in arenas).
The centre is calculated from map bounds, not image dimensions or a fixed lower-left coordinate.
Existing safety checks retain nearby fallback candidates when the centre is blocked/player-occupied.

Training remains `MapType.TRAINING`, with one original TrainingDummy and no Boss AI/reward/outcome.
On entry, `TrainingAppearance` makes independent random choices for elemental arena and guardian art.
Selection stays fixed during the visit and an R reset, while leaving/re-entering samples again (a repeat
is possible). The dummy stays still, refills its own HP and scores delivered damage. CombatDemo now launches fixed upward practice shots; these use the existing enemy projectile artwork while retaining physical damage rules.
The random selector accepts a seeded Random in tests and does not share the Mage ability RNG.

## Status overlay without new assets

Negative statuses are named above the actor they affect, in large uppercase text, using the built-in
bitmap font and the world-space name-tag batch that already labels the player and NPCs. `FROZEN`,
`STAGGERED`, `CHARGED`, `VULNERABLE` and `INVULNERABLE` come from shared combat state, so both the
player and a guardian report them; `BURNING xN` comes from the fire guardian's own trait because only
the trait holds its stack count. Control effects sort first and the immunity window is included so
that negated hits read as a mechanic rather than a missing hit. Anchors match the scene's visual
bodies rather than hitboxes, and the player's labels sit above its existing name tag.

No asset was added for this: the existing `art/status/frozen.png` overlay and status icons stay as
they are, and burning has text only. A dedicated burning icon remains optional future art work.
Edges are slightly soft at large window sizes because the font is scaled in world units, matching
the existing name tags. Up to five labels can stack above one actor.

## Verification

### Memorial Hall follow-up

- `./gradlew test build javadocAudit`: **53 suites, 983 tests, zero failures/errors/skips**;
  build and JavaDoc audit passed.
- A temporary LWJGL check loaded all **226** manifest images on the GPU and exercised the actual
  GameScreen menu inputs, Memorial Hall entry, five simulated Boss defeats through the existing
  settlement/quest flow, and an explicit E interaction with Old Sage. The ending stayed closed
  until that interaction. Unlit/lit stones and the ending dialogue were inspected in framebuffer images.
- Regression tests cover quest-free travel, return/arrival protection, preserved class/O selection,
  retained accepted tasks, each guardian's distinct stone, no stones on defeat, one-time ending,
  preserved reward outbox and empty import staging. No save/load was added.

### Original import verification

- `./gradlew test build javadocAudit`
  Final result: **51 suites, 961 tests, zero failures/errors/skips**; build and JavaDoc audit passed.
- ArtIntegrationTest: all manifest files/dimensions/strip layouts; excluded backgrounds/empty staging;
  seeded independent selection; stationary training/reset; enlarged camera bounds.
- HitFeedbackTest: once-only feedback, actual reaction result, miss/block distinction, bounded retention
  and map cleanup. Existing gameplay tests are retained.
- `./gradlew :lwjgl3:run` starts the real GameScreen without missing-resource exceptions.
- A temporary, non-repository LWJGL validation program loaded/sliced all 220 PNGs on the GPU and rendered
  Hub, a random training arena/guardian, projectile and fixed Mage field screenshots from the framebuffer.
  It observed delivered practice damage. This is scripted graphics verification, not a full manual playthrough.
  Success/failure HUD replays were also rendered and captured without retaining battle actors.

During development, rebuilding the core JAR beneath an already-running desktop process caused that old
process to report `NoClassDefFoundError: CollisionSweep`. The completed JAR contains the class and a
fresh graphics verification run exercised projectile collision successfully. Finish builds before
starting the game; restart old windows after changing code instead of replacing their live classpath JAR.

Manual follow-up: talk to Portal Guide, choose Training, walk into the portal; test WASD/J/K/L/O and R.
Return via the practice NPC. At Hub use Class Master for each class/O mode, and Quest Keeper then Portal
Guide for real Boss battles. Walk toward each map edge to check scrolling and resize to verify HUD hiding.
