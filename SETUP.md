# Setup

This is the entry point for **using** `ehr-testing-sim` — installing
prerequisites, verifying they work, and generating your first synthetic
hospital traffic. It is written to be read by a human, or handed to
that human's AI assistant (see the copy-paste prompt in step 5).

If you are here to **contribute to this repo** (open a PR, commit code
or docs), stop and read [`AGENTS.md`](AGENTS.md) instead — it governs
contribution sessions and has its own, stricter rules (WSL-only
commits, the facts register, test-first). Nothing on this page applies
to that work, and nothing in `AGENTS.md` applies to you if you're only
running the simulator.

<!-- MAINTENANCE: verification claims below cite notes/facts-register.md
     rows (F17, F19) and the CI workflow rather than "this session";
     any session that materially edits this file should re-run the
     verification ladder and update the cited counts. -->

## 1. What you need and why

This repo is deliberately light: three prerequisites, no artifact
downloads, no subprocesses, no server. The full dependency set is a
few small libraries from Clojars and Maven Central (public,
unauthenticated — enumerated in `notes/facts-register.md` F19), fetched
automatically the first time you run anything.

| Prerequisite | Why | Version | Verify with |
|---|---|---|---|
| **git** | Clone the repo. | Any recent 2.x | `git --version` |
| **JDK** | Runs Clojure. Verified on **JDK 8** (`1.8.0_311` — the maintainer's own dev JVM, facts-register F13/F17) and **JDK 21** (Temurin, this repo's CI). Anything between should work; nothing here uses post-8 JVM APIs. | 8+ (CI pins Temurin 21) | `java -version` |
| **Clojure CLI** (`clojure`/`clj`) | Resolves `deps.edn` and runs everything: tests, the `sim` CLI. | Any recent release (CI uses latest) | `clojure --version` |

**Optional:** GNU `make` wraps two conveniences (`make test`,
`make run`) and the maintainer-only pack targets — but unlike the
sibling `ehr-testing-tools` repo, nothing here *requires* make or even
bash: every documented command is a plain `clojure` invocation, which
is also why this repo runs on native Windows (see below).

## 2. Platform guidance

- **Linux / WSL2 (Ubuntu).** The primary environment. Install a JDK
  and git from your package manager; install the Clojure CLI via the
  official installer (on Ubuntu, **do not `apt install clojure`** —
  that's an unrelated, ancient Debian package; the sibling repo's
  SETUP.md documents the same trap):

  ```sh
  sudo apt update && sudo apt install -y openjdk-21-jdk git
  curl -O https://download.clojure.org/install/linux-install.sh
  chmod +x linux-install.sh && sudo ./linux-install.sh
  ```

- **Native Windows works — for running.** Unlike the tools sibling
  (make/bash-driven, WSL-required), this repo's commands are pure
  Clojure CLI, and the full suite has been verified passing on
  Windows-side Clojure with results byte-identical to WSL
  (facts-register F17: 403/1058 on both, empty diff of the test-var
  lists). Install the Clojure CLI for Windows per clojure.org's
  instructions and use the same commands as everywhere else.
  *Contributing* still routes through WSL (that's `AGENTS.md`'s
  business, not yours).

- **macOS / NixOS** — untested, flagged as such. Same three
  prerequisites; `brew install clojure/tools/clojure` /
  `nix-shell -p clojure jdk21 git` are the conventional routes, but
  this repo's ladder has not been run on either.

## 3. Verification ladder

Run in order from a fresh clone:

```sh
git --version
java -version
clojure --version

git clone https://github.com/pragsmike/ehr-testing-sim.git
cd ehr-testing-sim
clojure -X:test
```

