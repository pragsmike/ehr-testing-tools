# Charter — `kernel`

> **Draft for the author's edit.** Every line below is derived from a
> shipped surface — `src/ehrt/kernel/interface.clj` and the namespaces
> it delegates to, their own docstrings, and the ADRs those docstrings
> cite. Nothing here is invented. Where the contract is genuinely
> unclear the charter says **UNCLEAR** and gives the competing
> readings rather than picking one.

## 1. Mission

Own the vocabulary and the primitives that every other brick needs and
none of them should restate: the result envelope, content digests,
the artifact cache, the canonicalizer registry, locator parsing,
subprocess invocation, and loud filesystem operations.

Named hole **H4** in ADR-0002 (R14); opened as a component and closed by
ADR-0008.

## 2. Interface contract

Every var on `ehrt.kernel.interface`. The seam is sized by *grep of
actual external callers* — judge, the corpus domain's own
corpus/check/lint, and the CLI directly since stage 3 retired the
`tools` façade (ADR-0018) — not by copying each source namespace's
public API. A function with no caller outside this component stays
unexported (ADR-0008 carries the census).

**The result envelope** (`ehrt.kernel.result`) — the result-not-throw
doctrine's shared vocabulary, pattern nursery #11.

- `ok` — `(ok payload)` → `{:status :ok :payload payload}`. No
  category: there is nothing to classify.
- `rejected` — `(rejected category payload)`. A legitimate, expected
  non-success: the check ran and the answer is no.
- `error` — `(error category payload)`. An operational failure: the
  check could not run at all.
- `ok?` — `(ok? r)` → true when `:status` is `:ok`.
- `rejected?` — `(rejected? r)` → true when `:status` is `:rejected`.
- `error?` — `(error? r)` → true when `:status` is `:error`.
- `valid?` — `(valid? r)` → true when `r` conforms to the `Result`
  schema. This is `result/valid?`; see UNCLEAR-K2 on the name.

**Digests** (`ehrt.kernel.digest`) — hex-encoded SHA-256, three input
shapes.

- `sha256-file` — `(sha256-file path)` → hex digest of a file's bytes.
- `sha256-string` — `(sha256-string s)` → hex digest of a string's
  UTF-8 bytes.
- `sha256-bytes` — `(sha256-bytes bs)` → hex digest of a raw byte
  array. Exported because the spool's own per-item hashing
  (`ehrt.corpus-io.spool`, SS-3) needs it directly rather than through
  `sha256-string`'s UTF-8 re-encoding.

**Artifacts** (`ehrt.kernel.artifact`) — the content-addressed cache.

- `fetch` — ensures an artifact is present and hash-verified in the
  cache, downloading only if necessary. Short-circuits with **no
  network access** when a verified copy is already cached.
- `read-lockfile` — `(read-lockfile path)` → `ok {:artifacts [...]}`,
  or `error` `:not-found` / `:parse-failed` / `:invalid-lockfile`.
- `resolve-artifact` — looks up name+version in a lockfile's
  `:artifacts` and answers **strictly from the cache — never touches
  the network**. Named `resolve-artifact`, not `resolve`, to avoid
  shadowing `clojure.core/resolve`; the same discipline the
  pre-extraction `tools` façade already used (ADR-0002), carried
  forward.
- `resolve-and-extract` — resolves as above, then ensures the cached
  archive is unpacked under a content-addressed `extracted-dir`,
  extracting only when not already extracted (idempotent).
- `find-executable` — finds a file at a relative path (e.g.
  `bin/java`) anywhere under a *one-level* subdirectory of a root,
  because archives extract to a single version-named top directory
  whose exact name this deliberately does not need to know.

**Canonicalizers** (`ehrt.kernel.canonical`) — a keyed registry.

- `register!` — `(register! entry)` → `ok {:id :version}` or
  `rejected :invalid-entry`. Keyed by `[id version]`.
- `lookup` — `(lookup id version)` → the registered entry. **See
  UNCLEAR-K1**: no docstring, and it returns a bare value rather than
  a result envelope.
- `apply-canonicalizers` — applies an *ordered vector* of
  `[id version]` pairs to data in exactly that order — composition
  order is never implicit, so anything but a vector is rejected.
  Returns `ok {:data ... :applied [...]}`.

**Locators** (`ehrt.kernel.locator`) — a format-tagged path envelope.

- `Locator` — the schema: `{:format <one of :fhir :v2 :table :xpath>
  :path <non-empty string>}`.
- `make` — `(make format path)` → an `ok` Locator, or **rejects (not
  throws)** an unknown format or an empty path. Grammar-specific
  validity is a later, format-dispatched concern.
- `fhir-data-path` — parses a FHIR locator's `:path`
  (`"entry[0].resource.gender"`) into a data-path vector of string
  keys and integer indices, usable directly with
  `get-in`/`assoc-in`/`update-in`. `ok [...]` or
  `rejected :invalid-fhir-path`.
- `v2-data-path` — the HL7 v2 counterpart, parsing
  segment/repeat/field/component/subcomponent. MSH-1 is refused with
  its own actionable hint.

**Invocation** (`ehrt.kernel.invocation`).

