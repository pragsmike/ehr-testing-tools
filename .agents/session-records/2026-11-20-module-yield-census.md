# Module prescribing yield in an in-log outpatient visit, and how the walked share could scale (measurement only)

2026-11-20. Base `a7f37add`. Ceremony: from the session prompt
(`.agents/prompts/2026-11-20-module-yield-census.md`). Measurement
only: no source, config, schema, test or consumer-doc change; this
record is the output. No local `make test` owed (AGENTS.md 22).
`bin/preflight` exited 0 with no findings; HEAD matched origin/main and
the tree was clean.

Author ruling, verbatim, 2026-11-20: "go" -- to: "a measurement session
first: 1. For every vendored module, measure in-log outpatient orders
per walk at dense's horizon, split chronic and acute. 2. Report how the
weighted assignment form chooses walkers. 3. Propose two or three
candidate mixes with their projected fills at 28k, without choosing."

Nothing here chooses a mix or a mechanism.

## The answers

1. **Three modules carry nearly all of the yield, and all three are
   already listed.** `bronchitis` 0.215, `sore_throat` 0.103 and
   `sinusitis` 0.086 in-log outpatient orders per walk. The other four
   of the current seven write none in an outpatient visit. Outside the
   seven, `ear_infections` (0.051), `urinary_tract_infections` (0.035)
   and `osteoporosis` (0.031, all chronic) are the only modules above
   0.01 without seeding.
2. **The walked share is thin for two reasons the prompt did not
   name.** A walker arrival that binds to a person already registered
   walks nothing: 197 of the 937 walker arrivals at 7,500, so 740 real
   walks. And of those 740, a third to all (by module) produce only
   history: the walk's first visit after registration falls past the
   horizon or never comes.
3. **No mechanism in the tree makes the walked share follow
   `--patients`.** The enumeration is the only form that runs and keeps
   walkers and admitted patients apart by ordinal. A weighted
   `module-only` pathway beside a weighted module pool is accepted by
   the config check and then fails the run's own self-check (measured).
4. **At one walker in two, the best acute mix projects about 1,080
   community fills at 28,024 against about 10,400 inpatient dispenses,
   a ratio of 0.10**; the current seven at one in eight project 159
   against 18,215, a ratio of 0.009. The whole three-by-three table is
   in step 3.

## Method

One config per top-level vendored module, each
`demos/scenarios/dense-7500/config.edn` with only the module changed:
`:modules` becomes `["<m>"]` and each of the 937 `:module-id` rows
names `<m>` (`gen.py`). Every other key, the 937 walker ordinals, the
person pool and `:module-horizon-days 1825` are dense's own. Each was
run by the gated 7,500 cell's command, as
`bin/ehrt sim run --seed 20260824 --patients 7500 --churn --config <cfg> --format ground-truth`.
All 31 exited 0 (the run's self-check passed in every one). A patient's
`:patient` stream draws its module first and its pathway, churn,
persona and walk after (`components/sim-engine/src/ehrt/sim_engine/run.clj`
1177-1181), and an explicit row consumes the same one draw whatever it
names (`assignment.clj` 140-147), so each walker keeps the persona it
has in the shipped cell.

That gives 937 walker arrivals per module, of which **740 are walks**.
The run manifest of the shipped 7,500 cell marks 197 of the 937 as
`:repeat? true`: the arrival bound to a person an earlier arrival had
already registered, and a repeat arrival queues only its pathway's
steps, which for `module-only` are none (`run.clj` 596-617). The 740
are exactly the walker ordinals whose own patient id is registered in
the log, so the figure is the same in all 31 runs. Rates below are per
walk, over 740. Over the 937 arrivals they are 0.79 of that.

In-log counts iterate the top-level vector; history is read from each
walker's `:registered` -> `:pre-horizon-facts`. No nested `:event` is
read. Chronic and administration are joined from the module JSON by
citation, as the 2026-11-17 dispense census did; the class map is the
same 134 entries. An order's encounter class is the kind of the
top-level event that opened its `:encounter-id`.

## Step 1: yield per module

740 walks each. "Visit" is the walk's one in-log encounter by the kind
that opened it; no walk had both. "Fills" are the `:medication-dispense`
events naming a module-cited order. "Chronic open at horizon" is a
pre-horizon `chronic: true` order no pre-horizon end and no in-log end
closed.

| module | walks | with in-log event | visit: outpatient / admission / none | outpatient orders: chronic / admin / other | per walk | admission orders: chronic / admin / other | fills: community / inpatient | pre-horizon orders: chronic / admin / other | per walk | chronic open at horizon: orders (walks) | cited in-log events per walk |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `bronchitis` | 740 | 181 | 181 / 0 / 559 | 0 / 0 / 159 | 0.215 | 0 / 0 / 0 | 159 / 0 | 0 / 0 / 1705 | 2.304 | 0 (0) | 1.092 |
| `sore_throat` | 740 | 209 | 209 / 0 / 531 | 0 / 0 / 76 | 0.103 | 0 / 0 / 0 | 76 / 0 | 0 / 0 / 1166 | 1.576 | 0 (0) | 1.078 |
| `sinusitis` | 740 | 328 | 328 / 0 / 412 | 0 / 0 / 64 | 0.086 | 0 / 0 / 0 | 64 / 0 | 0 / 0 / 808 | 1.092 | 0 (0) | 0.973 |
| `ear_infections` | 740 | 26 | 26 / 0 / 714 | 0 / 0 / 38 | 0.051 | 0 / 0 / 0 | 38 / 0 | 0 / 0 / 1714 | 2.316 | 0 (0) | 0.124 |
| `urinary_tract_infections` | 740 | 38 | 30 / 7 / 703 | 0 / 0 / 26 | 0.035 | 0 / 0 / 7 | 26 / 7 | 0 / 3 / 443 | 0.603 | 0 (0) | 0.661 |
| `osteoporosis` | 740 | 29 | 29 / 0 / 711 | 23 / 0 / 0 | 0.031 | 0 / 0 / 0 | 23 / 0 | 50 / 0 / 0 | 0.068 | 50 (50) | 0.164 |
| `attention_deficit_disorder` | 740 | 11 | 11 / 0 / 729 | 0 / 0 / 4 | 0.005 | 0 / 0 / 0 | 4 / 0 | 0 / 0 / 14 | 0.019 | 0 (0) | 0.050 |
| `osteoarthritis` | 740 | 4 | 4 / 0 / 736 | 0 / 0 / 4 | 0.005 | 0 / 0 / 0 | 4 / 0 | 0 / 0 / 84 | 0.114 | 0 (0) | 0.027 |
| `asthma` | 740 | 14 | 11 / 3 / 726 | 2 / 0 / 0 | 0.003 | 0 / 0 / 0 | 2 / 0 | 30 / 0 / 0 | 0.041 | 26 (13) | 0.043 |
| `dementia` | 740 | 2 | 2 / 0 / 738 | 0 / 0 / 2 | 0.003 | 0 / 0 / 0 | 2 / 0 | 0 / 0 / 4 | 0.005 | 0 (0) | 0.014 |
| `allergic_rhinitis` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 17 | 0.023 | 0 (0) | 0.000 |
| `anemia___unknown_etiology` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `appendicitis` | 740 | 3 | 0 / 3 / 737 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.008 |
| `colorectal_cancer` | 740 | 198 | 198 / 0 / 542 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 84 / 4 | 0.119 | 0 (0) | 0.905 |
| `dermatitis` | 740 | 4 | 2 / 0 / 738 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 20 | 0.027 | 0 (0) | 0.011 |
| `fibromyalgia` | 740 | 2 | 0 / 2 / 738 | 0 / 0 / 0 | 0.000 | 0 / 0 / 2 | 0 / 0 | 0 / 0 / 5 | 0.007 | 0 (0) | 0.008 |
| `hypothyroidism` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 4 | 0.005 | 0 (0) | 0.000 |
| `injuries` | 740 | 225 | 6 / 218 / 516 | 0 / 0 / 0 | 0.000 | 0 / 0 / 215 | 0 / 0 | 0 / 0 / 2266 | 3.062 | 0 (0) | 1.541 |
| `med_rec` | 740 | 722 | 722 / 0 / 18 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 2.399 |
| `metabolic_syndrome_care` | 740 | 718 | 718 / 0 / 22 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 1.941 |
| `rheumatoid_arthritis` | 740 | 1 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 6 | 0.008 | 0 (0) | 0.004 |
| `sepsis` | 740 | 2 | 0 / 2 / 738 | 0 / 0 / 0 | 0.000 | 0 / 5 / 0 | 0 / 5 | 0 / 73 / 0 | 0.099 | 0 (0) | 0.036 |
| `sleep_apnea` | 740 | 38 | 38 / 0 / 702 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.147 |
| `total_joint_replacement` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `veteran_lung_cancer` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `veteran_prostate_cancer` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `veteran_ptsd` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `veteran_self_harm` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `veteran_substance_abuse_treatment` | 740 | 620 | 620 / 0 / 120 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 1.654 |
| `vhd_pulmonic` | 740 | 4 | 4 / 0 / 736 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.011 |
| `vhd_tricuspid` | 740 | 4 | 4 / 0 / 736 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.011 |

Reading it:

- **A walk puts at most one visit in the log, and often none.** `none`
  in the visit column is a walk whose content is all history. `med_rec`
  and `metabolic_syndrome_care` reach an in-log visit in 97% of walks
  and prescribe in none of them; `bronchitis` reaches one in 24% and
  prescribes in 88% of those.
- **Every outpatient order was filled once**, community setting, in
  every run: the fills column equals the outpatient orders, chronic
  included.
- **Module orders written in an admission are filled unevenly.**
  `urinary_tract_infections` 7 of 7 and `sepsis` 5 of 5 got an
  inpatient dispense; `injuries` 0 of 215 and `fibromyalgia` 0 of 2.
  Not traced; ADR-0189's rule (the order must still be active and the
  stay open when the turnaround elapses) would explain a module that
  orders at discharge, and that was not checked.
