# Reflection

[Home](home) · [Planning and Schedule](Planning-and-Schedule)

This reflection describes specific repository/process examples and improvements. It does not substitute for fixing remaining defects or missing deliverables.

## Keeping documentation aligned with implementation

Several early descriptions became stale as the project grew. The class-skills report originally assigned F5 to ultimate-mode selection; production later reserved F5 for manual saving and moved ultimate selection to Class Master. Earlier Training descriptions called the target non-hostile; later armour-testing shots changed that behavior. The initial Home listed Docker among technologies before a project Dockerfile existed in the inspected baseline.

These discrepancies can send players or assessors through obsolete steps. Dated integration notes now distinguish historical reports from current behavior, and this Wiki uses current controls and explicit implementation limits. Future feature reviews should include a documentation checklist covering controls, setup, tests and screenshots. Historical reports should remain dated rather than silently masquerading as current manuals.

## Commit and issue granularity

Commits `a4c7e9f` and `bbe814b` share the title “feat: improve pause menu, save management and equipment progression”; `aa2a8d5` is titled “update README.md”. The first two titles make separate changes difficult to distinguish without opening their diffs, while the README title does not explain what instructions changed. Some planning entries also remained “to be created” after related work existed.

This weakens traceability between requirements, implementation and acceptance checks. A better workflow is to open a focused issue before implementation, reference it in the MR, and describe the concrete behavior changed in each commit. Retrospective records should retain their true creation dates. Already shared history should be explained honestly rather than rewritten solely for presentation.

## Shared-file integration

The source branch became eight commits behind its target during a later MR. Synchronizing required reconciling both `ARCHITECTURE.md` and the documentation index; merge commit `6d8b8a3` records that integration. Shared documents collect contributions from multiple feature streams, so editing an old baseline can create conflicts or discard another contributor's updates if resolved carelessly.

Smaller integration intervals and reviewing the target branch before broad documentation edits would reduce this risk. Conflict resolution should preserve both relevant contributions and rerun applicable checks before pushing.

## What testing evidence can establish

The F10 console originally failed because the desktop backend cancelled the native text-input path; model-level command tests did not establish that an actual desktop dialog opened. The final console uses an in-game input adapter. Similarly, a successful layout-model test does not prove that resized text and pointer targets look correct on screen.

These cases show why automated logic checks and manual desktop checks must be recorded separately. We added targeted regression tests for console input ownership and Training reset/pause behavior, and maintain a visual acceptance checklist. Future work should attach actual platform/check results and measured coverage rather than treating test counts as complete verification.
