# Setup

This is the entry point for **using** what this workspace builds —
installing prerequisites and verifying they work. It is written to be
read by a human, or handed to that human's AI assistant.

If you are here to **contribute to this repo** (open a PR, commit code
or docs), stop and read [`AGENTS.md`](AGENTS.md) instead — it governs
contribution sessions and has its own, stricter rules (WSL-only
commits, the ADR/facts-register discipline, test-first). Nothing on
this page applies to that work, and nothing in `AGENTS.md` applies to
you if you're only running a built artifact.

<!-- MAINTENANCE: the environment table below cites this repo's own
     bootstrap session (2026-07-28) rather than assumed defaults; any
     session that materially changes the toolchain should re-verify
     and update it, citing its own session rather than trusting this
     comment indefinitely. -->

## 1. What you need and why

| Prerequisite | Why | Verify with |
|---|---|---|
| **git** | Clone the repo. | `git --version` |
| **JDK** | Runs Clojure and `poly`. | `java -version` |
| **Clojure CLI** (`clojure`/`clj`) | Resolves `deps.edn` and runs everything: `poly`, tests, each project's own CLI. | `clojure --version` |

**No standalone `poly` install is required** — this workspace uses the
`:poly` alias (`clojure -M:poly ...`), which resolves `polylith/clj-poly`
`0.3.32` from Clojars on first use.

### JDK, precisely

This workspace's own bootstrap session (WSL2/Ubuntu, 2026-07-28) found:

```
$ java -version
openjdk version "21.0.7"
OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
```

That's Ubuntu's own OpenJDK 21 package (`apt install openjdk-21-jdk`),
**not** Eclipse Temurin — a `temurin-17-jdk` package was present on
that machine but unused (JDK 17, not 21), and no Temurin 21 build was
installed. This matches how sim's own `SETUP.md` tells WSL2/Ubuntu
users to install a JDK (the stock apt package); "Temurin" is a
CI-only characterization inherited from sim's CI convention (`distribution:
temurin` in its GitHub Actions workflow), not a claim about anyone's
local dev JVM. Any JDK 21 build should work — nothing here is known to
use post-8 JVM APIs beyond what sim itself already required.

## 2. Platform guidance

- **Linux / WSL2 (Ubuntu).** The primary environment for contribution
  (see `AGENTS.md`, WSL-only git). Install a JDK and git from your
  package manager; install the Clojure CLI via the official installer
  (on Ubuntu, **do not `apt install clojure`** — that installs an
  unrelated, ancient Debian package):

  ```sh
  sudo apt update && sudo apt install -y openjdk-21-jdk git
  curl -O https://download.clojure.org/install/linux-install.sh
  chmod +x linux-install.sh && sudo ./linux-install.sh
  ```

- **Native Windows** — untested for this workspace as of bootstrap.
  Sim's own repo verified Windows-side Clojure byte-identical to WSL
  for its own test suite (its `SETUP.md`, citing its facts-register
  F17/F20); whether that holds once sim's code lives inside a Polylith
  workspace hasn't been checked here. *Contributing* still routes
  through WSL regardless (`AGENTS.md`'s business, not yours).

- **macOS / NixOS** — untested, flagged as such, same as it was for sim.

## 3. Verification ladder

Run in order from a fresh clone:

```sh
git --version
java -version
clojure --version

git clone https://github.com/pragsmike/ehr-testing-tools.git
cd ehr-testing-tools
clojure -M:poly version   # expect: poly 0.3.32 (2025-12-29)
clojure -M:poly check     # expect: OK
```

Per-project verification ladders and first-traffic walkthroughs get
added here as each project lands — see the root
[`README.md`](README.md#quickstart) for the canonical Quickstart
(`bin/ehrt ...`) and `AGENTS.md` for what's landed so far.
`bases/sim-cli` (sim's own standalone CLI) is deprecated in favor of
the `bin/ehrt sim run` mount (R33, ADR-0009) — it still works, but this
page doesn't teach it as a path.

## 4. Troubleshooting

- **`clojure -M:poly check` fails with a missing-alias or file-not-found
  error.** Confirm you're running from the workspace root (the
  directory containing `workspace.edn`), not a subdirectory.
- **Different `poly` version than this page's.** `workspace.edn`
  doesn't pin a version by itself; `deps.edn`'s `:poly` alias does
  (`polylith/clj-poly {:mvn/version "0.3.32"}`). If your output
  differs, check that alias against what's actually in your clone.
- **`bin/ehrt corpus generate` fails with `:category :out-dir-exists`.**
  Zero-flag `generate` is byte-reproducible and therefore rejects
  overwriting a prior run rather than silently clobbering it — remove
  `target/corpus/synthea-s1-p5` first, or pass a different `--out-dir`,
  to regenerate.
- **A `WARNING: run! already refers to...` line appears on stdout.**
  Harmless namespace-shadowing warning from `ehrt.tools.sim`; it
  doesn't affect the exit code and can be ignored.
- **`bin/ehrt gate ... --report <file>` still prints the full result to
  stdout.** `--report` writes the file but doesn't suppress the console
  dump; redirect stdout (`> /dev/null`) if you only want the exit code,
  or read the report file directly.
- **`clojure -M:poly test :all` takes several minutes.** It runs every
  brick's test suite, including property-based tests with 100-200
  random cases each; there's no faster documented subset for a
  first-time full run.

## 5. The agent prompt

If you'd rather have an AI assistant do this for you, give it this
prompt:

```
I have a fresh clone of the ehr-testing-tools repository. I am on
[Windows 11 / Ubuntu / NixOS / macOS]. Read SETUP.md and README.md in
the repo root. Help me: (1) install the prerequisites for my platform,
(2) run the verification ladder until `clojure -M:poly check` passes,
(3) work through the root README's Quickstart to generate, mutate, and
gate my first synthetic corpus, and (4) explain what the output files
are. If anything fails, diagnose from the repo's docs before searching
the web.
```