- `run-invocation!` — executes command+args as a subprocess,
  redirecting stdout/stderr to given files, returning
  `ok InvocationRecord` or `error :spawn-failed` when the process
  could not even be started. Named `run-invocation!`, not `run!`, to
  avoid shadowing `clojure.core/run!` — a collision caught by this
  extraction's own verification run as a load-time WARNING, not by
  static review; the name borrows `corpus.generate`'s own
  `:run-invocation` injection-seam name rather than inventing one.

**Filesystem** (`ehrt.kernel.io`) — *result or loud*, ADR-0078. Each of
these exists because the underlying Java call reports failure by a
boolean that is easy to ignore.

- `list-files` — `ok` a vector of entries (empty when the directory
  genuinely has none), or `error :listing-failed {:path}` when the
  lister returns nil — an I/O failure, **not** an empty directory.
- `existing-dir-nonempty?` — guards a `:fail-if-exists` /
  `:out-dir-exists` check. A path that does not exist or is not a
  directory is safe → `ok false`.
- `rename!` — renames via `.renameTo`, whose `false` return (a
  cross-filesystem rename, common on CI runners) means the file was
  **never moved, silently**, unless checked.
- `mkdirs!` — ensures a directory exists, creating parents, returning
  a `java.io.File`; loud on the ambiguous `false`.
- `delete!` — deletes loudly, returning a `java.io.File`.
- `delete-quietly!` — the **declared exception** to `delete!`:
  best-effort, never throws; true when the path is absent afterwards.

## 3. Data shapes owned

`kernel` is the authority for these; no other brick may restate them.

| shape | where | what it fixes |
|---|---|---|
| `Result` | `result.clj` | `{:status #{:ok :rejected :error} :category? :payload}` |
| `Artifact`, `Lockfile` | `artifact.clj` | a lockfile's artifact vector |
| `ArtifactKind` | `artifact.clj` | `:engine :profile :module :runtime :other` |
| `LicenseStatus` | `artifact.clj` | includes `:use-permitted--unstated--confirmation-pending` (ADR-0005, 2026-07-24 amendment) |
| `ResolvedVia` | `artifact.clj` | `:artifact-cache` or `:deps-edn` (P2-3, ruled 2026-07-31) |
| `Entry` | `canonical.clj` | a canonicalizer registry entry |
| `InvocationRecord` | `invocation.clj` | command, args, and the digests of its output files |
| `Locator` | `locator.clj` | `{:format :path}`, formats `#{:fhir :v2 :table :xpath}` |

## 4. Invariants guaranteed

- **Result-not-throw.** Every capability function returns `:ok` /
  `:rejected` / `:error` rather than throwing. `:rejected` is a
  legitimate, expected outcome; `:error` is an operational failure.
  Exceptions stay reserved for programmer error.
- **`resolve-artifact` never touches the network.** It answers
  strictly from the cache. Only `fetch` may download, and only when a
  verified copy is absent.
- **Content addressing.** A cache entry and its `extracted-dir` are
  both keyed by the artifact's own sha256, so extraction is a derived,
  idempotent side effect rather than a state to manage.
- **Composition order is explicit.** `apply-canonicalizers` takes a
  vector and rejects anything else.
- **No silent filesystem failure.** Every `io` operation converts an
  ignorable boolean into a result or a throw; `delete-quietly!` is the
  one declared exception.

## 5. Non-goals

- **Not a grammar validator.** `make` checks that a locator is
  well-formed as an envelope; whether the path is valid *for its
  format* is dispatched later, by the format's own parser.
- **Not a downloader by default.** Network I/O is confined to
  `default-downloader!`, and subprocess spawning to
  `default-extractor!` — both injectable, and both the only functions
  in their namespaces that reach outside the process.
- **Not a full re-export.** The seam carries what external callers
  actually reach, not each namespace's public API — so
  `env-override`, `cache-dir`, `extracted-dir`, `entries`,
  `registry-snapshot`, and `reset-registry!` are deliberately absent
  from the interface even though they are public in their own
  namespaces.
- **Knows nothing of the domain.** No HL7, no FHIR resources, no
  simulation, no judging — only the shapes above.

## 6. Forbidden edges

`kernel` requires **no other brick**, in src or in test. It is the
root of the dependency order, and every other brick may depend on it.
Any require of another `ehrt.*.interface` from this component's `src`
would create the workspace's first cycle candidate and must be refused.

## UNCLEAR — the author's review queue

- **UNCLEAR-K1 — `lookup`'s contract.** It is the one exported var in
  this component with **no docstring**, and its body is a bare
  `(get @registry [id version])`, so it returns the entry or `nil`
  rather than a result envelope. Two readings, and the shipped
  surface does not settle which: *(a)* deliberate — a pure map read
  is not a "capability function" in the result-doctrine's sense, so an
  envelope would be ceremony; *(b)* an oversight — every other
  exported function in this component returns a result, and a caller
  who mistakes `nil` for "registered but empty" gets no help. Note
  the doctrine's own wording is "every capability function", which
  does not obviously classify a registry read either way.
- **UNCLEAR-K2 — `valid?` at the seam.** Three namespaces in this
  component define `valid?` (`result`, `locator`, `invocation`), and
  the interface exports exactly one of them — `result/valid?`. A
  reader at the seam cannot tell that from the seam alone, and
  `kernel/valid?` reads as if it might validate a `Locator`. This is
  a naming observation, not a defect: the delegation is unambiguous
  in `interface.clj`'s own source. Whether it is worth renaming
  (e.g. `valid-result?`) is the author's call.
