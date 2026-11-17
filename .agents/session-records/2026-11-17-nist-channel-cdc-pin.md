# NIST channel: the six coordinates resolve from CDC's public repo, pinned by commit

2026-11-17. Base `6e38bc25`. Ceremony: R30, taken from the session
prompt (`.agents/prompts/2026-11-17-nist-channel-cdc-pin.md`), which
stated no prepare-only mode. NOT docs-only: four `deps.edn` files
changed, so a local `make test` was owed and run.

`bin/preflight` ran first and exited **1** with one finding, disclosed:
the last two CI runs on main are RED (`6e38bc25`, `68d972d2`). That is
the failure this session exists to fix. Everything else OK (HEAD
matched origin/main, tree clean; HEAD not tagged stable-*, disclosed).

**Author ruling, verbatim, 2026-11-17: "A"** -- chosen from the
channel's options, where A read: "Point `:mvn/repos` at CDC's repo,
pinned to that commit. ... It is public, hash-identical, and this repo
still ships no jars. It needs an amendment naming CDC's repo as an
accepted channel." For these six coordinates it supersedes the clause
of ADR-0005's 2026-07-24 amendment that they are fetched only from
NIST's own channel. The amendment's other clause -- this repo
redistributes none of them -- is untouched: no jar entered the repo.

The pinned URL, the same string at all four sites:
`https://raw.githubusercontent.com/CDCgov/lib-hl7v2-nist-validator/eeac90c5f88dca3018992005232acdf3da644d88/lib/`

## Step 1: baseline, before any edit

- `bin/verify-nist-lock` against `~/.m2/repository`: exit 0, six
  matched.
- `make mirror-nist`: exit 0, "6 coordinate(s) verified and copied" to
  `~/.ehrt/nist-mirror`. Not activated; this host's
  `~/.m2/settings.xml` has no mirror entry.
- `clojure -A:test -Stree` in `projects/conformance`: exit 0, 170
  lines, saved.
- sha256 of the six poms in `~/.m2`:

| pom | sha256 |
| --- | --- |
| `gov/nist/hl7-v2-parser/1.7.3` | `3c5dc345381377cc58064fda37505af24dc8633938b8e5c62e37696a478a0da4` |
| `gov/nist/hl7-v2-profile/1.7.3` | `5b2cdf9fc9133098c8840ba0cd8e698db283566c7bd15615600b8c62076ae2c0` |
| `gov/nist/hl7-v2-validation/1.7.3` | `506377901dfcf61b7c436dfb8db689c3aa00548edfd1f792c16d6c34e1102568` |
| `gov/nist/xml-util/2.1.0` | `7e6e1bff26b4aec7273be9a947cc2fab8a8adaf31513cc02304e1636de940c33` |
| `gov/nist/hit/hl7-v2-schemas/1.7.2` | `a432e5d7fe0ee1ebbef79a9ce6a90cb64781c9a370e406147a971ad17829749a` |
| `com/github/hl7-tools/validation-report/1.2.0` | `c2123a1dbc6031e9589d24c657cd62b870dfbe889bed4004a5d3be454591d446` |

## Step 2: the source, independently of Maven

`git ls-remote` gave CDC's HEAD as
`eeac90c5f88dca3018992005232acdf3da644d88`. Twelve `curl` fetches from
the pinned URL, all HTTP 200. Jars: sha256 against the lock row. Poms:
`cmp` against step 1's `~/.m2` file.

| coordinate | jar sha256 (fetched) | vs lock | pom vs `~/.m2` |
| --- | --- | --- | --- |
| `gov.nist/hl7-v2-parser` 1.7.3 | `62fac887d19842b26ddde2b9916db39dd7a2cdcdafeeeb1cac6a0e52c0d17999` | match | identical |
| `gov.nist/hl7-v2-profile` 1.7.3 | `354d7a3e0a39b5eae705d9f2d56cc9c25721bbd2ae80d71ea6bd0df92f4846e7` | match | identical |
| `gov.nist/hl7-v2-validation` 1.7.3 | `3e5b6a9b95066c4abeae1435de0a06e08c43fa8e786bb5c1a609d8172925de50` | match | identical |
| `gov.nist/xml-util` 2.1.0 | `ba41c996d50e2a14c617f780a301cc69f5c683ddc6830e1b52f065e50bef224c` | match | identical |
| `gov.nist.hit/hl7-v2-schemas` 1.7.2 | `22fd209727f7d1e4fa17b773bf62f9b27b076c95b082e98faa7592ab454c28fb` | match | identical |
| `com.github.hl7-tools/validation-report` 1.2.0 | `e21fc63edcb8efd5c1db137b224dfb187d2fed63b924564ff34693eedd46cc7f` | match | identical |

