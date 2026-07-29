# Consolidating Tangled Clojure Repos into a Polylith Monorepo

A working brief for the developer (or agent) tasked with merging several related Clojure
repositories — which have drifted into duplicated code and cross-repo dependency knots — into a
single Polylith workspace.

Tool version referenced throughout: `poly` **0.3.32** (released 2025-12-29), Clojars coordinate
`polylith/clj-poly` ([Clojars](https://clojars.org/polylith/clj-poly),
[releases](https://github.com/polyfy/polylith/releases)).

---

## 1. Executive summary

You have N repos. Some code has been copy-pasted between them, some has been frozen into internal
libraries, and the dependency graph between repos is now a source of friction. The two conventional
escapes — "extract more libraries" and "extract more services" — both make the development loop
worse. Polylith takes the fourth option: **one repository, one development REPL, many deployable
artifacts.**

The mental model:

- **Component** — a chunk of your domain behind a single `interface` namespace. Reusable, swappable.
- **Base** — the thing that faces the outside world (a `-main`, an HTTP handler, a lambda entry point). No interface.
- **Component + Base = "brick".** Bricks are plain source directories with their own `deps.edn` declaring only third-party libraries — never other bricks.
- **Project** — a `deps.edn` that lists which bricks go into one deployable artifact, via `:local/root`.
- **Development project** — the root `deps.edn`, which pulls in *everything* so one REPL sees the whole codebase.

Because bricks never name each other and projects wire them together, the *same* component can be
composed into any number of deployables, and a change to a shared component is visible instantly in
every project from a single REPL, with no library publish step. This is precisely the duplication and
tangle problem you have ([Sharing code](https://polylith.gitbook.io/polylith/introduction/sharing-code)).

The migration is incremental and low-risk: each existing repo becomes one Polylith *project* first,
behaving exactly as it did before; deduplication happens afterwards, one component at a time
([Transitioning to Polylith](https://polylith.gitbook.io/polylith/conclusion/should-you-convert-your-system)).

This is not theoretical. World Singles Networks migrated a 136K-line Clojure codebase over roughly 20
months and now build 21 projects from 21 bases and 144 components
([Corfield, part XI](https://corfield.org/blog/2023/07/15/deps-edn-monorepo-11/)).

---

## 2. Why Polylith fits *this specific* problem

Polylith's own framing is a diagnostic. Ask, of your current repos, whether these are true
([Sharing code](https://polylith.gitbook.io/polylith/introduction/sharing-code)):

- I can easily split a service into two.
- I can easily share code between services without creating libraries.
- I have no unwanted code duplication anywhere in the system.
- I can easily change, find, refactor, debug, and reason about all my code, even across services.

If the answers are no, the four available remedies are: duplicate the code, publish a library,
extract a service, or use a monorepo. The docs walk through why the first three fail:

| Remedy | Failure mode |
| --- | --- |
| Duplicate | You must copy not just the shared namespace but its whole transitive closure. Divergence follows. |
| Library | The shared code's transitive closure gets bundled too, violating single responsibility. Worse, every change now requires a build/publish/restart cycle, and versions drift across consumers. |
| New service | You pay network calls and an extra deployable for what was a function call — and the moment the new service grows a *second* responsibility, you are back to square one. |
| Monorepo | Works, and Polylith adds composability on top of it. |

Two Polylith properties matter most for your situation:

**Defrosting libraries.** Any internal library you maintain purely to share code between your own
repos should become a component. The docs put it plainly: "Libraries are created by freezing code in
time, which leads to friction in the development experience. By defrosting them into components, we
get living code that is easy to change and which is always in sync with the rest of our codebase"
([Transitioning](https://polylith.gitbook.io/polylith/conclusion/should-you-convert-your-system)).

**Incremental testing.** The tool tracks which bricks changed since the last stable git tag and runs
only the affected tests, in every project that transitively includes them
([Testing incrementally](https://polylith.gitbook.io/polylith/introduction/testing-incrementally)).
A monorepo without this becomes a CI tax; with it, merging repos actually *speeds up* CI. Corfield
reported build times "cut dramatically" mid-migration
([part IX](https://corfield.org/blog/2022/11/05/deps-edn-monorepo-9/)).

**What Polylith is not:** not a framework, not a library, not a runtime. It is an architecture plus
a static-analysis/build CLI. It does not do dependency injection, hot code upgrade, or state
management — you keep using Integrant/Component/mount for that
([FAQ](https://polylith.gitbook.io/polylith/conclusion/faq)).

---

## 3. The building blocks, precisely

### Workspace
The repository root. Contains `bases/`, `components/`, `projects/`, `development/`, `deps.edn`,
`workspace.edn` ([Workspace](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/workspace)).

### Component
A directory under `components/` with `src/`, `test/`, optional `resources/`, and its own `deps.edn`.
Its public contract is the namespace `<top-ns>.<interface-name>.interface`. Everything else in the
component is implementation and is invisible to other bricks' `src` code
([Component](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/component)).

### Interface
Just a namespace called `interface` (or `ifc`, or whatever `:interface-ns` says) containing `def`,
`defn`, and `defmacro` forms. The conventional shape is thin delegation:

```clojure
(ns com.acme.user.interface
  (:require [com.acme.user.core :as core]))

(defn hello [name]
  (core/hello name))
```

You may split it into `interface/foo.clj`, `interface/spec.clj` sub-namespaces, but the docs advise
that the urge to do so is usually a signal to split the *component* instead
([Interface](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/interface)).

Two or more components may implement the *same* interface (e.g. a real `user` and a `user-remote`
that RPCs to a service). The tool cross-checks arity, argument order, type hints, and fn-vs-macro
between them, so they cannot silently drift apart
([Interface](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/interface)).

### Base
Same shape as a component, minus the interface, plus a public API to the outside world. One per
deployable, usually. A base delegates to components through their interfaces and never declares a
dependency on a component in its own `deps.edn`
([Base](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/base)).

### Project
`projects/<name>/deps.edn` — a `:deps` map of `:local/root` entries pointing at bricks, plus
third-party libs, plus build aliases. No `src/` directory; production code lives only in bricks
([Project](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/project)).

### Development project
The root `deps.edn`. Its `:dev` alias lists *all* bricks; its `:test` alias lists all brick test
paths. `development/src/` holds scratch/REPL code under a `dev.*` top namespace, deliberately
separate from production namespaces
([Development](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/development)).

### Rules the tool enforces
- From `src`, a brick may require **only** `interface` namespaces of other components (plus libraries and its own namespaces).
- Components may not depend on bases. Bases may depend on bases.
- From `test`, anything goes — you can reach implementation namespaces directly.
- Violations surface as errors from `poly check`, `poly info`, and `poly test`
  ([Validations](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/validations)).

---

## 4. Target monorepo layout

Assume three legacy repos — `billing`, `reporting`, `ingest` — and an internal `acme-common`
library. The end state:

```
acme/                                # workspace root == git repo root
├── .git/
├── .vscode/settings.json            # Calva REPL config (generated)
├── workspace.edn                    # Polylith config
├── deps.edn                         # THE development project
├── development/
│   └── src/
│       └── dev/
│           ├── mike.clj             # per-developer scratch ns
│           └── server.clj
├── components/                      # ~80% of your code ends up here
│   ├── config/
│   │   ├── deps.edn
│   │   ├── resources/config/
│   │   ├── src/com/acme/config/
│   │   │   ├── interface.clj
│   │   │   └── core.clj
│   │   └── test/com/acme/config/
│   │       └── interface_test.clj
│   ├── log/
│   ├── jdbc/                        # was acme-common.db
│   ├── invoice/
│   ├── invoice-remote/              # same `invoice` interface, RPC impl
│   ├── ledger/
│   ├── report-builder/
│   ├── s3/
│   └── kafka-consumer/
├── bases/                           # thin: entry points only
│   ├── billing-rest-api/
│   ├── reporting-lambda/
│   ├── ingest-worker/
│   └── admin-cli/
├── projects/                        # one dir per deployable artifact
│   ├── billing/
│   │   ├── deps.edn
│   │   └── test/                    # optional project-level tests
│   ├── reporting/
│   │   └── deps.edn
│   └── ingest/
│       └── deps.edn
├── build.clj                        # tools.build, shared by all projects
└── scripts/
    └── build-uberjar.sh
```

Notes on the shape:

- Directory names under `components/` and `bases/` are brick names; they must be unique across
  *both* directories.
- Every brick namespace is `<top-ns>.<brick-or-interface-name>.*`. With `:top-namespace "com.acme"`
  and component `invoice`, the interface is `com.acme.invoice.interface` living at
  `components/invoice/src/com/acme/invoice/interface.clj`.
- `resources/` inside a brick gets a subdirectory named after the brick/interface
  (`components/config/resources/config/`) so classpath entries can't collide when several bricks
  land in one uberjar ([Source code](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/source-code)).
- `.cljc` is read automatically; add `"cljs"` to `:dialects` if you have ClojureScript
  (supported since poly 0.3.0).

### `workspace.edn`

```clojure
{:top-namespace "com.acme"
 :interface-ns "interface"          ; use "ifc" if you share .cljc with ClojureScript
 :default-profile-name "default"
 :dialects ["clj"]
 :compact-views #{"libs"}           ; nicer output once you have many bricks
 :vcs {:name "git"
       :auto-add true}              ; git-add files generated by `poly create`
 :tag-patterns {:stable "^stable-.*"
                :release "^v[0-9].*"}
 :template-data {:clojure-ver "1.12.2"}
 :validations {:inconsistent-lib-versions {:type :error   ; see §7
                                           :exclude []}}
 :projects {"development" {:alias "dev"}
            "billing"     {:alias "bill"}
            "reporting"   {:alias "rep"}
            "ingest"      {:alias "ing"}}}
```

### Root `deps.edn` (development project)

```clojure
{:aliases
 {:dev {:extra-paths ["development/src"]
        :extra-deps {poly/config          {:local/root "components/config"}
                     poly/log             {:local/root "components/log"}
                     poly/jdbc            {:local/root "components/jdbc"}
                     poly/ledger          {:local/root "components/ledger"}
                     poly/report-builder  {:local/root "components/report-builder"}
                     poly/s3              {:local/root "components/s3"}
                     poly/kafka-consumer  {:local/root "components/kafka-consumer"}
                     poly/billing-rest-api {:local/root "bases/billing-rest-api"}
                     poly/reporting-lambda {:local/root "bases/reporting-lambda"}
                     poly/ingest-worker    {:local/root "bases/ingest-worker"}
                     poly/admin-cli        {:local/root "bases/admin-cli"}
                     org.clojure/clojure  {:mvn/version "1.12.2"}}}

  ;; tools.deps does NOT pull test paths from :local/root deps, so list them here
  :test {:extra-paths ["components/config/test"
                       "components/log/test"
                       "components/jdbc/test"
                       "components/ledger/test"
                       "components/report-builder/test"
                       "components/s3/test"
                       "components/kafka-consumer/test"
                       "bases/billing-rest-api/test"
                       "bases/reporting-lambda/test"
                       "bases/ingest-worker/test"
                       "bases/admin-cli/test"
                       "projects/billing/test"]}

  ;; profiles — note the leading + — for interface collisions (see §6)
  :+default {:extra-deps  {poly/invoice {:local/root "components/invoice"}}
             :extra-paths ["components/invoice/test"]}
  :+remote  {:extra-deps  {poly/invoice-remote {:local/root "components/invoice-remote"}}
             :extra-paths ["components/invoice-remote/test"]}

  :poly {:extra-deps {polylith/clj-poly {:mvn/version "0.3.32"}}
         :main-opts  ["-m" "polylith.clj.core.poly-cli.core"]}

  :build {:deps {io.github.clojure/tools.build {:mvn/version "0.10.9"}}
          :ns-default build}}}
```

### A brick `deps.edn`

Generated for you; only third-party libs go here. **Never** reference another brick.

```clojure
{:paths ["src" "resources"]
 :deps {com.github.seancorfield/next.jdbc {:mvn/version "1.3.1002"}}
 :aliases {:test {:extra-paths ["test"]
                  :extra-deps {}}}}
```

### A project `deps.edn`

```clojure
{:deps {poly/config           {:local/root "../../components/config"}
        poly/log              {:local/root "../../components/log"}
        poly/jdbc             {:local/root "../../components/jdbc"}
        poly/invoice          {:local/root "../../components/invoice"}
        poly/ledger           {:local/root "../../components/ledger"}
        poly/billing-rest-api {:local/root "../../bases/billing-rest-api"}

        org.clojure/clojure {:mvn/version "1.12.2"}}

 :aliases {:test    {:extra-paths ["test"] :extra-deps {}}
           :uberjar {:main com.acme.billing-rest-api.core}}}
```

---

## 5. The migration playbook

The governing principle from the Polylith docs, and it is worth tattooing on the agent's forehead:
**migrate first, refactor second.** During phase 1 every service must behave exactly as before. "It's
tempting to try merging components across service boundaries as soon as we notice similarities. We'd
advise against this"
([Transitioning](https://polylith.gitbook.io/polylith/conclusion/should-you-convert-your-system)).

### Phase 0 — Prerequisites and inventory

1. Install Java 21+, the Clojure CLI, and git. Then install `poly`:
   - macOS: `brew install polyfy/polylith/poly`
   - Linux/Windows: download the jar from
     [releases](https://github.com/polyfy/polylith/releases) and wrap it in a shell script, or skip
     the standalone install and use the `:poly` alias (`clojure -M:poly ...`)
     ([Install](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/install)).
2. Inventory, in a scratch file, for each existing repo:
   - deployable artifacts it produces (uberjar? lambda? library? several?)
   - its entry-point namespaces (these become bases)
   - namespaces duplicated in more than one repo (these are your first component candidates)
   - internal libraries it consumes that you also maintain (these get defrosted)
   - every third-party lib and its version, per repo — the version skew list matters in §7
3. Decide the single `:top-namespace`, e.g. `com.acme`. Every brick namespace will sit under it.
   Existing namespaces will be renamed; plan for it.

### Phase 1 — Create the workspace and land each repo as a project

Create the workspace at the root of a fresh repo:

```bash
poly create workspace name:acme top-ns:com.acme dialects:clj branch:main :commit
```

Or, to create it inside an existing git repository (omit `name:`, run from the repo root; `:commit`
is not supported in this mode):

```bash
cd acme && poly create workspace top-ns:com.acme dialects:clj
```

If you want to preserve the git history of each legacy repo, do the merge with git before you start
moving code — this is standard git practice rather than a Polylith feature:

```bash
# in each legacy repo, first move everything into a staging subdir and commit,
# then from the monorepo:
git remote add billing ../billing
git fetch billing
git merge --allow-unrelated-histories billing/main
```

Then, **for each legacy repo, one at a time**:

```bash
poly create base    name:billing-rest-api dialect:clj
poly create project name:billing          dialect:clj
```

- Copy the *entire* repo's source, including its API layer, into `bases/billing-rest-api/src`,
  renamed under `com.acme.billing-rest-api.*`.
- Copy its tests into `bases/billing-rest-api/test`.
- Copy all of its third-party deps into `projects/billing/deps.edn`.
- Wire the base into `projects/billing/deps.edn` and into the root `deps.edn` `:dev`/`:test` aliases.
- Add `"billing" {:alias "bill"}` to `:projects` in `workspace.edn`.
- Verify: `poly info`, `poly check`, `poly test :all`, and build the uberjar. Compare artifact
  behaviour with the legacy build. **Commit.**

At the end of phase 1 you have N fat bases, N projects, zero components, and a monorepo that
produces byte-for-byte equivalent behaviour. Nothing is deduplicated yet. That is correct.

### Phase 2 — Tease out components, one at a time

Repeat until the bases are thin:

```bash
poly create component name:jdbc dialect:clj
```

1. Move one cohesive namespace group out of a base into `components/jdbc/src/com/acme/jdbc/`.
2. Write `interface.clj` exposing only what callers actually use; delegate to `core.clj` (or
   whatever the impl namespaces are called).
3. Update callers in the base to `(:require [com.acme.jdbc.interface :as jdbc])`.
4. Move any third-party libs the component needs from the project's `deps.edn` into the
   component's `deps.edn`.
5. Add `poly/jdbc {:local/root "..."}` to the project's `deps.edn` and to the root `:dev` alias;
   add `components/jdbc/test` to the root `:test` alias.
6. `poly check && poly test :all` → commit.

Resist restructuring while extracting. Change as little as possible per step; a green build after
each extraction is what makes the migration safe.

### Phase 3 — Deduplicate across the former repo boundaries

Now the payoff. When `billing` and `reporting` both contain a component that does the same job:

1. Diff the two implementations. Pick one as canonical (or synthesise a third).
2. Widen its interface if the other caller needs more.
3. Point the second project's `deps.edn` at the canonical component; delete the duplicate directory
   and its entries from the root `deps.edn`.
4. `poly deps` to inspect the new dependency matrix; `poly check` for orphan warnings;
   `poly test :all`.

Use `poly info` and `poly deps` continuously here — the matrices are the map of your tangle and they
update instantly.

### Phase 4 — Consolidate deployables (optional but usually worth it)

Once components are shared, ask whether you still need N deployables. Polylith's argument is that
service boundaries drawn to enable code sharing were never really about scaling, and each one costs
real complexity. Merging two projects is now a two-line change to a `deps.edn`
([Transitioning](https://polylith.gitbook.io/polylith/conclusion/should-you-convert-your-system)).

---

## 6. Naming and design conventions

From the [Naming](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/naming) docs:

| Element | Convention | Examples |
| --- | --- | --- |
| Component doing one thing | The action | `validator`, `invoicer`, `purchaser` |
| Component around a concept | The noun | `account`, `car`, `ledger` |
| Third-party API wrapper | Suffix `-api` | `stripe-api`, `foobarcorp-api` |
| Well-known cloud service | Plain or vendor-prefixed service name | `s3`, `dynamodb`, `aws-cloudwatch` |
| Interface | Same name as its primary component | `invoicer` / `invoicer` |
| Alternate impl of an interface | Base name + qualifier | `invoicer-remote` |
| Base | What it does + API type | `invoicer-rest-api`, `report-generator-lambda` |
| Deployable project | What the service does | `invoicer`, `report-generator` |

Design guidance worth internalising:

- Keep interfaces tiny; expose only what's needed. Sort interface fns alphabetically for lookup.
- Interface docstrings describe *what problem is solved*; implementation docstrings describe *how*.
- Prefer delegation over putting logic in the interface — except for genuine one-liners.
- Because `src` can only reach `interface`, publicly-declared implementation fns behave like
  `protected`: testable and debuggable from `test`, unreachable from other bricks.

### Profiles: when two components share an interface

If you have both `invoice` and `invoice-remote` implementing `com.acme.invoice.interface`, the
development REPL would see two copies of that namespace on one classpath. Production is fine —
separate processes — and `poly test` is fine because test runners isolate per project. Only the dev
REPL breaks.

The fix is profiles: aliases prefixed with `+` in the root `deps.edn`, each pulling in one of the
competing components (see the `:+default` / `:+remote` example in §4). `:default-profile-name` in
`workspace.edn` decides which one is merged in when none is specified. Start the REPL with
`-A:dev:test:+default` or `-A:dev:test:+remote`; **a REPL restart is required when switching**
([Profile](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/profile)).

If you never re-specify an interface, you don't need profiles at all.

---

## 7. Managing the dependency tangle after the merge

This is where the monorepo pays for itself, and where an agent should spend time.

**Third-party version skew.** Merging repos will surface libraries pinned at different versions.
Turn this into a build failure:

```clojure
:validations {:inconsistent-lib-versions {:type :error :exclude []}}
```

`poly libs` shows the full matrix of libraries across every brick and project (`x` = used from
`src`, `t` = test-only, plus type and size). `poly libs :outdated` flags upgrades;
`poly libs :update` rewrites the `deps.edn` files. `:keep-lib-versions` in `workspace.edn` pins
specific libs per brick or project when a version really must differ
([Libraries](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/libraries)).

**Brick dependency graph.** `poly deps` renders the brick-to-brick matrix; `poly deps brick:jdbc`
and `poly deps project:billing` narrow it. Cycles are impossible to hide once you're looking at the
matrix.

**Dead and orphan code.** `poly check` emits *Warning 207 — Unnecessary components were found in
project* for components no project actually uses. Genuinely-needed-but-dynamically-loaded components
get whitelisted with `:necessary ["helper"]` under the project in `workspace.edn`
([Validations](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/validations)).

**Unreadable/broken namespaces.** *Error 111* catches files the tool cannot parse; inspect with
`poly ws get:components:jdbc:namespaces:src:core`.

**Machine-readable everything.** The whole workspace is queryable data:
`poly ws get:keys`, `poly ws get:components:keys`, `poly ws get:messages`,
`poly ws get:changes:changed-or-affected-projects since:release skip:dev`. For an *agent* driving
this migration, `poly ws` is the primary API — prefer it over parsing the pretty-printed reports
([Workspace structure](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/workspace-structure)).

---

## 8. Testing and CI

`poly test` computes the set of bricks changed since the last `stable-*` tag and runs the tests for
those bricks *and* everything transitively affected, once per project that includes them.

```bash
poly test                      # changed bricks, in changed/affected projects
poly test :all                 # everything
poly test :dev                 # include the development project
poly test :project             # project-level tests too
poly test project:billing
poly test brick:jdbc
```

Test runners are pluggable ([Test runners](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/test-runners)):

- Bundled in-process `clojure.test` runner (default).
- [Sean Corfield's external test runner](https://github.com/seancorfield/polylith-external-test-runner) — runs each project's tests in a subprocess, avoiding classloader, daemon-thread, and memory issues. Recommended for large workspaces.
- [Imre Kószó's `polylith-kaocha`](https://github.com/imrekoszo/polylith-kaocha) — Kaocha integration.

Per-project `:setup-fn` / `:teardown-fn` and `:include` / `:exclude` brick lists live under
`:projects` in `workspace.edn`.

CI pattern for "build and release only what changed"
([Continuous integration](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/continuous-integration)):

```bash
poly test
poly ws get:changes:changed-or-affected-projects since:release skip:dev color-mode:none
# → tag the release, then build/deploy only the listed projects
```

Tag `stable-<sha>` after a green build so the next run has a baseline; `poly check` returns a
non-zero exit code on errors (warnings do not fail it), which makes it a clean CI gate.

---

## 9. Command cheat sheet

```
poly                          # start the interactive shell (history + autocomplete) — use this
poly info                     # workspace overview + validation; try  info :loc  and  info +
poly check                    # validate only; non-zero exit on error.  check :dev  includes dev
poly deps                     # brick dependency matrix
poly libs                     # third-party library matrix;  :outdated  :update  :compact
poly diff                     # files changed since last stable point
poly test                     # incremental tests
poly ws get:...               # the workspace as EDN data
poly doc                      # open docs in a browser; doc page:profile, doc more:blog-posts
poly create workspace name:acme top-ns:com.acme dialects:clj branch:main :commit
poly create component name:invoice dialect:clj
poly create component name:invoice-remote dialect:clj interface:invoice
poly create base      name:billing-rest-api dialect:clj
poly create project   name:billing dialect:clj
```

`poly` is also usable without a standalone install via `clojure -M:poly <cmd>`. Use `clojure`, not
`clj`, when launching the shell — `rlwrap` interferes.

---

## 10. Editor setup

Nothing special is required: a Polylith workspace is an ordinary `deps.edn` project whose `:dev`
alias happens to include a lot of `:local/root` deps. Start the REPL with the `:dev` and `:test`
aliases (plus a profile if you use them).

**Emacs / Spacemacs with CIDER.** CIDER supports `:local/root` deps. Set the aliases so
`cider-jack-in` picks them up — e.g. in `.dir-locals.el` at the workspace root:

```elisp
((clojure-mode . ((cider-clojure-cli-aliases . ":dev:test:+default"))))
```

Then jack in from anywhere in the workspace. Because every brick is on one classpath, `M-.`
navigation, `cljr` refactorings, and rename-across-workspace all work over the entire codebase —
which is exactly the capability you lose when the code is split across repos.

**VS Code with Calva.** `poly create workspace` writes a `.vscode/settings.json` for you. Use
`Calva: Start a Project REPL and Connect (Jack-In)` and select the `dev` and `test` aliases. The
[RealWorld example app](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app)
is set up for exactly this flow and is the best thing to open first.

**Cursive** (1.13.0+) has explicit Polylith support and can activate profiles from the Clojure Deps
tool window ([Cursive Polylith guide](https://cursive-ide.com/userguide/polylith.html)).

If your editor genuinely cannot handle `:local/root`, fall back to listing brick `src` and
`resources` directories in `:extra-paths` — but you then have to duplicate each brick's library deps
at the root, so avoid it ([Component](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/component)).

---

## 11. Pitfalls

- **Refactoring during extraction.** The single most repeated warning in the docs. Move code, don't improve it. Improve it in a separate commit.
- **Forgetting the root `deps.edn`.** `poly create` deliberately does not edit your files. Every new brick needs a `:local/root` entry under `:dev` *and* a test path under `:test`, or the REPL won't see it.
- **Test paths don't propagate.** tools.deps does not pull `:aliases :test :extra-paths` through `:local/root` deps. `poly test` figures it out, but your IDE and any raw `clojure -M:test` will not. Hence the explicit list at the root.
- **Bricks referencing bricks.** A brick's `deps.edn` lists libraries only. Composition happens in projects. If you find yourself wanting a brick-to-brick `:local/root`, you've misunderstood the model.
- **Interface reserved word.** `interface` is reserved in ClojureScript (and in Kotlin, if you consume from there). Set `:interface-ns "ifc"`, or pass `interface:ifc` per component.
- **Resources at the root of `resources/`.** Always nest under a brick-named subdirectory or you will get classpath collisions in uberjars.
- **Building more than one library from one workspace.** Don't. If two published jars share a component, classpath ordering decides which version wins. The docs recommend one library per workspace ([Artifacts](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/artifacts)).
- **Profiles require a REPL restart** when added or switched.
- **Learning curve is real.** Furkan Bayraktar likens it to coming to FP from OO — a genuine reorientation, then it disappears ([FAQ](https://polylith.gitbook.io/polylith/conclusion/faq)).

---

## 12. Projects using Polylith

### In production
([Production systems](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/production-systems))

| Organisation | Scale / notes |
| --- | --- |
| [World Singles Networks](https://worldsinglesnetworks.com) | Online dating platform, 100+ web properties. Migrated April 2021 → December 2022; 136K lines of Clojure at completion, later 138,497 lines across 21 projects, 21 bases, 144 components. The most thoroughly documented migration in the ecosystem — read Corfield's blog series. |
| [Scrintal](http://scrintal.com) | Polylith since the first commit, April 2019. Pivoted the entire product in 2020 and reused most bricks across the old and new products. ~60 components deployed as 4 services. |
| [Greenlabs](https://greenlabs.co.kr) | Seoul agtech startup. Clojure since 2020, Polylith since March 2022, 93K lines. Event-driven, Kafka plus many AWS Lambdas — multiple serverless projects from one repository. |
| [Qantas](https://medium.com/qantas-engineering-blog/leveraging-polylith-to-improve-consistency-reduce-complexity-and-increase-changeability-2031dd3d5f3d) | Felix Barbalet's write-up on applying Polylith to an existing production system. |
| [polyfy/polylith](https://github.com/polyfy/polylith) | The `poly` tool builds itself from a Polylith workspace. The best reference codebase for interface design and sub-namespace interfaces. |

### Example workspaces to read
([Example systems](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/example-systems))

- [clojure-polylith-realworld-example-app](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app) — full RealWorld backend (Ring, auth, CRUD, specs); the [`cljs-frontend` branch](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app/tree/cljs-frontend) adds a re-frame frontend in the same workspace. Includes a [step-by-step build-it-yourself guide](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app/blob/master/how-to.md) and a [Gitpod setup](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app/blob/master/gitpod.md). **Start here.**
- [usermanager-example (polylith branch)](https://github.com/seancorfield/usermanager-example/tree/polylith) — Sean Corfield's small web app; other branches hold non-Polylith versions of the *same* app, so the diff is the clearest possible illustration of what changes.
- [polylith-integrant](https://github.com/marksto/polylith-integrant) — how to do system/state management with Integrant inside Polylith.
- [game-of-life](https://github.com/tengstrand/game-of-life) — the minimum viable workspace.
- [demo-rama-electric](https://github.com/jeans11/demo-rama-electric) — Rama + Electric.

---

## 13. Bibliography

### Official documentation

- [Polylith — high-level documentation](https://polylith.gitbook.io/polylith) — the architecture itself, language-agnostic. Start with [Polylith in a nutshell](https://polylith.gitbook.io/polylith/introduction/polylith-in-a-nutshell).
- [Sharing code](https://polylith.gitbook.io/polylith/introduction/sharing-code) — the four ways to share code and why three of them fail. Read this first if you need to justify the migration to anyone.
- [Testing incrementally](https://polylith.gitbook.io/polylith/introduction/testing-incrementally)
- [Transitioning to Polylith](https://polylith.gitbook.io/polylith/conclusion/should-you-convert-your-system) — the migration steps from monolith / microservices / serverless.
- [Advantages of Polylith](https://polylith.gitbook.io/polylith/conclusion/advantages-of-polylith)
- [Current architectures](https://polylith.gitbook.io/polylith/conclusion/current-arcitectures)
- [Simplicity](https://polylith.gitbook.io/polylith/architecture/simplicity)
- [FAQ](https://polylith.gitbook.io/polylith/conclusion/faq) — protocols vs interfaces, mixing languages, state, polymorphism, multiple bases per artifact.
- Machine-readable index for agents: [llms.txt](https://polylith.gitbook.io/polylith/llms.txt). Any doc page is available as Markdown by appending `.md` to its URL.

### `poly` tool reference (cljdoc — always use `CURRENT`)

- [Tool documentation home](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/readme)
- [Install](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/install) · [Workspace](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/workspace) · [Component](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/component) · [Interface](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/interface) · [Base](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/base) · [Project](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/project) · [Development](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/development)
- [Commands](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/commands) — full CLI reference · [Flags](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/flags) — decoding the `st-` markers in `poly info`
- [Configuration](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/configuration) — every `workspace.edn` key
- [Workspace structure](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/workspace-structure) — the EDN data model behind `poly ws`; the agent-facing API
- [Profile](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/profile) · [Dependencies](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/dependencies) · [Libraries](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/libraries) · [Naming](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/naming) · [Source code](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/source-code)
- [Testing](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/testing) · [Test runners](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/test-runners) · [Validations](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/validations) · [Continuous integration](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/continuous-integration) · [Build](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/build) · [Artifacts](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/artifacts) · [Tagging](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/tagging) · [Upgrade](https://cljdoc.org/d/polylith/clj-poly/CURRENT/doc/upgrade)
- Source of the docs (AsciiDoc, greppable): [polyfy/polylith/doc](https://github.com/polyfy/polylith/tree/master/doc)
- [GitHub repo](https://github.com/polyfy/polylith) · [Releases](https://github.com/polyfy/polylith/releases) · [Clojars](https://clojars.org/polylith/clj-poly)

### Migration experience reports

- Sean Corfield, **deps.edn and monorepos** — the 11-part series covering a 136K-line migration end to end. [Part I](https://corfield.org/blog/2021/02/23/deps-edn-monorepo/) · [II](https://corfield.org/blog/2021/04/21/deps-edn-monorepo-2/) · [III](https://corfield.org/blog/2021/06/06/deps-edn-monorepo-3/) · [V](https://corfield.org/blog/2021/08/25/deps-edn-monorepo-5/) · [VI](https://corfield.org/blog/2021/10/01/deps-edn-monorepo-6/) · [IX](https://corfield.org/blog/2022/11/05/deps-edn-monorepo-9/) · [XI (final)](https://corfield.org/blog/2023/07/15/deps-edn-monorepo-11/)
- Felix Barbalet, [Leveraging Polylith to improve consistency, reduce complexity and increase changeability](https://medium.com/qantas-engineering-blog/leveraging-polylith-to-improve-consistency-reduce-complexity-and-increase-changeability-2031dd3d5f3d) (Qantas Engineering, 2024)

### Conceptual background

- Joakim Tengstrand, [Understanding Polylith through the lens of hexagonal architecture](https://tengstrand.github.io/blog/2023-11-01-understanding-polylith-through-the-lens-of-hexagonal-architecture.html) (2023)
- Joakim Tengstrand, [The origin of complexity](https://tengstrand.github.io/blog/2019-09-14-the-origin-of-complexity.html) (2019)
- Joakim Tengstrand, [How Polylith came to life](https://tengstrand.github.io/blog/2018-10-02-how-polylith-came-to-life.html) (2018)
- Joakim Tengstrand, [The Micro Monolith architecture](https://tengstrand.github.io/blog/2016-12-28-the-micro-monolith-architecture.html) (2016) — the predecessor idea
- Joakim Tengstrand, [Tetris-playing AI the Polylith way, part 1](https://tengstrand.github.io/blog/2025-12-28-tetris-playing-ai-the-polylith-way-1.html) and [part 2](https://tengstrand.github.io/blog/2026-01-11-tetris-playing-ai-the-polylith-way-2.html) — a recent worked example in Clojure and Python
- ClojureVerse, [Polylith: a software architecture based on lego-like blocks](https://clojureverse.org/t/polylith-a-software-architecture-based-on-lego-like-blocks/2976) and [On Polylith](https://clojureverse.org/t/on-polylith-from-amazing-ideas-thread/7410)

### Talks and podcasts

- James Trunk, [Polylith in a nutshell](https://www.youtube.com/watch?v=Xz8slbpGvnk) — 10 minutes, the fastest way to get the shape of it
- Joakim Tengstrand & Furkan Bayraktar, [The last architecture you will ever need](https://www.youtube.com/watch?v=pebwHmibla4) — 39 minutes
- Sean Corfield, [Collaborative learning: Polylith](https://www.youtube.com/watch?v=_tpNKAv4fro) — 70 minutes, LA Clojure Users Group; the practitioner's view of running it at scale
- Joakim Tengstrand, [Polylith — a software architecture based on LEGO-like blocks](https://www.youtube.com/watch?v=wy4LZykQBkY) — clojureD 2019
- Vedang Manerikar, [Developer tooling for speed and productivity](https://youtu.be/pVvuyaRDA58?t=1333) — IN/Clojure 2024
- ClojureStream podcast with Jacek Schae: [part 1](https://podcasts.apple.com/se/podcast/s4-e21-polylith-with-joakim-james-and-furkan-part-1/id1461500416?i=1000505948894) · [part 2](https://podcasts.apple.com/se/podcast/s4-e22-polylith-with-joakim-james-and-furkan-part-2/id1461500416?i=1000507542984)

### Tooling and community

- [seancorfield/polylith-external-test-runner](https://github.com/seancorfield/polylith-external-test-runner) — subprocess test runner
- [imrekoszo/polylith-kaocha](https://github.com/imrekoszo/polylith-kaocha) — [Kaocha](https://github.com/lambdaisland/kaocha) integration
- [Cursive Polylith support](https://cursive-ide.com/userguide/polylith.html) · [Calva](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva) · [CIDER](https://cider.mx/)
- [#polylith on Clojurians Slack](https://clojurians.slack.com/archives/C013B7MQHJQ) — the maintainers are active and responsive
- [Polylith for Python](https://davidvujic.github.io/python-polylith-docs) (David Vujic) — useful if any of your services are Python; see also [A fresh take on monorepos in Python](https://davidvujic.blogspot.com/2022/02/a-fresh-take-on-monorepos-in-python.html)
- [Japanese translation of the poly tool docs](https://zenn.dev/shinseitaro/books/clojure-polylith) (Shinsei Taro)
- [Clojure deps.edn reference](https://clojure.org/reference/deps_edn) · [tools.deps](https://github.com/clojure/tools.deps)

---

## 14. Suggested order of work for the agent

1. Read [Sharing code](https://polylith.gitbook.io/polylith/introduction/sharing-code) and [Transitioning to Polylith](https://polylith.gitbook.io/polylith/conclusion/should-you-convert-your-system). Twenty minutes.
2. Clone and run [usermanager-example (polylith branch)](https://github.com/seancorfield/usermanager-example/tree/polylith). Diff it against the `master` branch of the same repo.
3. Install `poly`, create a throwaway workspace, and follow the tutorial through workspace → component → base → project → profile.
4. Produce the inventory from §5 Phase 0 for the real repos, and commit it into the new monorepo as `doc/migration-plan.md`.
5. Execute Phase 1 for the smallest repo first. Get CI green. Then the rest.
6. Only then start Phase 2.

The workspace-as-data interface (`poly ws get:...`) is what makes this tractable to automate: every
validation, dependency edge, change set, and library version is retrievable as EDN, so migration
progress can be measured rather than guessed.
