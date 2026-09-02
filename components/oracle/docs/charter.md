# Charter — `oracle`

> **Draft for the author's edit.** Derived from
> `src/ehrt/oracle/interface.clj`, `digest.clj`,
> `bin/regression-oracle`, `bin/oracle-lib.sh`, and the ADRs those
> docstrings cite. **UNCLEAR** marks a contract the shipped surface
> does not settle.

## 1. Mission

Own the **regression oracle's digest side**: run a fixed set of
seeded, from-scratch simulations and reduce each to a SHA-256 digest,
so two commits can be compared byte-for-byte rather than by assertion.

Promoted to standing equipment on 2026-08-05 (AR-P-2). It is
**dev/CI equipment, not a shipped capability.**

## 2. Interface contract

The whole seam is one entry point.

- `-main` — `(-main out-dir)` → writes one digestable artifact per
  golden root under `out-dir`. Delegates to `ehrt.oracle.digest/-main`,
  where **all digest logic lives**; this namespace re-exports only the
  one thing an external caller needs.

**The only real caller is `bin/regression-oracle`'s own synthetic
per-worktree classpath**, which invokes `-m ehrt.oracle.interface` —
repointed from `-m ehrt.oracle.digest` directly at AR-P-3, so that a
dev tool enters through its own component's interface rather than
reaching an implementation namespace, the same discipline every
cross-component call in this workspace follows.

## 3. Data shapes owned

- The **golden-root set**: which seeded runs are digested, and with
  which fixed seeds. `digest.clj` carries this, and it is versioned
  with the code rather than configured.
- The **digest artifact**: the `{:ground-truth :hl7}` pair written per
  root, which `bin/regression-oracle` then sha256s **as one file**.

## 4. Invariants guaranteed

- **Fixed draws.** Every golden run is seeded, so a digest changes
  only when behaviour changes.
- **From-scratch classpath per worktree.** `bin/regression-oracle`
  builds two disposable `git worktree`s and points `:local/root` at
  *that* worktree — **never at a historical commit's own `deps.edn`**.
  `sim_brick_dir_for` resolves the simulator brick's directory per
  worktree, which is what lets a bracket span a rename (ADR-0162
  found this the hard way: one hard-coded literal cannot serve both
  sides of a bracket).
- **A digest change must be declared.** An undeclared change to
  `digest.clj` itself **aborts the bracket** (exit 1) rather than
  silently rebaselining; `--declared-digest-change` is the explicit
  override.
- **The phrase is reserved.** `rulings.md#R-oracle-script-contract`
  reserves "the regression oracle held" for **`bin/regression-oracle`'s
  own whole-pair output**. A test-count or assertion-count comparison
  is **not an oracle** and may not be reported as one (ADR-0030 J2,
  ratified after that exact substitution went uncaught through two
  sessions).

## 5. Non-goals

- **Not a shipped capability.** No `ehrt` command mounts it; it is
  reached only from `bin/`.
- **Digests only what it digests.** The oracle is **blind** to
  anything outside the golden-root set — notably `engine/replay`,
  where an IDENTICAL verdict is *vacuous* rather than reassuring. A
  session must know which surfaces its bracket can actually see.
- **Cannot see a ground-truth-only claim.** Because a root's
  `{:ground-truth :hl7}` pair is digested as **one file**, an
  emission-only change makes every engine-layer root differ — the
  correct outcome, and simultaneously destructive of the oracle's
  ability to say anything about the ground-truth half. That is what
  `bin/ground-truth-bracket` exists for, and its output is
  **explicitly not a regression-oracle claim.**
- **Does not decide baselines.** It digests; the script brackets.

## 6. Forbidden edges

Requires `kernel`, `patient-simulator`, `sim`, `sim-emit-hl7`,
`sim-engine` and `sim-model` in `src` — the widest dependency set of
any component, and correctly so: to digest a run it must be able to
produce one.

**Nothing in the workspace requires `oracle`** — it is the only
component with no dependents, and `clojure -M:poly deps` gives it no
column for that reason. That is the property that keeps its breadth
harmless.

Must never be required **by** any component or base. A production
brick depending on the oracle would invert the relationship: the thing
being measured would depend on the instrument.

## UNCLEAR — the author's review queue

- **UNCLEAR-O1 — a dev tool's needs shape a production seam.**
  `patient-simulator` exports `dob-epoch-day` for exactly one reason:
  `digest.clj` needed it and nothing else did (AR-P-2's census found
  it the only gap). So this component, which nothing depends on,
  nonetheless widened a component that eight things depend on. Two
  readings: *(a)* correct — the alternative is `digest.clj` reaching
  an implementation namespace, which is worse; *(b)* it means
  "sized by live consumers" now includes consumers that ship to
  nobody, and that rule should say so explicitly. Raised from the
  other side as `patient-simulator`'s UNCLEAR-P2.
- **UNCLEAR-O2 — the golden-root set is code, and its size is
  reported per run.** Brackets have reported 9, 11 and 35 roots at
  different dates. Nothing on this seam says what governs membership,
  when a root is added, or whether adding one is itself a declared
  change the way editing `digest.clj` is. Given that a root is what
  the oracle can *see*, the membership rule is as load-bearing as the
  digest logic.
