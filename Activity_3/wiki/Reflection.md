# Reflection

[Home](home) · [Planning and Schedule](Planning-and-Schedule)

## Keeping documentation aligned with implementation

During development, we discussed changing requirements in team meetings, so the developers understood the latest agreed behavior. However, we did not update the documentation at the same pace as the implementation. Some reports therefore continued to describe earlier controls or behavior even though the team already knew what had changed.

Shared understanding within the team was not enough to keep the written instructions reliable for players and assessors. We later reviewed the Activity 2 reports against the code and updated the Wiki. In future, each agreed requirement change should include a documentation task with a clear owner, and the related instructions should be updated as part of the same feature or merge request. This would reduce the amount of documentation work left until the end.

## Commit and issue granularity

Our usual process was to write an issue draft and then start development. As implementation made the scope clearer, we gradually created and expanded the corresponding issue in GitLab. This helped us record decisions as they emerged, but it meant that the shared issue sometimes followed the work instead of providing a complete plan before it began. Some planning entries also remained “to be created” after related work already existed.

A better approach would be to agree on a complete initial plan before starting implementation: define the scope, break the work into tasks, identify dependencies, assign responsibilities and specify acceptance checks. Publishing that plan as an issue first would give development a clearer order and make progress easier for the whole team to follow. We could still update the issue when implementation reveals new information, while keeping the reasons for scope changes visible.

Commit descriptions could also be more specific. For example, commits `a4c7e9f` and `bbe814b` share the title “feat: improve pause menu, save management and equipment progression”, while `aa2a8d5` is titled “update README.md”. Describing the concrete change in each commit and linking the issue from its merge request would make the work easier to review and trace.

## Tracking time and meeting the deadline

We rarely recorded time spent in issues or merge requests. Although we completed the work before the submission deadline, we have little recorded evidence for comparing estimated effort with actual effort or understanding which tasks took longer than expected. Meeting the deadline alone does not show that our estimates or workload allocation were accurate.

For future projects, we should record an initial estimate in each issue and update the time spent as work progresses, with a short summary in the merge request when useful. Reviewing these records at milestones would help us spot delays earlier, distribute work more evenly and make better estimates for later tasks.

## What testing evidence can establish

We sometimes found bugs during manual playtesting even when every CI check was green. For example, the Objectives panel overlapped the Training information in the running game despite passing automated checks. A successful CI run shows that the configured checks passed for that revision; it does not prove that every player interaction, screen layout or gameplay sequence works correctly.

We should therefore treat CI as one source of verification evidence and combine it with manual testing of the actual game. Before accepting a feature, we should run the relevant automated checks, play through its user-facing behavior and record any bugs with reproduction steps and expected versus actual results. Where practical, a manually discovered bug should also lead to a regression test. This would make CI more useful over time while retaining the manual checks needed for rendering, input and the overall gameplay experience.