- **Six modules emit nothing at all in dense**, not a history fact and
  not an event: `anemia___unknown_etiology`, `total_joint_replacement`,
  `veteran_lung_cancer`, `veteran_prostate_cancer`, `veteran_ptsd`,
  `veteran_self_harm`. Their six logs are byte-identical to each other
  (66,598,193 bytes).
- **Chronic orders in the log come from `osteoporosis` (23) and
  `asthma` (2) only.** Open chronic history: `osteoporosis` 50 orders
  on 50 walks, `asthma` 26 on 13.
- `urinary_tract_infections` left 41 pre-horizon ends this script could
  not pair with an order; none is chronic, so no column above depends
  on them.

**Reconciliation with the shipped 7,500 cell.** That cell holds 48
in-log outpatient orders: `bronchitis` 26, `sore_throat` 13,
`sinusitis` 9, and 48 community fills. Each all-`<m>` run, cut to the
ordinals `<m>` holds in `config.edn`, gives `bronchitis` 26,
`sinusitis` 9, `sore_throat` 12, and 0 for the other four. The one
missing order is ordinal 1128: in the all-`sore_throat` run a churn
merge absorbs that patient 14 days in, before the visit; in the
shipped cell no merge touches it. Changing the module mix moves which
patients merge.

**Why 59 fills at 28,024 and 48 at 7,500 from the same 937 rows.** At
28,024 the walker ordinals (all below 7,500) are early in a larger
person pool and fewer of them are repeats: 892 walker patients register
against 740. 48/740 = 0.065 and 59/892 = 0.066 per walk. The README's
"937 (892 subjects)" is this figure.

### Modules whose yield depends on an attribute no vendored module sets

`attrs.py` lists, per top-level module, the attributes its closure
reads in a condition that no vendored module writes. Attributes are
namespaced per module (`:veteran-ptsd/veteran`), so a write in another
module would not reach them either.

| module | attributes read, written by no vendored module |
|---|---|
| `allergic_rhinitis`, `dermatitis` | `atopic` |
| `asthma` | `atopic`, `smoker` |
| `bronchitis` | `smoker` |
| `anemia___unknown_etiology`, `hypothyroidism` | `pregnant` |
| `colorectal_cancer` | `pregnant`, `smoker` |
| `metabolic_syndrome_care` | `ckd`, `diabetes`, `microalbuminuria`, `nephropathy`, `neuropathy`, `obesity`, `prediabetes`, `pregnant`, `proteinuria` (and `hypertension`, written only outside its closure) |
| `osteoarthritis`, `veteran_prostate_cancer` | `veteran` |
| `sleep_apnea` | `chf`, `coronary_heart_disease` |
| `urinary_tract_infections` | `GU_pregnancy_check`, `UTI_ambulatory`, `ckd`, `diabetes`, `pregnancy`, `pregnant` |
| `veteran_lung_cancer` | `smoker`, `veteran` |
| `veteran_ptsd` | `mTBI`, `veteran` |
| `veteran_self_harm` | `mdd`, `opioid_addiction`, `veteran` (and `ptsd`, outside its closure) |
| `veteran_substance_abuse_treatment` | `opioid_addiction` |
| `injuries` | `osteoporosis`, written only outside its closure |
| `total_joint_replacement` | `joint_replacement`, written only outside its closure |