Expected: **403 tests / 1058 assertions, 0 failures, 0 errors**, about
35 seconds after the one-time dependency fetch (verified end-to-end
from a fresh clone: facts-register F19; the count moves as capability
code lands — higher with 0 failures is fine, lower or failing means
your environment differs from what's verified). The suite is hermetic:
no network beyond that first dependency fetch, no server, nothing
written outside the repo and your Maven cache.

## 4. First traffic walkthrough

Everything is one CLI. Same config + same seed ⇒ byte-identical
output, every time, on every machine — run any command below twice to
see for yourself.

```sh
# A five-patient run with churn, rendered as HL7v2 into the payload.
# Output is one EDN result map: ground truth, manifest, summary, and
# :messages (a vector of ER7 strings).
clojure -M:cli run --seed 42 --patients 5 --churn --emit hl7

# The same, as JSON:
clojure -M:cli run --seed 42 --patients 5 --churn --emit hl7 --json

# Just the messages, human-readable (ER7's segment separator is \r,
# so raw messages look like one long line in a terminal — that's the
# wire format being correct, not broken; this unrolls it for eyes):
clojure -M:cli run --seed 42 --patients 5 --churn --emit hl7 --json \
  | jq -r '.payload.messages[] | gsub("\r"; "\n") + "\n"'

# Self-check: pipe a run's ground truth through the invariant catalog.
# Exit 0 = every consistency law holds.
clojure -M:cli run --seed 42 --patients 5 | clojure -M:cli check

# FHIR instead: one Bundle per patient, end-of-run snapshots, every
# resource carrying the standard HTEST "test data" security label.
clojure -M:cli run --seed 42 --patients 2 --emit fhir

# The complete identifier inventory a run will ever produce — MRNs,
# control ids, FHIR resource ids, NPIs — without generating the
# corpus. (Why this exists: docs/simulate-your-facility.md's "how
# would we find and remove it" answer.)
clojure -M:cli identifiers --seed 42 --patients 5

# Everything at once — clinical modules, orders, churn, your own
# facility and dialect — via a config file:
clojure -M:cli run --seed 7 --config my-site.edn --churn --emit hl7
```

For that last one: exact, runnable configs live in
[`docs/demos/`](docs/demos/) (start from one and edit), and
[`docs/simulate-your-facility.md`](docs/simulate-your-facility.md)
walks a facility analyst through building `my-site.edn` from a
one-page site interview. Every run also self-checks its own invariant
catalog and emits a provenance manifest — the manifest plus this
repo's version *is* the corpus, regenerable on demand.

**Outputs are plain text** — EDN/JSON on stdout, ER7 strings, FHIR
JSON — consumable from any language; nothing about using the output
requires Clojure.

## 5. The agent prompt

If you'd rather have an AI assistant do this, give it this prompt:

```
I have a fresh clone of the ehr-testing-sim repository. I am on
[Windows 11 / WSL2 Ubuntu / macOS / NixOS]. Read SETUP.md and
README.md in the repo root. Help me: (1) install the three
prerequisites for my platform, (2) run the verification ladder until
`clojure -X:test` passes, (3) run the first-traffic walkthrough and
show me one HL7 message and one FHIR resource from it, and (4) explain
what the ground-truth log, manifest, and HTEST label are, using
docs/GLOSSARY.md. If anything fails, diagnose from the repo's docs
before searching the web.
```

## 6. Troubleshooting seeds

- **`--seed is required`.** Deliberate — determinism is a feature,
  not a default; there is no wall-clock fallback. Pick a seed.
- **Messages print as one long line.** ER7's segment separator is
  `\r`; your terminal is showing you correct wire bytes. Use the
  `jq`/`gsub` recipe above for reading, redirect to a file for
  machines.
- **Config rejected with `:incompatible-assignment`.** One patient
  was given both an encounter-opening authored pathway and a disease
  module — that combination is illegal by design (one encounter per
  run horizon); assign pathway patients and module patients
  disjointly. See docs/simulate-your-facility.md.
- **Config rejected with a schema error.** Field names drift is the
  usual cause — diff your file against the nearest example in
  `docs/demos/`, which are validated against the real schemas.
- **Clojure CLI not on `PATH` after install.** The official installer
  puts `clojure`/`clj` in `/usr/local/bin` — open a new shell (or
  `hash -r`).
- **Different test count than step 3's.** Higher + 0 failures: fine
  (code landed since this page's last verification — check the
  README badge). Lower, or any failure: your environment differs;
  start with `java -version` against the table above.
