# Art integration (2026-09-19)

## Resource organization

- Imported `assets/art_resources/art/` to `assets/art/`, preserving category-relative paths.
- Moved the pack README and manifest into `assets/art/`.
- Removed only `maps/hub/hub_main.png` and `maps/hub/achievement_corridor.png` from runtime assets.
  Recoverable originals are in `/Users/zdy/.Trash/comp6120-unused-map-art-20260919/`.
- The remaining **220 PNGs** matched their pre-move SHA-256 hashes. The manifest now lists those 220.
- `assets/art_resources/` remains empty. Existing assets outside that import were not overwritten.
- Hub gameplay, NPCs, quest acceptance, class selection and return-to-Hub are retained.

## Runtime boundary

The desktop module declares the shared `assets/` directory as main resources. Gradle copies it into
the runtime classpath and desktop JAR, which is included in the generated distribution. Internal paths
remain `art/...`; there is no hard-coded machine path or required working-directory override. Reload
the Gradle project in IDEA after this build-configuration change. Generated macOS distribution scripts
also carry the first-thread/native-access JVM options. RuntimeAssetsTest checks LibGDX's actual
internal-file lookup outside the asset directory, including all 220 manifest images.

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
- Success/failure plays a brief **non-blocking Hub HUD replay**, including death/defeat/disappear and a
  themed material picture on success. Auto-return and full-HP recovery remain immediate. This is not
  a corpse left in Hub or a second drop/claim operation; the reward payload still uses existing tuning.

### Honest coverage limit

The five `art/projectiles/boss/*_projectile.png` images are relocated, GPU-load-validated and supported
by the generic projectile renderer, but **not currently emitted in gameplay**: existing Boss patterns
are sweep/slam, not projectile attacks. No new hostile mechanics were invented to force image use.
Animation selection is presentation-only; short gameplay windows can finish before all frames play.
Balance, five unique elemental Boss movesets, art authorship/licensing review and final UI polish remain
separate work. No audio was added. The original pack's provenance filenames are retained in its manifest.

## Larger maps and practice

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
is possible). The dummy stays still, does not attack, refills HP and scores delivered damage as before.
The random selector accepts a seeded Random in tests and does not share the Mage ability RNG.

## Verification

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
