## ADR-0185 — the ground-truth rebaseline: reinstated stays close, churn candidates in patient-id order

**Status:** Accepted 2026-09-30 (author rulings "Accept recommendation"
and "Prioritize simplicity of design over strict preservation of
determinism", design channel 2026-09-30; build session same day, R30).
Closes `roadmap.md#cancel-discharge-reopens-an-encounter-that-never-closes`
and `roadmap.md#determinism-hash-order-dependence`. Event schema
unchanged (no new kind).

### Context

Every line number below is at `c19b1024`, read this session.

**Row 4.** A legal `:cancel-discharge` reinstates the stay its
`:discharge` ended (`evolve`'s `reopen-encounter`, deliberate: a
reinstated stay is ONE encounter), and nothing ever closed it again
(`decide.clj:1838-1875`). Churn's static applicability oracle sets
`:has-uncancelled-discharge?` only after a pathway's own `:discharge`,
the last authored step of every shipped pathway, so the insertion lands
in the end gap with nothing behind it. 224 of 224 in downstream's 28k
corpus, 54 of 55 at the 10^5 cell. The catalog permits it by
construction (`every-encounter-is-opened-and-closed-or-still-open`
reads "or still open"), so no gate was ever red; ADR-0183's `describe`
measures it as `reinstated-stay-without-closer`.

**Row 1.** `decide :merge` and `decide :bed-swap` resolve their peer
with `streams/uniform-choice`, positionally, over a vector whose order
was the `:eligible-index` carrier's iteration order — proven equal to
`:patients`' `PersistentHashMap` order everywhere except at a full
32-bit `hasheq` collision (ADR-0180's R-hash-order addendum,
2026-09-07). Every churn-bearing corpus therefore depended on a hash
order Clojure owns, against `sim/ADR-0002`'s "no hash-order
dependence", which AGENTS.md had stopped claiming at ADR-0180 site 5.

Both rows were held for the downstream window since 2026-09-12 because
each is draw-affecting; the author ruled them together so the corpus
moves once.

### Decision

1. **The closer.** `decide :cancel-discharge`'s success arm returns
   `:prepend-steps [{:type :delay :from A :to B} {:type :discharge}]`.
   The `:delay` draws on the subject's `:patient` stream through the
   existing `decide :delay`; the `:discharge` runs through the existing
   `decide :discharge` and reads the location the cancel restored, so
   the bed is released (and, with `:bed-cycle`, enters its turnaround)
   exactly as any discharge does. Every reinstated stay gets it; a
   rejected cancel reinstated nothing and owes nothing. No new kind, no
   `InjectChurn` change, `churn/strip` untouched: the closer is runtime
   work, never pathway IR. `:order`'s `:schedule-followup` is the
   precedent for a decide owing further work; `:prepend-steps` is the
   shape for work owed by the same patient.
2. **The constant pair.** `churn/reinstated-stay-delay-minutes` is
   `[240 1440]` — four hours to a day. **The values are this session's
   choice, not a ruled figure**: the ruling fixed the SHAPE (one authored
   range for every reinstated stay), and no range was named in the tree
   or the handoff. A cancelled discharge is one the ward calls back the
   same day, so the reinstated stay is hours, not a second admission;
   and since the stay previously outlived the run, any finite range
   holds a bed for less time than the engine did before.
3. **The sort.** `fold/merge-eligible` and `fold/swap-eligible` return
   their candidates SORTED by `:patient-id` before `uniform-choice`
   resolves them. No map's iteration order — hash, array or collision
   node — reaches a draw; the carrier's class
   (`fold/empty-eligible-index`, R-empty-carrier) stops mattering to the
   answer. The draw COUNT is unchanged (one `.nextInt` per dynamic
   merge or swap); only its resolution moves. ADR-0180's R-hash-order
   section carries a dated supersession note.
4. **Census, every collection-resolving draw.** Besides the two views,
   `streams/uniform-choice` has one caller (`decide :outpatient-visit`,
   over `:providers`, a config vector); `sim-model/facility`'s private
   `choose` reads bed-id vectors derived from ward config and the
   ward-filtered provider vector; `person-simulator/process`'s `pick`
   reads three authored enums and the household roster, built in time
   order. None reads a hash map's order. AGENTS.md's inherited
   determinism sentence again says "no hash-order dependence".

### What the simplicity ruling relaxed, and what it did not

*Relaxed:* byte identity with every prior churn-bearing corpus, and the
fixed draw consumption of this sweep — the closer adds a `:patient`
draw per reinstated stay (and the discharge's own follow-up and
turnaround draws where those opt-ins are on), so streams shift after
the first closer. Downstream accepted a different dataset for the same
arguments, this once.

*Not relaxed:* determinism itself — the same arguments give the same
bytes, gated by `ehrt.sim.run-test/the-dense-750-cell-is-byte-identical-across-two-runs`
— and the checker: no invariant's text changed;
`admitted-occupies-one-slot` and the encounter laws read as before.

### Laws

- `ehrt.sim-engine.engine-test/a-reinstated-stay-is-discharged-again-after-a-delay`
  — an authored `:cancel-discharge` after `:discharge` yields a second
  `:discharge` after a positive delay, the bed released, `check-all` ok.
- `ehrt.sim.run-test/no-reinstated-stay-outlives-its-corpus` —
  `describe`'s `reinstated-stay-without-closer` reads 0 on
  seed-202-ed-tuesday and the dense 750 cell (1 and 6 before), each
  carrying a legal `:cancel-discharge`.
- `ehrt.sim-engine.eligible-index-test` — the views equal the
  from-scratch scan SORTED at every replay entry; the colliding pair and
  every insertion permutation of a small population answer the same
  `:patient-id`-ordered views.

### What moved

**Predicted before any comparison**, from the base corpora themselves:
replay each root's base log and stop at the first event that can move —
a legal `:cancel-discharge`, or a churn `:merge`/`:bed-swap` (no
`:cause`) whose drawn position in the hash-ordered candidates holds a
different id once sorted. Everything before that event is identical by
construction. Prediction: **DIFFERS = {`bed-cycle`, `encounter-horizon`,
`scheduling`}** — exactly the three roots that run `--churn` — and every
other root IDENTICAL. `chatter-charges`' and `demographic-fold`'s
merges are identification merges (`:cause :identification`), which
draw nothing.

**Measured**, base `c19b1024`, target `fecbe43b`, both
`--declared-digest-change`:

- `bin/ground-truth-bracket`: 40 roots digested, 3 batch roots skipped
  by construction; DIFFERS on `bed-cycle`, `encounter-horizon`,
  `scheduling`; the other 37 IDENTICAL.
- `bin/regression-oracle`: 43 roots; DIFFERS on the same three; the
  other 40 IDENTICAL.

**Gated corpora.** One of four moved: `seed-202-ed-tuesday`, first
difference event 136 of 1,213 (a churn bed-swap peer), 1,213 -> 1,214
events; re-pinned in `ehrt.sim.run-test`. The two clinic-decade corpora
and the adhd run are byte-identical.

**dense-7500** (`demos/scenarios/dense-7500/figures.edn`, re-measured
2026-09-30 at `fecbe43b`):

| cell | events before -> after | messages before -> after |
|---|---|---|
| `config.edn` 7,500 | 167,197 -> 167,715 | 222,819 -> 176,971 |
| `config-nobed.edn` 7,500 | 125,642 -> 125,700 | 165,466 -> 134,886 |
| `config-bare.edn` 7,500 | 100,868 -> 101,120 | 65,457 -> 65,643 |
| `config.edn` 750 | 33,306 -> 33,296 | 40,291 -> 35,926 |
| `config-28024.edn` 28,024 (ground truth) | 668,496 -> 671,589 | — |

**The message drop is chatter, not traffic.** A stay that never closed
kept emitting periodic ADT^A08 to the end of the run: A08 at the
all-keys 7,500 cell went 52,091 -> 5,520, and every other trigger moved
by a few percent. Messages per event no longer climbs from 750 to 7,500
arrivals (1.0790 -> 1.0552, where it was 1.2097 -> 1.3327). The 28,024
cell's log is now 264,570,024 bytes, sha-256 `ee59219e...`; the
pre-rebaseline log (`589600c5...`, reproduced byte for byte downstream)
is kept in `figures.edn` under `:pre-rebaseline` as a valid artifact of
the earlier engine.

**What the closer cannot reach.** `describe`'s
`reinstated-stay-without-closer` reads 0 on seed-202 and the 750 cell,
1 at the all-keys 7,500 cell, 2 at the bare cell and 4 of 227 at
28,024 (224 of 224 downstream before). Every residual was checked: a
churn `:merge` absorbed the subject during the closer's delay window,
and `:merged` is terminal — the run loop takes no further step for the
record and ADR-0179 releases its bed. The measure counts only a later
`:discharge` as a closer; teaching it the merge terminal would be a
describe-report change of its own, not taken here.

### Downstream

Ground truth moves on every churn-bearing corpus: reinstated stays now
close after a delay, and churn's candidate choice is in patient-id
order; schema unchanged (no new kind); retained corpora remain
self-check-valid as pre-rebaseline artifacts; regenerate for the new
bytes.
