# EXP-<id> — Results

<!-- Soft type (pattern nursery #12): a template/rubric pair for prose
     artifacts that can't be hard-schema'd. Fill in every section; do not
     omit the self-score at the end. Findings and classifications only --
     design conclusions, F-rows, and pattern promotions happen in the
     design channel afterward, not in this file. -->

## Metadata

- **Experiment:** EXP-<id>
- **Date:** YYYY-MM-DD
- **Executor:** (agent/session identifier)
- **HEAD at execution:** (git commit sha)
- **Protocol:** link to the protocol doc this results file executes

## Environment record

<!-- Everything an independent party would need to know to judge whether
     a divergence is real or an artifact of this run's setup. -->

| Field | Value |
|---|---|
| OS / kernel | |
| JVM(s) used | |
| Locale / timezone (host default) | |
| Artifact(s) resolved (name, version, sha256) | |
| Config file(s) used (path, sha256) | |

## Per-round findings

<!-- One table per round. A round with zero divergences still gets a row
     stating "no divergence" -- silence is not evidence of determinism. -->

### Round: <name>

| Divergence observed | Field(s) | Classification (pin / control / canonicalize) | Action taken |
|---|---|---|---|
| | | | |

## Protocol amendments made

<!-- Corrections to the protocol discovered during execution, with date
     and reason. Protocols are corrected loudly, not silently -- if none,
     say so explicitly rather than leaving this blank. -->

## Acceptance verdict

<!-- Judge strictly against the protocol's own Acceptance and Stop
     Condition sections -- quote them, then state whether they were met. -->

- **Acceptance criterion:** (quote from protocol)
- **Met?**
- **Stop condition triggered?**

## Artifacts produced

| Artifact | Path | Hash |
|---|---|---|

## Rubric self-score

<!-- Score against docs/experiments/results-rubric.md. This section is
     mandatory -- a results file that doesn't self-score is incomplete. -->

| Criterion | Met? | Evidence |
|---|---|---|