Ten were run again with one attribute seeded through
`:module-initial-attributes`, the mechanism clinic-decade uses
(`demos/scenarios/clinic-decade/config.edn` 31). Seed maps:
`{:asthma/atopic true}`, `{:allergic-rhinitis/atopic true}`,
`{:dermatitis/atopic true}`, `{:osteoarthritis/veteran true}`,
`{:total-joint-replacement/joint-replacement "knee"}`, and
`{:<module>/veteran true}` for the five veteran modules.

| module | walks | with in-log event | visit: outpatient / admission / none | outpatient orders: chronic / admin / other | per walk | admission orders: chronic / admin / other | fills: community / inpatient | pre-horizon orders: chronic / admin / other | per walk | chronic open at horizon: orders (walks) | cited in-log events per walk |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `seeded:asthma` | 740 | 274 | 215 / 59 / 466 | 18 / 0 / 0 | 0.024 | 0 / 0 / 0 | 18 / 0 | 626 / 0 / 0 | 0.846 | 556 (278) | 0.789 |
| `seeded:dermatitis` | 740 | 36 | 19 / 0 / 721 | 0 / 0 / 18 | 0.024 | 0 / 0 / 0 | 18 / 0 | 0 / 0 / 405 | 0.547 | 0 (0) | 0.103 |
| `seeded:osteoarthritis` | 740 | 4 | 4 / 0 / 736 | 0 / 0 / 4 | 0.005 | 0 / 0 / 0 | 4 / 0 | 0 / 0 / 84 | 0.114 | 0 (0) | 0.027 |
| `seeded:allergic_rhinitis` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 17 | 0.023 | 0 (0) | 0.000 |
| `seeded:total_joint_replacement` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `seeded:veteran_lung_cancer` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `seeded:veteran_prostate_cancer` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `seeded:veteran_ptsd` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `seeded:veteran_self_harm` | 740 | 0 | 0 / 0 / 740 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 0.000 |
| `seeded:veteran_substance_abuse_treatment` | 740 | 620 | 620 / 0 / 120 | 0 / 0 / 0 | 0.000 | 0 / 0 / 0 | 0 / 0 | 0 / 0 / 0 | 0.000 | 0 (0) | 1.654 |

- **`asthma` seeded atopic** goes from 2 to 18 in-log chronic orders
  and from 26 to 556 open chronic history orders, on 278 of 740 walks.
  It also opens 59 in-log admissions.
- **`dermatitis` seeded atopic** goes from 0 to 18 outpatient orders.
- **Seven seeds changed nothing.** `allergic_rhinitis`,
  `osteoarthritis`, `total_joint_replacement`, `veteran_substance_abuse_treatment`
  and the four silent veteran modules gave byte-identical logs seeded
  and unseeded. For the silent five that means seeding `veteran` (or
  `joint_replacement`) alone is not what they lack in dense; the
  oracle's roots seed the same key
  (`components/oracle/src/ehrt/oracle/digest.clj` 576-607) at their own
  configs. Why was not traced.
- `smoker`, `pregnant`, `diabetes` and the rest were not seeded.

## Step 2: assignment mechanics

**How an ordinal comes to walk a module.** Before the run loop, for
each arrival ordinal, `module-for` calls
`ehrt.sim-engine.assignment/assign-module`
(`components/sim-engine/src/ehrt/sim_engine/run.clj` 550-552). That
function (`assignment.clj` 127-147) always draws once, then:

- **explicit form**: a `{:patient-ordinal i :module-id m}` row naming
  this ordinal gives `m`;
- **weighted form**: otherwise, if the vector holds any
  `{:module-id m :weight w}` member, a cumulative-weight pick among
  them (`weighted-pick`, 75-93). Every ordinal no explicit row covers
  gets a pool member. There is no weight for "no module";
- neither: `nil`, no module.

The id is then looked up among the closures `:modules` loaded
(`run.clj` 550). A pathway is assigned the same way and independently,
by a second draw on the same stream (`assign-pathway`, 95-120). The two
draws know nothing of each other.

**Why pathway and module exclude each other.** One patient ordinal
given both an encounter-opening pathway and a module would open two
encounters, which the project's single-encounter horizon forbids
(sim/ADR-0007; `components/sim/src/ehrt/sim/run.clj` 641-648).
`incompatible-assignments` (`run.clj` 280-304) rejects a config before
the engine starts, as `:rejected :incompatible-assignment`, for every
ordinal CERTAIN to get both: certain of a module when an explicit row
names it or any pool exists (267-278); certain of an encounter-opening
pathway when its explicit row has one or EVERY weighted pathway has one
(244-265). A pool mixing empty and admission-bearing pathways is not
reported, because it cannot be decided without the draw.

Two things the tree does that qualify "may not hold both":

- The rule is per ORDINAL. Per PATIENT it already fails to hold in the
  shipped cell: with `:encounters` on, a repeat arrival queues its
  pathway's steps onto the patient its person already is (`run.clj`
  596-617). At 7,500, **64 patients hold both an authored stay and
  in-log module events**, and 111 hold both a stay and module history.
  The run is green.
- The check reports only certain conflicts, so an uncertain one reaches
  the engine (variant (a) below).

**What fixes dense's walked share at one in eight.** Two enumerations
in `demos/scenarios/dense-7500/config.edn`, 937 rows each over the same
ordinals 8, 16, ... 7,496: `:pathways` rows 201-1137 pin them to
`{:name "module-only" :steps []}`, and `:module-assignment` rows
1144-2080 give each a module, the seven in rotation. Neither vector
holds a weighted module member, so every other ordinal gets no module
and one of the three weighted pathways (153, 169, 193). Nothing past
ordinal 7,496 is named, so above 7,500 arrivals the share falls: 937 of
28,024 is 3.3%.

**Ways the walked share could follow `--patients`.** Not chosen among.
Three were run at 750 arrivals with dense's own seed and `--churn`
(`mech/gen.py`).