Six of six jars match the lock; six of six poms are byte-identical. No
HALT.

## Step 3: the edit

Four `:url` values changed, all to the pinned URL (root,
`projects/conformance`, `projects/integration`, `projects/ehrt-cli`);
the key stays `"nist-hit"`. Comments restated at those four sites, at
`components/judge-v2-nist/deps.edn`, and at
`.github/workflows/integration.yml`. `artifacts.lock.edn`: one dated
comment above the six rows, every row byte-identical. The diff carries
no `:deps`, `:mvn/version`, `:sha256` or `:source` value change;
`bin/verify-nist-lock` still enumerates six.

## Step 4: cold proof

`clojure -Sforce -Sdeps '{:mvn/local-repo "<scratch>"}' -A:test -Stree`
in `projects/conformance`, the scratch directory empty: exit 0, 252
downloads, twelve of them "from nist-hit" (six poms, six jars).

| gate | result |
| --- | --- |
| `bin/verify-nist-lock --repo <scratch>` | exit 0, six matched |
| resolved tree vs step 1's | byte-identical (170 lines; empty set difference) |
| `make test` | exit 0; 0 failures, 0 errors; poly 28 min 44 s; its own `verify-nist-lock` six matched |

Maven printed "Download corrupted: Checksum validation failed, no
checksums available" twelve times, once per file from the new URL, and
proceeded: the tree publishes no `.sha1` (a probe for one returned
404). The lock's sha256 is the check that binds. The same twelve lines
will appear in every cold CI run.

`make ci-parity` does NOT cover this: it colds the artifact cache
(`EHR_TESTING_TOOLS_CACHE`), not `~/.m2`.

## Step 5: the doc

`components/judge-v2-nist/docs/nist-mirror.md`: the opening and "Why
not vendor" restated (what was observed, the new channel, nothing
vendored, the ruling and its scope, this record). No ADR written, and
no rule in the tree was found that requires one to supersede an ADR
clause (`notes/adr/README.md` 19-22 says where an amendment lands if
one is written; it does not demand one). `notes/tools/` not edited.

## Where the prompt's facts differed

1. **"Five declare `<repositories>`" is three.** The parser, profile
   and validation poms declare `<repositories>` (hit-nexus releases).
   The schemas and validation-report poms declare
   `<distributionManagement>` instead, which does not affect
   resolution; `xml-util` declares neither. Five poms name hit-nexus,
   by two different elements. No pom declares a `<parent>`, as stated.
   The cold resolve reached nothing at hit-nexus.
2. **The Integration lane may not see this fix or its absence.**
   `integration.yml` restores `~/.m2/repository` from `actions/cache`,
   keyed on `hashFiles('deps.edn')`. So the green nightly run
   43909539260 of 2026-11-16 is NOT evidence the Nexus was up then,
   and the dispense-disclosure record's sentence "so the repository
   changed after it" does not follow. Erratum to that record, stated
   here; root `deps.edn` changes in this commit, so the next nightly
   misses the exact key and falls back to `restore-keys`.
3. Every other channel fact held: CDC HEAD, twelve 200s, six hashes,
   no `.sha1`, and NIST's page (re-fetched: "Program is ending", Nexus
   listed "Transition to Prometheus", no new host named).

Not done, for the author:

- `notes/adr/0053-alignment-fixes-4.md` still records the user-side
  mirror ruling in terms of NIST's channel, unamended.
- `nist-mirror.md`'s "The fix" section, both scripts' headers and
  messages, and Makefile 324-334 still say "hit-nexus-sourced" and
  "NIST's own official channel". True of where the rows were acquired;
  stale as a description of the live fetch. Outside the prompt's edit
  list.

## Close

Pushed `6e38bc25..e5cd16f8` as one commit. Before it: `make test` exit
0 (0 failures, 0 errors, log kept with its exit code); gitleaks clean;
`git diff --cached` showed exactly four `:url` values changed.
`bin/post-push-verify 6e38bc25 e5cd16f8`: remote tip matches; the
commit message is ASCII; CI not yet indexed at that moment (disclosed
by the script). **CI run 44108231313 completed `success`** at
`e5cd16f8c81d261cdaec31d0d1e7d4ba65ee7683`, attempt 1 -- the close
marker. Its `poly test :all skip:integration`, `verify-nist-lock` (six
matched, on a runner with no Maven cache) and generated-doc freshness
steps all ran and passed; its log carries the twelve "no checksums
available" lines step 4 predicted. No tag paid. Background processes
this session started: one `make test` and one `gh run watch`, both
exited; none left running.
