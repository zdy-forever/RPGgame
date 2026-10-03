# Asset attribution and packaging

Reviewed 2026-10-02 against the assets and packaging configuration at `7fea86a`.
This page preserves the project's recorded provenance; it does not establish new licensing terms.

## Visual assets

The imported raster artwork used in this project was generated using OpenAI image generation.
Some presentation, including the Hub background, debug geometry and potion bottle/count, is
drawn by code and is not an imported image asset.

The current art directory contains 232 PNGs, matching the 232 records in
[art/ASSET_MANIFEST.csv](art/ASSET_MANIFEST.csv). The manifest includes relative paths,
source filenames and dimensions. [art/README.txt](art/README.txt) describes the pack and strip format.
The proprietary source-code license does not cover these visual assets.

## Music

The project's recorded provenance states that all three shipped music tracks were generated
using Suno during an active paid subscription. They are used under the rights associated with
that subscription and are not covered by the proprietary source-code license.
See [audio/README.md](audio/README.md) for filenames, format, playback and volume behavior.

## Packaging and source code

The desktop Gradle module adds this directory as main resources. Art/audio paths are relative
to this root; IDE/Gradle/distribution classpaths receive the same asset tree. `art_resources/**`,
`.DS_Store` and `.gitkeep` are excluded from desktop resources.

The source code is proprietary; see the repository-root `LICENSE.md`.
Rendering/verification documents are `Activity_2/docs/ART_INTEGRATION.md` and
`Activity_2/docs/TEST_COVERAGE_REVIEW.md` (paths from the repository root, outside this resource root).