| option | mechanism it needs | exists? |
|---|---|---|
| (a) a weighted `module-only` pathway (weight 1 in 8) beside a weighted module pool | the pathway draw and the module draw to be COUPLED: a module only for an ordinal that drew `module-only` | **No.** Both forms exist (`pathway.clj` 317-328, `gmf.clj` 2015-2018) and the config check accepts the pair (`run.clj` 264: the pathway pool is not all admission-bearing). The draws are independent (`assignment.clj` 95-147), so 7 in 8 walkers also draw an admission pathway. **Measured: exit 2, `:self-check-failed`**, `:admission-only-when-no-open-encounter` on one patient. |
| (b) as (a), plus a pool member naming a module absent from `:modules`, at 7 times the real members' weight, as a "no module" weight | the same coupling; and a sanctioned no-module member | **No.** The lookup at `run.clj` 550-552 returns nil for an unlisted id, so this ran green at 750 (34,137 events). It is not a mechanism: nothing validates or documents it, the draws are still uncoupled, and 35 of the 52 patients with in-log module events also held an authored stay. Green at one size is not green at all sizes; (a) fails on the same collision. |
| (c) a weighted module pool alone, the clinic-decade form | every pathway to be empty | **Exists, and does not fit dense.** `clinic-decade/config.edn` 14-30 pairs it with one empty `:pathway`, so every arrival walks. With dense's three admission pathways it is rejected for every ordinal: **measured, exit 1, `:incompatible-assignment`**. |
| (d) a period or fraction key (`every eighth ordinal walks`) | a config key the assignment reads | **No.** `ModuleAssignment` and `PathwayAssignment` are closed two-form schemas (`gmf.clj` 2015-2018, `pathway.clj` 326-328); a search of `components`, `bases`, `demos`, `docs` and `bin` for a period, fraction or share key found only the README's prose. |
| (e) regenerating the enumeration to the arrival count | a generator, and a rule for the sibling | **Mechanism exists, generator does not.** The explicit form runs today at any length. No script in the tree writes the 1,874 rows; the README derives the three siblings by `sed` (86-93) and says the cohort "is ENUMERATED, not a rule" (172). `config-28024.edn` is defined as `config.edn` with one line changed (README 29, 93), so a 28,024-arrival enumeration (3,503 rows in each vector) either goes in `config.edn` too or breaks that rule. Each lookup scans the vector (`assignment.clj` 117, 142), linear in rows per ordinal. |
| (f) a pathway that names its own module, one draw | a `:module` key on a pathway or a pathway key on a module member | **No.** `Pathway` is `{:name :steps}` (`pathway.clj` 305-308). This is the coupling (a) lacks, stated as a schema change. |
| (g) a larger person pool | none new | **Exists, and raises walks, not share.** `:persons {:count N}` sets how many walker arrivals are repeats: 740 of 937 register at 7,500 with 15,000 persons, 892 of 937 at 28,024 with 56,048. |

## Step 3: candidate mixes

Arithmetic from step 1 only, equal weights within a mix, unseeded rows
unless said. Per walk (over 740):

| mix | modules | acute outpatient orders | chronic outpatient orders | fills | open chronic history orders |
|---|---|---|---|---|---|
| A, the current seven | `sore_throat`, `med_rec`, `injuries`, `bronchitis`, `metabolic_syndrome_care`, `veteran_substance_abuse_treatment`, `sinusitis` | (76+0+0+159+0+0+64)/7/740 = 0.0577 | 0 | 0.0577 | 0 |
| B, highest acute yield | `bronchitis`, `sore_throat`, `sinusitis`, `ear_infections`, `urinary_tract_infections` | (159+76+64+38+26)/5/740 = 0.0981 | 0 | 0.0981 | 0 |
| C, B plus chronic | B's five, `osteoporosis`, `asthma` | 363/7/740 = 0.0701 | (23+2)/7/740 = 0.0048 | 0.0749 | (50+26)/7/740 = 0.0147 |
| C with `asthma` seeded atopic | as C | 0.0701 | (23+18)/7/740 = 0.0079 | 0.0780 | (50+556)/7/740 = 0.1170 |

B's cut is the five unseeded modules above 0.03 acute orders per walk;
the next is `attention_deficit_disorder` at 0.005. Mix A's 0.0577 is
the equal-weight expectation; the shipped cell's own ordinals gave
48/740 = 0.0649.

Scaling constants, all read from the shipped 28,024 cell
(`stream28.clj`): 22,091 of 28,024 arrivals register their own patient
(0.7883), which is taken as the fraction of walker arrivals that walk
once walkers are spread over the whole run; 20,121 inpatient dispenses
from 27,087 non-walker arrivals (0.7428 each); 25.14 events per
non-walker arrival; 12.03 events per walker patient under mix A, moved
for B and C by the difference in step 1's walker events per walk.

Projected at 28,024 arrivals:

| mix | walked share | walker arrivals | walks | `:community` fills | in-log chronic orders | open pre-horizon chronic orders | inpatient dispenses | community : inpatient | events (change from 691,758) |
|---|---|---|---|---|---|---|---|---|---|
| A | 1 in 8 | 3,503 | 2,761 | 159 | 0 | 0 | 18,215 | 0.009 | 649,732 (-42,026) |
| A | 1 in 4 | 7,006 | 5,523 | 319 | 0 | 0 | 15,613 | 0.020 | 594,879 (-96,879) |
| A | 1 in 2 | 14,012 | 11,046 | 638 | 0 | 0 | 10,409 | 0.061 | 485,173 (-206,585) |
| B | 1 in 8 | 3,503 | 2,761 | 271 | 0 | 0 | 18,215 | 0.015 | 648,467 (-43,291) |
| B | 1 in 4 | 7,006 | 5,523 | 542 | 0 | 0 | 15,613 | 0.035 | 592,349 (-99,409) |
| B | 1 in 2 | 14,012 | 11,046 | 1,084 | 0 | 0 | 10,409 | 0.104 | 480,113 (-211,645) |
| C | 1 in 8 | 3,503 | 2,761 | 207 | 13 | 41 | 18,215 | 0.011 | 647,999 (-43,759) |
| C | 1 in 4 | 7,006 | 5,523 | 414 | 27 | 81 | 15,613 | 0.027 | 591,412 (-100,346) |
| C | 1 in 2 | 14,012 | 11,046 | 827 | 53 | 162 | 10,409 | 0.079 | 478,239 (-213,519) |

With `asthma` seeded atopic, C's rows become 215 / 431 / 861 fills, 22
/ 44 / 87 in-log chronic orders and 323 / 646 / 1,292 open chronic
history orders.

What the table does not carry:

- **Events fall in every cell.** A walker patient is about 12 events
  and an admitted one about 25, so "added events" is negative: raising
  the walked share thins the log.
- The projection is linear. It has no term for merges, for the bed
  pressure fewer admissions relieve, or for the reshuffle any config
  change brings; step 1's one-order miss at ordinal 1128 is the size of
  that effect at 937 walkers.
- Inpatient dispenses from module admissions are left out (`urinary_tract_infections`
  7 per 740 walks; about 20 for B at one in two).
- The 0.7883 walk fraction is the shipped pool's (56,048 persons). A
  different `:persons` count moves every row.
- Fills are one per outpatient order under the shipped rule. A refill
  rule would act on the chronic columns only.

## Where the tree differed from the prompt

1. **"937 walks yield 48 in-log outpatient orders."** 937 walker
   arrivals; 740 walks. 197 are repeat arrivals that walk nothing.
2. **"An ordinal may not hold both an admission-bearing pathway and a
   module."** True per ordinal and only where certain. Per patient the
   shipped cell already holds 64 with both, through repeat arrivals.
3. **"The weighted form as it stands cannot simply replace the
   enumeration."** Held, and more strongly than stated: the pair is
   not rejected up front, it fails the self-check (a), or passes at one
   size with the cohorts mixed (b).
