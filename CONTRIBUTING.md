# Contributing

Two doors, depending on what brought you here.

## Using something this workspace builds

You want to run a built artifact (e.g. generate synthetic traffic with
`sim-cli`), not change this repo. Go to [`SETUP.md`](SETUP.md) —
installation and verification. Nothing below applies to you.

## Contributing code or docs

Read [`AGENTS.md`](AGENTS.md) and [`AUTHORS-GUIDE.md`](AUTHORS-GUIDE.md)
**before opening a PR** — not after, and not skimmed. The discipline
they describe is real, mechanically checked in places, and not
optional ceremony:

- **Git operations are WSL-only**, and commits/pushes are the author's
  ceremony unless explicitly delegated for a given session — see
  `AUTHORS-GUIDE.md` §1 before your first commit. Enforced by a
  pre-commit hook, not merely requested.
- **`clojure -M:poly check` and `clojure -M:poly test :project` must
  be green before a push leaves your clone** — the pre-push hook
  enforces this, alongside a clean `gitleaks detect` scan.
- **A brick's `interface` is its whole public contract.** Reaching into
  another brick's implementation namespace from `src` is not a style
  preference, it's a build error (`poly check`).
- **Test-first, with properties for law-bearing constructs** — inherited
  from `ehr-testing-sim`'s own discipline (`AGENTS.md` "Discipline
  inherited from sim") for anything landed from that repo; applies to
  new workspace-native code the same way.
- **ADRs** (`notes/ADRs.md`): structural or architectural decisions get
  a numbered, append-only record. An Accepted ADR is never silently
  reverted; it's superseded by a new one.

If you skip straight to a PR without reading these, expect review
comments asking you to go back and do so — better to arrive knowing
the shape the discipline expects.

## Domain knowledge — no code required

Once a given project has landed (see `AGENTS.md` for what's landed so
far), its own domain-knowledge intake process — e.g. `ehr-testing-sim`'s
clinical-reality report template — applies at its new home under
`components/`. This section will point at real, in-tree paths once
each project's own docs land; it isn't guessing at paths that don't
exist yet in this workspace.
