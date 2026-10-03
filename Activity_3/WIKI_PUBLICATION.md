# Wiki publication record

Published to the team GitLab Wiki on **3 October 2026**, reviewed against main
`59de4e66120654605150c8baad4ca49dea39224f`. The English files in `wiki/` match the
published Markdown. Existing live pages were read before editing; teammate changes
were retained. The Docker page was reviewed and already matched its local copy.

- [Wiki Home](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/wikis/home)
- [Playtest and Screenshots](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/wikis/Playtest-and-Screenshots)
- [Documentation Index](https://gitlab.comp.anu.edu.au/u7782042/comp2120-fri11_a3_u7782042_u7998420_u7851266_u8350466/-/wikis/Documentation-Index)

## Published changes

- Refreshed the existing overview, features, engine, tester, installation, player guide,
  user stories, planning, known issues, reflection and license references.
- Added a dedicated native evidence gallery, including five-guardian settlement,
  Memorial Hall, the three ending lines, all five Credits pages and ending restoration.
- Added an assignment-to-evidence map and the full Activity 2 document catalogue.
- Added both new pages to Home and the existing custom sidebar.
- Embedded 21 distinct Activity 2 game screenshots across the Wiki. The gallery itself
  contains 17; the complete repository index retains all 41 captures.
- Replaced undated feature illustrations with dated repository images and removed
  obsolete claims that Hall, ending and Training evidence had not been captured.
- Recorded the merged Training fix separately from the original campaign binary and
  retained the remaining native input, audio, balance and packaging checks.

Images use absolute GitLab raw URLs pinned to `59de4e6`; they do not depend on local
filesystem paths or duplicate Wiki uploads. Detailed source/document links use the same
commit. The old CI screenshot on Game Tester remains explicitly labelled as pipeline
#1052082 from 2 October; the current result is linked separately.

## Verification

- All 14 content pages were read back through their editors and matched the local drafts.
- The custom sidebar was published with both new navigation links.
- All 120 repository file/image references resolve to existing paths in the reviewed
  checkout; Wiki page links match the published page slugs.
- All 17 gallery images loaded successfully in the rendered Wiki, including every
  ending/Credits image and the fixed Training layout.
- Main pipeline **#1052436** and build job **#7717341** were inspected directly:
  `./gradlew clean build javadocAudit --no-daemon`, JDK 25 image, Gradle 9.7.1,
  **1,302 passed, zero failures/errors/skips**, both JavaDoc audits and build successful.
- Markdown source contains English prose; later saves use explicitly written English
  Wiki commit messages. Earlier saves in this publication session used GitLab's
  localized default messages before the English-only instruction; those historical
  entries were not rewritten.

The local code repository was not committed or pushed by this publication task.
The published Wiki is a separate repository; committing these source copies alone is
not a replacement for publishing future Wiki changes.

## Keep evidence current

1. Read the live page before editing and preserve teammate changes.
2. Update the affected Activity 2 report and the related Wiki overview together.
3. Keep screenshot capture versions and historical test execution records unchanged;
   add a new dated result when a new run is actually performed.
4. Use English page content and explicit English Wiki commit messages.
5. Preview and verify image loading, internal navigation and repository links after saving.
6. Check the final submission commit's own pipeline and assessor access. Complete the
   remaining native checks in `Activity_2/docs/PLAYTEST_REPORT.md` before claiming them.
7. Add measured line/branch coverage only when produced by a coverage tool; a test
   success percentage is not code coverage. Do not invent meeting minutes.
