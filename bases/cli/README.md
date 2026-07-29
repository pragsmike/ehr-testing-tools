# bases/cli

Thin CLI dispatch for `components/tools` — the `ehrt` command
("e-heart", R32/ADR-0009; `ehr` stays reserved for future payload-EHR
tooling). `ehrt sim run` dispatches straight into `components/sim`,
in-process, no subprocess (the `ehrt sim` mount, ADR-0005).

This base carries no user-facing documentation of its own — that
content moved to the root front door and the audience-forked doc paths
(`notes/ADRs.md` ADR-0010): [`README.md`](../../README.md) for what
this workspace is and the Quickstart; [`docs/`](../../docs/) for the
complete user path; [`docs/dev/`](../../docs/dev/) for maintainers.
`docs/dev/architecture.md`'s own bricks table documents this base
alongside every other one.
