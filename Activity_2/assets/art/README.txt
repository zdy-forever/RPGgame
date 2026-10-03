COMP2120 / COMP6120 Pixel Art Asset Pack

Reviewed 2026-10-02 against commit 7fea86a.

Current inventory: 232 PNG files and 232 ASSET_MANIFEST.csv records. Manifest paths are
relative to assets/; each record retains source filename and image dimensions. Animation
strips are horizontal. Runtime texture/frame ownership belongs to ArtAssets and SceneArt.

Style: pixel art with a top-down / semi-top-down medieval fantasy RPG perspective,
coordinated colours and readable silhouettes. Single-object assets use transparency where applicable.

The pack includes three classes, five guardians/arenas, NPCs, props, portals, skill/reaction
artwork, UI/status images, six Memorial Hall images, five Boss bolt copies and an attack warning.
Boss flight rendering uses projectiles/boss/*_bolt.png; older *_projectile.png images remain
in the pack but are not the current Boss flight selection. Not every imported image is used
by the current renderer. Hub floor and potion quickbar geometry do not add PNG records.

Katana action/VFX concepts draw on Monster Hunter Rise long-sword timing (parry/counter,
stance and follow-up) without copying its characters, UI or art assets.

Recorded image provenance and license boundaries are in assets/README.md. Current rendering,
HUD and native verification limits are in Activity_2/docs/ART_INTEGRATION.md from the repository root.
