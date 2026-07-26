# Setup

This is the entry point for **using** `ehr-testing-tools` — installing
prerequisites, verifying they work, and generating your first synthetic
corpus. It is written to be read by a human, or handed to that human's AI
assistant (see the copy-paste prompt in step 5).

If you are here to **contribute to this repo** (open a PR, commit code or
docs), stop and read [`AGENTS.md`](AGENTS.md) instead — it governs
contribution sessions and has its own, stricter rules (WSL-only commits,
the facts register, test-first). Nothing on this page applies to that
work, and nothing in `AGENTS.md` applies to you if you're only running the
tools.

## 1. What you need and why

| Prerequisite | Why | Minimum version | Verify with |
|---|---|---|---|
| **git** | Clone the repo. | Any recent 2.x | `git --version` |
| **JDK** | Runs the Clojure orchestrator (`make test`, the `ehr` CLI itself). Verified this session on OpenJDK 11.0.27 — `make test` and a full `corpus generate` + `corpus mutate` run both passed on it. | 11+ (17+ recommended — this repo's own dependencies, HAPI FHIR/HAPI HL7v2, are compiled for 17, though nothing in the current codebase loads those classes yet) | `java -version` |
| **Clojure CLI** (`clojure`/`clj`) | Resolves `deps.edn` and runs everything above. | This repo is developed and verified against Clojure CLI **1.12.0.1479**. (`deps.edn` separately pins the Clojure *language* to `1.12.5` — that's resolved automatically the first time you run `clojure`/`make`, not something you install yourself.) | `clojure --version` |
| **GNU make** | Every documented command in this repo is a `make` target. | Any recent version (verified on GNU Make 4.2.1) | `make --version` |
| **bash** | The `Makefile`'s `SHELL` and its recipes are bash, not POSIX `sh` or PowerShell. | Any recent version (verified on 5.0.17) | `bash --version` |
| **curl** | Used by the artifact registry to fetch Synthea and its JDK on first use. | Any recent version (verified on 7.68.0) | `curl --version` |

**Note on the JDK:** Synthea itself requires a JDK 17+ *runtime* (its
release jar is class file version 61.0), but you never install that
yourself — `ehr artifact fetch --name temurin-jdk --version 17.0.19+10`
downloads a pinned Eclipse Temurin 17 build into this repo's own artifact
cache (`~/.cache/ehr-testing-tools/artifacts`, per `artifacts.lock.edn`)
and Synthea always runs under that copy, never under whatever `java` is on
your `PATH`. Don't install a second system JDK 17 on its account.

### Maintainer-only tools

Not needed to use the tools — only if you're contributing to this repo
(see `AGENTS.md`, `AUTHORS-GUIDE.md`):

| Tool | Used for |
|---|---|
| `jq` | Ad hoc EDN/JSON inspection during sessions. |
| `gh` | GitHub CLI — issues, PRs, gists. |
| WSL + `.githooks/pre-commit` | Enforces the WSL-only commit rule (`git config core.hooksPath .githooks`). |

## 2. Platform guidance

- **Linux.** This repo is developed and tested on **Ubuntu** (20.04 LTS
  this session). One-liner for the prerequisites table above (JDK 17,
  `git`, `make`, `curl` — the Clojure CLI needs a separate step, below):

  ```sh
  sudo apt update && sudo apt install -y openjdk-17-jdk git make curl
  ```

  **Do not `apt install clojure`** — on Ubuntu that installs an unrelated,
  much older Debian package (verified this session: it provides a
  `clojure1.10` binary, not `clojure`/`clj`, and does not give you the CLI
  tools this repo needs). Install the Clojure CLI via the official
  installer instead:

  ```sh
  curl -O https://download.clojure.org/install/linux-install.sh
  chmod +x linux-install.sh
  sudo ./linux-install.sh
  ```

- **Windows 11 → WSL2 with Ubuntu is the supported path.** Native Windows
  is **not supported** — the build is `make`/bash-driven, and neither
  exists there. Enable WSL2 (`wsl --install`, then a reboot) per
  Microsoft's own guide:
  <https://learn.microsoft.com/en-us/windows/wsl/install>. Once inside a
  WSL2 Ubuntu shell, follow the Linux instructions above. (Verified this
  session: the whole quickstart below runs correctly against a
  Windows-mounted repo path, `/mnt/c/...`, from inside WSL2 — you don't
  need to relocate the clone into the Linux filesystem to use the tools;
  `AGENTS.md`'s WSL-only rule is about `git commit`, not about running
  the CLI.)

- **NixOS** — untested this session, flagged as such. A one-line
  suggestion for the packages you'd need:

  ```sh
  nix-shell -p clojure jdk17 gnumake bash curl git
  ```

- **macOS** — untested this session, flagged as such. Same prerequisites
  as Linux (a JDK 17, the Clojure CLI, `make`, `bash`, `curl`, `git`);
  Homebrew (`brew install clojure/tools/clojure openjdk@17`) is the
  conventional way to get them, but this repo's own quickstart has not
  been run on macOS.

## 3. Verification ladder

Run these in order from a fresh clone. Every command below was executed
this session (`ehr-testing-tools`, WSL2/Ubuntu 20.04) unless marked
otherwise.

```sh
git --version
java -version
clojure --version
make --version
bash --version
curl --version

git clone https://github.com/pragsmike/ehr-testing-tools.git
cd ehr-testing-tools
make test
```

Expected result: **166 tests, 0 failures, 0 errors.** (This number moves
as capability code lands — if it's higher and still 0 failures, that's
fine; if it's lower or shows failures, something in your environment
differs from what's verified here.) The suite is hermetic — no network
access beyond the one-time dependency fetch `clojure` does automatically
on first run (see `AGENTS.md`'s hermetic-test-suite rule).

## 4. First corpus walkthrough

These are the exact commands verified this session, run in order from the
repo root.

```sh
# One-time: fetch the pinned Synthea distribution and its JDK into the
# local artifact cache. First run downloads roughly 190MB (Synthea jar)
# + 185MB (the Temurin 17 JDK tarball) ~375MB total; cached afterward —
# a second run returns instantly (":cached true" in the output).
bin/ehr artifact fetch --name synthea --version 4.0.0
bin/ehr artifact fetch --name temurin-jdk --version 17.0.19+10

# Generate a small deterministic corpus (10 patients, pinned seeds and
# reference date -- same invocation the README's quickstart uses).
bin/ehr corpus generate --config-path config/synthea/synthea.properties \
  --seed 100 --clinician-seed 555 --population 10 \
  --reference-date 20260101 --output-dir out/my-first-corpus
```

This lands in `out/my-first-corpus/` (gitignored — generated corpora are
never committed; the manifest and hashes are the record, not the bytes):

- `fhir/*.json` — one FHIR R4 Bundle per patient (plus two non-patient
  bundles, `hospitalInformation*.json` and `practitionerInformation*.json`).
- `manifest.edn` — the reproducibility manifest: the exact Synthea/JDK
  invocation, both seeds, the config file's hash, environment values
  forced for determinism (locale, timezone, JVM version), and the
  subprocess's exit code and stdout/stderr digests. This is what makes the
  run reproducible and auditable — not just the JSON output.

Now mutate one patient bundle — drop a required element at a named
locator, with a lineage record for the mutant:

```sh
PATIENT_FILE=$(ls out/my-first-corpus/fhir/*.json | grep -v -e hospitalInformation -e practitionerInformation | head -1)
bin/ehr corpus mutate --input $PATIENT_FILE \
  --operator-id remove-required-element --locator-path entry[0].resource.gender \
  --output-dir out/my-first-mutants
```

This lands in `out/my-first-mutants/`: the mutated bundle itself, plus
`lineage/<file>.lineage.edn` — an immutable, hash-linked record naming the
parent bundle's hash, the operator applied (`remove-required-element`,
with its contract: it *violates* the base FHIR spec's minimum-cardinality
constraint on the element it targets), and the resulting bundle's hash.

**Outputs are plain FHIR JSON and EDN manifests** — consumable from
Python, or any language that can parse JSON and a Lisp-flavored key/value
format, with no Clojure knowledge required to use the results. (EDN is
close enough to JSON's data model that a small parser, or even careful
regex/line-based reading for the fixed shapes above, is enough to consume
it from Python; nothing about consuming the output requires running or
understanding Clojure.)

## 5. The agent prompt

If you'd rather have an AI assistant do this, give it this prompt:

```
I have a fresh clone of the ehr-testing-tools repository. I am on
[Windows 11 / Ubuntu / NixOS / macOS]. Read SETUP.md and README.md in the
repo root. Help me: (1) install the prerequisites for my platform,
(2) run the verification ladder until `make test` passes, (3) generate my
first small synthetic corpus per the walkthrough, and (4) explain what
the output files are. If anything fails, diagnose from the repo's docs
before searching the web.
```

## 6. Troubleshooting seeds

- **Wrong or old `java` on `PATH`.** `java -version` should show 11+
  (17+ if you have it). If Synthea generation fails with
  `UnsupportedClassVersionError`, that's the fetched Temurin 17 not being
  used correctly — check `bin/ehr artifact fetch --name
  temurin-jdk --version 17.0.19+10` actually completed, not your system
  `java`.
- **Clojure CLI not on `PATH` after install.** The official installer
  puts `clojure`/`clj` in `/usr/local/bin` — open a new shell (or
  `hash -r`) if the command isn't found right after installing.
- **Running on native Windows.** Symptom: `make` (or `bash`) not found,
  or PowerShell errors on `Makefile` syntax. This repo's build doesn't
  run on native Windows at all — go to the WSL2 section above.
- **First `corpus generate` is slower than expected.** The first artifact
  fetch downloads roughly 375MB total (Synthea jar + Temurin JDK tarball,
  verified this session), plus extraction. Every run after that is fast —
  the cache is content-addressed and reused.