4. **"A walk contributes at most one visit to the log, so a module's
   yield is a property of that one visit."** At most one held in all 41
   runs. But yield is first a property of whether the walk reaches any
   in-log visit, which runs from 0% to 97% by module, and of whether
   the walker arrival walks at all.
5. **"(C) B plus the modules that put chronic orders in the log or
   leave them open at the horizon."** Unseeded that is two modules,
   `osteoporosis` and `asthma`, and `asthma`'s contribution is 2 orders
   in 740 walks.
6. **"The seven current modules' totals reconcile with the 7,500
   cell."** 47 of 48; the one is a merge (step 1).
7. "31 top-level module files" and "chronic must be joined from the
   module JSON by citation" both held.
8. **"Added events"** is a loss in every cell (step 3).

## Findings

- Module orders written in an admission by `injuries` (215) and
  `fibromyalgia` (2) get no inpatient dispense. Not traced.
- Seeding `veteran` changes nothing for four veteran modules in dense,
  though the oracle's roots seed the same key. Not traced.
- A pool member naming a module absent from `:modules` is accepted and
  silently means "no module" (variant (b)). Nothing validates it.

## Scripts

Run from the workspace root; `$S` is a scratch directory outside the
tree. Kept here whole; they live nowhere else in the tree. Order:
`python3 -I gen.py $S/cfg`; `runall.sh`; `clojure -J-Xmx5g -M:dev
yield.clj "" $S/logs/*.edn > all.edn`; `clojure -M table.clj all.edn`;
`clojure -M project.clj all.edn seeded.edn`. The seeded configs are
`gen.py`'s with one `:module-initial-attributes` line added above
`:module-horizon-days`.

### gen.py

```python
# python3 -I gen.py OUTDIR : one config per top-level vendored module, each
# dense-7500's config.edn with ONLY the module named changed: the :modules
# vector becomes ["<m>"] and every :module-id "<x>" becomes "<m>". The 937
# walker ordinals, the pathways, facility, persons and horizon are untouched.
import re, sys, pathlib
out = pathlib.Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
src = pathlib.Path("demos/scenarios/dense-7500/config.edn").read_text()
mods = sorted(p.stem for p in pathlib.Path("components/sim/resources/sim/modules").glob("*.json"))
pat = re.compile(r':modules \[[^\]]*\]')
assert len(pat.findall(src)) == 1
n_ids = len(re.findall(r':module-id "[^"]+"', src))
for m in mods:
    t = pat.sub(':modules ["%s"]' % m, src)
    t = re.sub(r':module-id "[^"]+"', ':module-id "%s"' % m, t)
    (out / (m + ".edn")).write_text(t)
print(len(mods), "configs;", n_ids, "module-id rows each")
```

### runall.sh

```bash
#!/usr/bin/env bash
# runall.sh : every generated config through the scenario's own command
# (seed, arrival count and --churn as the gated 7,500 cell), four at a time.
cd "$(git rev-parse --show-toplevel)" || exit 2
S=$SCRATCH
one() { m="$1"
  bin/ehrt sim run --seed 20260824 --patients 7500 --churn --config "$S/cfg/$m.edn" --format ground-truth > "$S/logs/$m.edn" 2> "$S/logs/$m.err"
  echo "$m exit=$? bytes=$(stat -c %s "$S/logs/$m.edn") sha=$(sha256sum "$S/logs/$m.edn" | cut -c1-16)" >> "$S/runs.txt"; }
export -f one; export S
: > "$S/runs.txt"
ls "$S/cfg" | sed 's/\.edn$//' | xargs -P 4 -I{} bash -c 'one {}'
echo RUNS_DONE
```

### yield.clj

