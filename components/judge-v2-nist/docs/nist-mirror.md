# The NIST jar mirror

`judge-v2-nist` resolves its six NIST-origin coordinates
(`gov.nist/hl7-v2-parser`, `gov.nist/hl7-v2-profile`,
`gov.nist/hl7-v2-validation`, `gov.nist/xml-util`,
`gov.nist.hit/hl7-v2-schemas`, `com.github.hl7-tools/validation-report`)
over the network, through the `"nist-hit"` entry each `deps.edn` carries
in `:mvn/repos`. **Since 2026-11-17 that entry points at CDC's public
`CDCgov/lib-hl7v2-nist-validator` repository** — its Maven-layout `lib/`
tree, read from `raw.githubusercontent.com` at commit
`eeac90c5f88dca3018992005232acdf3da644d88`. The URL is pinned by commit,
so it cannot change under a build, and the host is not the authority on
content: `artifacts.lock.edn` records each jar's sha256 and
`bin/verify-nist-lock` checks it.

**What was observed on 2026-11-17.** Until that date the entry was
NIST's own Nexus, `hit-nexus.nist.gov/repository/releases/`. That day
the host answered `302` to a `www.nist.gov` web page for every path
probed, Maven received HTML where it asked for a jar, and the per-push lane
failed (CI run 44062772634). NIST's legacy-website page says the Health
IT Program is ending and lists Nexus as "Transition to Prometheus"; it
names no new host. This is the risk this document was written against —
a live network host with no stated availability SLA — arriving.

**What was checked before the switch.** All six jars at the pinned
commit match their `artifacts.lock.edn` sha256, all six poms are
byte-identical to the ones NIST's Nexus had served, and a resolution
into an empty Maven repository produced the same dependency tree as
before. The evidence is in
[`2026-11-17-nist-channel-cdc-pin.md`](../../../.agents/session-records/2026-11-17-nist-channel-cdc-pin.md).
The tree publishes no `.sha1` files, so Maven prints "Checksum
validation failed, no checksums available" for each of the twelve
files and proceeds; the lock's sha256 is the check that binds.

## Why not vendor the jars into this repo?

That would be the obvious fix — CDC's own
`gov.cdc:lib-hl7v2-nist-validator` does exactly this, checking all six
jars into `lib/` and pointing a `file://` repo at them. This repo
cannot follow that pattern: ADR-0005's 2026-07-24 amendment
(`notes/tools/ADRs.md`) holds these six coordinates
`:use-permitted--unstated--confirmation-pending` — their formal license
is unconfirmed, so this repo does not vendor or ship their bytes.
Checking the jars into git would be exactly the redistribution the
amendment forecloses. (`notes/adr/0053-alignment-fixes-4.md` records
this ruling in full, including the earlier design note —
`components/corpus/docs/research/judge-v2-nist-spike-notes.md` item 4 —
that once prescribed the CDC pattern before the amendment's own
implications were fully drawn out.)

**This repo still vendors nothing.** Reading CDC's tree over the network
is not vendoring it: no jar enters this repository, and every fetch
still happens on the user's machine at the user's initiative. What
changed is which host that fetch reaches. The amendment permitted use
only as a user-initiated fetch **from NIST's own official channel**;
the author's ruling of 2026-11-17 supersedes that clause for these six
coordinates and names CDC's public repository an accepted channel. The
ruling is quoted verbatim in
[this session's record](../../../.agents/session-records/2026-11-17-nist-channel-cdc-pin.md);
the amendment's other clause — nothing redistributed by this repo —
stands unchanged.

## The fix: a mirror the USER builds, outside this repo

`bin/mirror-nist` (`make mirror-nist`) builds a Maven-repository-layout
mirror at `~/.ehrt/nist-mirror/`, copied from **the invoking user's own
`~/.m2/repository`** — jars that user already fetched from
`hit-nexus.nist.gov` at their own initiative, the same fetch ADR-0005's
amendment already permits. Nothing is downloaded by this script and
nothing crosses into this repo; the mirror lives entirely on that user's
own machine, outside the git tree.

```bash
make mirror-nist
# or: bin/mirror-nist [--m2 PATH] [--dest PATH]
```

Each jar's sha256 is verified against `artifacts.lock.edn` at copy
time. Three outcomes:

- **exit 0** — every coordinate present in `--m2`, sha256-verified, and
  copied (jar + pom) into `--dest`.
- **exit 1** — a sha256 MISMATCH: the mirror build aborts and names the
  offending coordinate. This is the alarm case — the jar on disk no
  longer matches what the lockfile recorded.
- **exit 2** — a coordinate is not yet in `--m2` — "not yet resolved,
  run a full build first." Not a failure, just nothing to mirror yet.

## Activating the mirror (author-only, not performed by any script)

Once built, the mirror is inert until Maven is told to prefer it. That
is a one-line addition to the *user's own* `~/.m2/settings.xml` — never
written by `bin/mirror-nist`, never by any part of this repo's tooling:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>nist-hit-local-mirror</id>
      <mirrorOf>nist-hit</mirrorOf>
      <url>file://${env.HOME}/.ehrt/nist-mirror</url>
    </mirror>
  </mirrors>
</settings>
```

`mirrorOf` matches the `"nist-hit"` key `deps.edn`'s own `:mvn/repos`
declares; `tools.deps` honors a `<mirror>` entry in `~/.m2/settings.xml`
the same way plain Maven does. Once this is in place, resolution against
the `nist-hit` repository is served from `~/.ehrt/nist-mirror/` instead
of the network — offline determinism, with no change to this repo's own
`deps.edn` coordinates or `:mvn/repos` map.

## The lockfile's own check: `verify-nist-lock`

`bin/verify-nist-lock` (`make verify-nist-lock`) is the mechanized
half of "the lockfile grows teeth": it reads every hit-nexus-sourced
entry from `artifacts.lock.edn`, locates it in a Maven repository
(`--repo`, default `~/.m2/repository`), and compares sha256s. Same
three-exit shape as `mirror-nist` (0 all-match, 1 any-mismatch —
coordinates named, 2 any-absent — "not yet resolved"). It is wired
into `make test` (the per-push lane), right after
`clojure -M:poly test :all skip:integration` — the point where
`projects/conformance`'s own classpath resolution (it depends on
`poly/judge-v2-nist`) has already pulled every one of these coordinates
into `~/.m2`, so a sha256 drift there fails the build.

## If the NIST licensing inquiry resolves favorably

The residual External (`docs/experiments/EXP-SBOM-inquiry-draft.md`,
per `notes/tools/ADRs.md` ADR-0008) asks NIST directly about these six
coordinates' formal license. If that inquiry returns a clear permissive
answer, `license-status` on the six `artifacts.lock.edn` rows can move
from `:use-permitted--unstated--confirmation-pending` to
`:verified` — and, separately, an author could then choose to revisit
whether an in-repo vendored mirror becomes viable after all, the CDC
pattern this document currently forecloses. Nothing about the mirror or
lockfile mechanism described here would need to change either way; a
favorable answer only widens what this repo is *allowed* to do, it
doesn't obsolete what already works.