```clojure
;; clojure -J-Xmx6g -M:dev yield.clj LABEL LOG.edn [LOG.edn ...]
;; One EDN map per log on stdout. In-log counts iterate the TOP-LEVEL
;; vector only; history is read from :registered -> :pre-horizon-facts
;; only. No nested :event is ever read.
(require '[ehrt.patient-simulator.gmf :as gmf]
         '[clojure.java.io :as io] '[clojure.string :as str] '[clojure.edn :as edn])

(def root (io/file "components/sim/resources/sim/modules"))
(def modules
  (for [^java.io.File f (sort (file-seq root))
        :when (str/ends-with? (.getName f) ".json")
        :let [id (-> (.getPath f) (subs (inc (count (.getPath root)))) (str/replace #"\.json$" ""))]]
    [id (gmf/load-module id (slurp f))]))
(def states (for [[id r] modules, [k s] (:states (:payload r))] [[id k] s]))
(def class-map
  (into {} (for [[k s] states :when (= :medication-order (:type s))]
             [k (cond (true? (:chronic s)) :chronic
                      (true? (:administration s)) :administration
                      :else :other)])))
(def order-attr (into {} (for [[k s] states :when (= :medication-order (:type s))] [k (:assign-to-attribute s)])))
(def end-target (into {} (for [[k s] states :when (= :medication-end (:type s))]
                           [k (select-keys s [:medication-order :referenced-by-attribute])])))

(defn ckey [c] [(:module c) (:state c)])
(defn klass [c] (class-map (ckey c) :unmatched))
(defn authored? [c] (str/starts-with? (str (:module c)) "dense-"))
(defn subject [e] (:patient-id (first (:participants e))))
(defn ordinal [pid] (when pid (some-> (re-find #"^PID-(\d+)-" pid) second Long/parseLong)))
(defn walker? [o] (and o (zero? (mod o 8)) (<= 8 o 7496)))
(defn freq [xs] (into (sorted-map) (frequencies xs)))

(defn census [label path]
  (let [log (edn/read-string {:default (fn [_ v] v)} (slurp path))
        idx (map-indexed vector log)
        ;; a walk is a walker ordinal; it REGISTERED when its own pid opens
        regs (for [[i e] idx :when (and (= :registered (:event e)) (walker? (ordinal (subject e))))] e)
        reg-ords (set (map (comp ordinal subject) regs))
        ;; module-authored top-level events: a citation that is not an authored pathway's
        cited (for [[i e] idx :let [c (:citation e)] :when (and c (not (authored? c)))]
                (assoc e :i i :ord (ordinal (subject e))))
        visits (filter #(#{:outpatient-visit :admission} (:event %)) cited)
        enc-kind (into {} (for [[i e] idx :when (#{:outpatient-visit :admission} (:event e))] [(:encounter-id e) (:event e)]))
        visit-class (reduce (fn [m v] (update m (:ord v) (fnil conj #{}) (:event v))) {} visits)
        orders (for [e cited :when (= :medication-order (:event e))]
                 {:i (:i e) :ord (:ord e) :c (ckey (:citation e)) :k (klass (:citation e))
                  :enc (get enc-kind (:encounter-id e) :no-encounter)})
        disp (for [[i e] idx :when (and (= :medication-dispense (:event e))
                                        (:order-citation e) (not (authored? (:order-citation e))))]
               {:setting (:setting e) :ord (ordinal (subject e)) :k (klass (:order-citation e))})
        ends (for [[i e] idx :when (and (= :medication-end (:event e)) (:citation e) (not (authored? (:citation e))))] e)
        ;; every top-level event whose first participant is a walker
        walker-events (count (for [[i e] idx :when (walker? (ordinal (subject e)))] 1))
        ;; history: each :registered's facts, walked in order
        hist (for [e regs]
               (loop [fs (:pre-horizon-facts e) open [] n-orders [] unres 0]
                 (if-let [f (first fs)]
                   (case (:event f)
                     :medication-order
                     (let [o {:ord (ordinal (subject e)) :subj (subject e) :c (ckey (:citation f)) :k (klass (:citation f))}]
                       (recur (rest fs) (conj open o) (conj n-orders o) unres))
                     :medication-end
                     (let [{mo :medication-order attr :referenced-by-attribute} (end-target (ckey (:citation f)))
                           hit? (fn [o] (or (and mo (= (:c o) [(:module (:citation f)) mo]))
                                            (and attr (= attr (order-attr (:c o))))))
                           pos (last (keep-indexed (fn [p o] (when (hit? o) p)) open))]
                       (if pos
                         (recur (rest fs) (into (subvec open 0 pos) (subvec open (inc pos))) n-orders unres)
                         (recur (rest fs) open n-orders (inc unres))))
                     (recur (rest fs) open n-orders unres))
                   {:open open :orders n-orders :unres unres :facts (count (:pre-horizon-facts e))})))
        pre-orders (mapcat :orders hist)
        pre-open0 (vec (mapcat :open hist))
        ;; an in-log end naming no log order closes the latest open history order of that subject and citation
        pre-open (reduce (fn [open e]
                           (let [c (ckey (:order-citation e)) s (subject e)
                                 pos (last (keep-indexed (fn [p o] (when (and (= s (:subj o)) (= c (:c o))) p)) open))]
                             (if pos (into (subvec open 0 pos) (subvec open (inc pos))) open)))
                         pre-open0 (remove :order-event-id ends))]
    (prn {:label label
          :events (count log)
          :kinds-total (select-keys (frequencies (map :event log)) [:registered :admission :outpatient-visit :medication-order :medication-dispense :step-rejected])
          :walks-registered (count regs)
          :walks-with-in-log-event (count (set (keep :ord cited)))
          :in-log-cited-events (count cited)
          :in-log-cited-kinds (freq (map :event cited))
          :visit-class (freq (map (fn [o] (let [s (visit-class o)]
                                             (cond (nil? s) :none (= 1 (count s)) (first s) :else :both)))
                                   reg-ords))
          :visits (freq (map :event visits))
          :orders (count orders)
          :orders-by-enc-and-class (freq (map (juxt :enc :k) orders))
          :orders-by-state (freq (map (juxt :enc :k :c) orders))
          :walks-with-outpatient-order (count (set (map :ord (filter #(= :outpatient-visit (:enc %)) orders))))
          :dispenses (freq (map (juxt :setting :k) disp))
          :in-log-ends (count ends)
          :pre-facts (reduce + (map :facts hist))
          :pre-orders (count pre-orders)
          :pre-orders-by-class (freq (map :k pre-orders))
          :pre-open-at-horizon-by-class (freq (map :k pre-open))
          :pre-open-chronic-by-state (freq (map :c (filter #(= :chronic (:k %)) pre-open)))
          :walks-with-open-chronic (count (set (map :ord (filter #(= :chronic (:k %)) pre-open))))
          :pre-ends-unresolved (reduce + (map :unres hist))
          :walker-events walker-events
          :orders-by-walker-ord (into (sorted-map) (frequencies (map :ord (filter #(= :outpatient-visit (:enc %)) orders))))})))

(let [[label & paths] *command-line-args*]
  (doseq [p paths] (census (str label (str/replace (.getName (io/file p)) #"\.edn$" "")) p)))
```

### table.clj

```clojure
;; clojure -M table.clj OUT.edn ... : the step-1 table from yield.clj's maps,
;; sorted by in-log outpatient orders per walk. A walk is a walker arrival
;; that registered its own patient: 740 of the 937 walker ordinals.
(require '[clojure.edn :as edn] '[clojure.string :as str])
(let [rows (for [p *command-line-args* l (str/split-lines (slurp p)) :when (str/starts-with? l "{")] (edn/read-string l))
      oc (fn [m enc k] (get (:orders-by-enc-and-class m) [enc k] 0))
      out-all (fn [m] (reduce + (for [k [:chronic :administration :other]] (oc m :outpatient-visit k))))
      r (fn [m x] (format "%.3f" (double (/ x (:walks-registered m)))))]
  (println "| module | walks | with in-log event | visit: outpatient / admission / none | outpatient orders: chronic / admin / other | per walk | admission orders: chronic / admin / other | fills: community / inpatient | pre-horizon orders: chronic / admin / other | per walk | chronic open at horizon: orders (walks) | cited in-log events per walk |")
  (println "|---|---|---|---|---|---|---|---|---|---|---|---|")
  (doseq [m (sort-by (fn [m] [(- (out-all m)) (:label m)]) rows)]
    (let [vc (:visit-class m) po (:pre-orders-by-class m) op (:pre-open-at-horizon-by-class m) d (:dispenses m)]
      (assert (zero? (get vc :both 0)))
      (println (str "| `" (:label m) "` | " (:walks-registered m) " | " (:walks-with-in-log-event m)
                    " | " (str/join " / " (map #(get vc % 0) [:outpatient-visit :admission :none]))
                    " | " (str/join " / " (map #(oc m :outpatient-visit %) [:chronic :administration :other]))
                    " | " (r m (out-all m))
                    " | " (str/join " / " (map #(oc m :admission %) [:chronic :administration :other]))
                    " | " (reduce + (for [[[s _] n] d :when (= s :community)] n)) " / " (reduce + (for [[[s _] n] d :when (= s :inpatient)] n))
                    " | " (str/join " / " (map #(get po % 0) [:chronic :administration :other]))
                    " | " (r m (:pre-orders m))
                    " | " (get op :chronic 0) " (" (:walks-with-open-chronic m) ")"
                    " | " (r m (:in-log-cited-events m)) " |")))))
```

### project.clj

```clojure
;; clojure -M project.clj all.edn seeded.edn : (1) the seven current modules restricted to their own real ordinals; (2) the three mixes.
(require '[clojure.edn :as edn] '[clojure.string :as str])
(let [rows (into {} (for [p *command-line-args* l (str/split-lines (slurp p)) :when (str/starts-with? l "{")] (let [m (edn/read-string l)] [(:label m) m])))
      seven ["sore_throat" "med_rec" "injuries" "bronchitis" "metabolic_syndrome_care" "veteran_substance_abuse_treatment" "sinusitis"]
      oc (fn [m enc k] (get (:orders-by-enc-and-class (rows m)) [enc k] 0))
      acute (fn [m] (+ (oc m :outpatient-visit :other) (oc m :outpatient-visit :administration)))
      chron (fn [m] (oc m :outpatient-visit :chronic))
      open (fn [m] (get (:pre-open-at-horizon-by-class (rows m)) :chronic 0))
      wev (fn [m] (:walker-events (rows m)))
      REG 740.0 ARR 28024 W0 937 REG28 892 FIRST28 22091 INP28 20121 EV28 691758 WEV28 10731]
  (println "(1) each current module, the all-<m> run cut to the ordinals <m> holds in config.edn:")
  (doseq [[i m] (map-indexed vector seven)]
    (println " " m (reduce + (for [[o n] (:orders-by-walker-ord (rows m)) :when (= i (mod (dec (quot o 8)) 7))] n))))
  (println "(2) registered fraction at 28,024:" (format "%.4f" (/ FIRST28 (double ARR)))
           "| inpatient dispenses per non-walker arrival" (format "%.4f" (/ INP28 (double (- ARR W0))))
           "| events per non-walker arrival" (format "%.3f" (/ (- EV28 WEV28) (double (- ARR W0))))
           "| walker events per registered walk, mix A, measured at 28,024" (format "%.3f" (/ WEV28 (double REG28))))
  (let [a-wev (/ (reduce + (map wev seven)) 7.0 REG)]
    (doseq [[nm ms] [["A" seven]
                     ["B" ["bronchitis" "sore_throat" "sinusitis" "ear_infections" "urinary_tract_infections"]]
                     ["C" ["bronchitis" "sore_throat" "sinusitis" "ear_infections" "urinary_tract_infections" "osteoporosis" "asthma"]]
                     ["C, asthma seeded" ["bronchitis" "sore_throat" "sinusitis" "ear_infections" "urinary_tract_infections" "osteoporosis" "seeded:asthma"]]]]
      (let [n (count ms) per (fn [f] (/ (reduce + (map f ms)) (double n) REG))
            pa (per acute) pc (per chron) po (per open) pw (per wev)]
        (println (format "mix %s (%d modules, equal weight): per registered walk: acute %.4f chronic %.4f fills %.4f open-chronic %.4f walker-events %.3f (A: %.3f)" nm n pa pc (+ pa pc) po pw a-wev))
        (doseq [[lbl den] [["1 in 8" 8] ["1 in 4" 4] ["1 in 2" 2]]]
          (let [w (quot ARR den) reg (* w (/ FIRST28 (double ARR)))
                fills (* reg (+ pa pc)) inp (* INP28 (/ (- ARR w) (double (- ARR W0))))
                ev (+ (* (/ (- EV28 WEV28) (double (- ARR W0))) (- ARR w))
                      (* reg (+ (/ WEV28 (double REG28)) (- pw a-wev))))]
            (println (format "   %s: walkers %d registered %.0f | community fills %.0f | in-log chronic %.0f | open pre-horizon chronic %.0f | inpatient %.0f | ratio %.4f | events %.0f (%+.0f)"
                             lbl w reg fills (* reg pc) (* reg po) inp (/ fills inp) ev (- ev EV28)))))))))
```

### attrs.py

```python
# python3 -I attrs.py : for each top-level vendored module, the attributes
# its closure READS in a condition that NO vendored module writes, and
# whether :module-initial-attributes in a shipped scenario seeds them.
import json, pathlib, collections
root = pathlib.Path("components/sim/resources/sim/modules")
files = sorted(root.rglob("*.json"))
def mid(f): return str(f.relative_to(root))[:-5]
reads = collections.defaultdict(set); writes = collections.defaultdict(set); calls = collections.defaultdict(set)
def walk_cond(c, m):
    if isinstance(c, dict):
        if c.get("condition_type") == "Attribute" and "attribute" in c: reads[m].add(c["attribute"])
        for v in c.values(): walk_cond(v, m)
    elif isinstance(c, list):
        for v in c: walk_cond(v, m)
for f in files:
    d = json.loads(f.read_text()); m = mid(f)
    for name, s in d.get("states", {}).items():
        t = s.get("type")
        if t in ("SetAttribute", "Counter"): writes[s["attribute"]].add(m)
        if "assign_to_attribute" in s: writes[s["assign_to_attribute"]].add(m)
        if t == "CallSubmodule": calls[m].add(s["submodule"])
        walk_cond(s, m)
def closure(m):
    seen = set(); todo = [m]
    while todo:
        x = todo.pop()
        if x in seen: continue
        seen.add(x); todo += list(calls.get(x, ()))
    return seen
tops = sorted(p.stem for p in root.glob("*.json"))
print("files", len(files), "top-level", len(tops))
for m in tops:
    cl = closure(m)
    rd = set().union(*(reads[x] for x in cl))
    unset = sorted(a for a in rd if not writes.get(a))
    outside = sorted(a for a in rd if writes.get(a) and not (writes[a] & cl))
    print("%-36s closure %2d  reads %2d  written by no vendored module: %s%s" % (
        m, len(cl), len(rd), unset or "-", ("  | written only outside its closure: %s" % outside) if outside else ""))
```

### manifest.clj

```clojure
;; clojure -J-Xmx6g -M manifest.clj MANIFEST.edn EVENTS.edn : the walker ordinals, from the manifest's own assignment record
(require '[clojure.edn :as edn])
(let [[mp ep] *command-line-args*
      asg (:assignments (edn/read-string {:default (fn [_ v] v)} (slurp mp)))
      log (edn/read-string {:default (fn [_ v] v)} (slurp ep))
      subj #(:patient-id (first (:participants %)))
      w (filter :module asg)
      reg (set (map subj (filter #(= :registered (:event %)) log)))
      facts (into {} (for [e log :when (= :registered (:event e))] [(subj e) (count (:pre-horizon-facts e))]))
      cited (set (for [e log :when (and (:citation e) (not (re-find #"^dense-" (str (:module (:citation e))))))] (subj e)))]
  (println "arrivals" (count asg) "| with a module" (count w) "| pathway names of those" (frequencies (map :pathway w))
           "| repeat?" (count (filter :repeat? w)) "| placeholder?" (count (filter :placeholder? w))
           "| distinct patient-ids" (count (set (map :patient-id w))))
  (println "walker patient-ids matching PID-<own ordinal>-" (count (filter #(re-find (re-pattern (format "^PID-%06d-" (:ordinal %))) (:patient-id %)) w)))
  (println "first-arrival walkers" (count (remove :repeat? w)) "| of them :registered in the log" (count (filter #(reg (:patient-id %)) (remove :repeat? w)))
           "| with >0 history facts" (count (filter #(pos? (facts (:patient-id %) 0)) (remove :repeat? w)))
           "| with a cited in-log event" (count (filter #(cited (:patient-id %)) (remove :repeat? w))))
  (println "repeat walkers by ordinal of the patient they resolve to: walker ordinal?" 
           (frequencies (for [a (filter :repeat? w)] (let [o (Long/parseLong (second (re-find #"^PID-(\d+)-" (:patient-id a))))] (and (zero? (mod o 8)) (<= 8 o 7496))))))
  (println "all arrivals: repeat?" (count (filter :repeat? asg)) "| registered events" (count (filter #(= :registered (:event %)) log))
           "| registered subjects that are an arrival's patient-id" (count (filter reg (set (map :patient-id asg))))))
```

### stream28.clj

```clojure
;; clojure -J-Xmx6g -M stream28.clj EVENTS.edn ARRIVALS : streamed; first arrivals, walker registrations, module orders and fills
(require '[clojure.edn :as edn] '[clojure.java.io :as io])
(let [[path n] *command-line-args* n (Long/parseLong n)
      rdr (java.io.PushbackReader. (io/reader path) 65536)
      _ (loop [] (let [c (.read rdr)] (when (not= c (int \[)) (recur))))
      eof (Object.)
      st (atom {:reg-forms {} :first 0 :walker-reg 0 :walker-facts 0 :enc {} :orders {} :disp {} :events 0 :walker-events 0 :visits {}})
      ord (fn [pid] (when pid (some-> (re-find #"^PID-(\d+)-" pid) second Long/parseLong)))
      walker? (fn [o] (and o (zero? (mod o 8)) (<= 8 o 7496)))
      authored? (fn [c] (boolean (re-find #"^dense-" (str (:module c)))))]
  (loop []
    (let [e (try (edn/read {:eof eof :default (fn [_ v] v)} rdr) (catch Exception _ eof))]
      (when-not (identical? e eof)
        (let [k (:event e) pid (:patient-id (first (:participants e))) o (ord pid)]
          (swap! st update :events inc)
          (when (walker? o) (swap! st update :walker-events inc))
          (when (= :registered k)
            (swap! st update-in [:reg-forms (cond (nil? o) (subs (str pid) 0 (min 4 (count (str pid)))) (< o n) :arrival-ordinal :else :beyond-arrivals)] (fnil inc 0))
            (when (walker? o) (swap! st update :walker-reg inc)
              (when (seq (:pre-horizon-facts e)) (swap! st update :walker-facts inc))))
          (when (#{:admission :outpatient-visit} k)
            (swap! st assoc-in [:enc (:encounter-id e)] k)
            (when (and (:citation e) (not (authored? (:citation e)))) (swap! st update-in [:visits [(:module (:citation e)) k]] (fnil inc 0))))
          (when (and (= :medication-order k) (:citation e) (not (authored? (:citation e))))
            (swap! st update-in [:orders [(:module (:citation e)) (get-in @st [:enc (:encounter-id e)])]] (fnil inc 0)))
          (when (and (= :medication-dispense k) (:order-citation e) (not (authored? (:order-citation e))))
            (swap! st update-in [:disp [(:module (:order-citation e)) (:setting e)]] (fnil inc 0)))
          (when (and (= :medication-dispense k) (authored? (:order-citation e)))
            (swap! st update-in [:authored-disp (:setting e)] (fnil inc 0))))
        (recur))))
  (prn (dissoc @st :enc)))
```

### mech/gen.py

```python
# python3 -I gen.py OUTDIR : three weighted variants of dense-7500's config.
import re,pathlib,sys
out=pathlib.Path(sys.argv[1])
src=pathlib.Path("demos/scenarios/dense-7500/config.edn").read_text()
t=re.sub(r'\{:patient-ordinal \d+ :pathway \{:name "module-only" :steps \[\]\}\}','',src)
t=re.sub(r'\{:patient-ordinal \d+ :module-id "[^"]+"\}','',t)
assert ':patient-ordinal' not in t
i=t.index(':weight 20}')+len(':weight 20}')
wp=t[:i]+'\n            {:pathway {:name "module-only" :steps []} :weight 14.2857}'+t[i:]
seven=["sore_throat","med_rec","injuries","bronchitis","metabolic_syndrome_care","veteran_substance_abuse_treatment","sinusitis"]
pool=' '.join('{:module-id "%s" :weight 1}'%m for m in seven)
def put(base,p):
    r=re.sub(r':module-assignment \[\s*\]', ':module-assignment ['+p+']', base); assert r!=base; return r
# a: a weighted module-only pathway (1 in 8) and a weighted pool of the seven
(out/"a-weighted-both.edn").write_text(put(wp,pool))
# b: a, plus a pool member naming a module absent from :modules, at 7 times the seven's weight
(out/"b-null-member.edn").write_text(put(wp,pool+' {:module-id "no-such-module" :weight 49}'))
# c: the weighted pool alone; the three admission pathways keep every ordinal
(out/"c-pool-only.edn").write_text(put(t,pool))
```

### mech/both.clj

```clojure
;; clojure -J-Xmx4g -M both.clj LOG.edn : patients holding module-cited events, and how many of them also hold an authored-pathway stay
(require '[clojure.edn :as edn] '[clojure.string :as str])
(let [log (edn/read-string {:default (fn [_ v] v)} (slurp (first *command-line-args*)))
      subj #(:patient-id (first (:participants %)))
      authored? #(str/starts-with? (str (:module %)) "dense-")
      mod-pids (set (for [e log :when (and (:citation e) (not (authored? (:citation e))))] (subj e)))
      hist-pids (set (for [e log :when (and (= :registered (:event e)) (some #(not (authored? (:citation %))) (:pre-horizon-facts e)))] (subj e)))
      adm (for [e log :when (and (= :admission (:event e)) (not (and (:citation e) (not (authored? (:citation e))))))] (subj e))
      adm-pids (set adm)]
  (println "events" (count log) "| patients with an authored admission" (count adm-pids)
           "| with module history facts" (count hist-pids) "| with in-log module events" (count mod-pids)
           "| history AND authored admission" (count (filter adm-pids hist-pids))
           "| in-log module events AND authored admission" (count (filter adm-pids mod-pids)))
  (println "module in-log kinds on patients who also hold an authored admission:"
           (into (sorted-map) (frequencies (for [e log :when (and (:citation e) (not (authored? (:citation e))) (adm-pids (subj e)))] (:event e))))))
```

## Close

Appended by the close commit.
