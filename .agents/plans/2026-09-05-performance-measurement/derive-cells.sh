#!/usr/bin/env bash
# Derives this directory's measurement-cell configs from
# `demos/scenarios/dense-7500/config.edn`. Run from anywhere:
#
#   .agents/plans/2026-09-05-performance-measurement/derive-cells.sh
#
# Every cell is DERIVED, never independently authored -- the same rule
# `demos/scenarios/dense-7500/README.md` states for its own two
# siblings. Edit `config.edn` and re-run this script; never edit a cell
# file by hand. The script is idempotent and rewrites all eight.
#
# ---------------------------------------------------------------
# WHY THE CELLS ARE INDEXED BY ARRIVAL COUNT AND NOT BY WARD SCALE.
# This directory's first cut (commit `plans: performance-measurement
# cells committed`) crossed three WARD SCALES with the two `:persons`
# variants and held `:persons :count` at the provenance's literal
# 15,000 across the whole decade. Both halves of that were wrong, and
# both were corrected on measured evidence rather than on review:
#
#   THE WARD SCALING WAS SET ASIDE FOR THE WRONG REASON FIRST, and
#   the corrected reason is the one that holds. The first cut read the
#   7,500 census -- peak ward census 30.0% / 22.5% / 27.5% of the x1
#   wards -- as proof that no cell could ever exhaust the ladder. The
#   22,500 census refutes that: Emergency 67.3% and Surgery 72.5%, more
#   than double, because a 5.2-simulated-day run against a
#   `:module-horizon-days` of 1,825 is nowhere near steady state and the
#   module cohort is still accumulating. Nothing halted, so every cell
#   measured is valid -- but the prompt's ward headroom was a real
#   precaution, not a redundant one.
#
#   THE DECADE IS STILL x1, on the argument that never depended on
#   capacity: ward scale is not a free parameter. More beds is a
#   different bed vocabulary, a different `allocate` result and
#   therefore a DIFFERENT LOG -- 167,197 / 167,184 / 167,212 events at
#   x1 / x3 / x9 on otherwise identical inputs. Three points at three
#   facility sizes are three experiments rather than one experiment at
#   three sizes, so ward scale is a SEPARATE axis here, measured at one
#   fixed arrival count where the answer means something.
#
#   HOLDING `:persons` AT 15,000 BROKE THE RUN. The provenance makes
#   that value a RULE -- twice the arrival count -- and says in its own
#   comment what the rule is for: a pool too small "would collide
#   nearly every arrival onto an already-registered person". At 22,500
#   arrivals against 15,000 people the pool is SMALLER than the arrival
#   count, and the run refused after 19m38s with six self-check
#   violations on one patient id (a `:discharge` whose `:location`
#   fails the schema, a second `:admission` over an open encounter, and
#   the encounter-bracket invariants that follow from it). The engine
#   behaved correctly and loudly: `:self-check-failed`, exit 2, no
#   corpus. The CELL was out of contract, and the rule is honoured
#   below.
# ---------------------------------------------------------------
set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
src="$repo_root/demos/scenarios/dense-7500/config.edn"
out="$repo_root/.agents/plans/2026-09-05-performance-measurement"

[ -f "$src" ] || { echo "missing provenance: $src" >&2; exit 2; }

persons_line=' :persons {:count 15000 :years 20}'

# --- the decade: three arrival counts at ward scale x1 ---
#
# `:persons :count` is 2x the arrival count at every point, which is
# the provenance's own rule and not this session's choice. 7,500 is
# therefore the provenance UNCHANGED, and that is the strongest check
# in this script.
for a in 2500 7500 22500; do
  p=$((a * 2))
  sed "s|^${persons_line}\$| :persons {:count ${p} :years 20}|" "$src" > "$out/cell-a$a-persons.edn"
  grep -v '^ :persons {:count [0-9]* :years 20}$' \
    "$out/cell-a$a-persons.edn" > "$out/cell-a$a-nopersons.edn"
done

# --- the facility axis: one arrival count, three ward scales ---
#
# Measured at 7,500 arrivals for all three, so the only thing moving is
# the ward count. x1 is `cell-a7500-persons.edn` itself and is not
# re-emitted under a second name.
for f in 3 9; do
  perl -pe 's/:beds (\d+) :surge-slots (\d+)/":beds ".($1*'"$f"')." :surge-slots ".($2*'"$f"')/e' \
    "$out/cell-a7500-persons.edn" > "$out/cell-a7500-w$f-persons.edn"
done

# --- every rule CHECKED rather than asserted ---
for a in 2500 7500 22500; do
  p=$((a * 2))
  grep -q "^ :persons {:count ${p} :years 20}\$" "$out/cell-a$a-persons.edn" \
    || { echo "cell-a$a-persons.edn: :persons is not ${p}" >&2; exit 1; }
  grep -q '^ :persons ' "$out/cell-a$a-nopersons.edn" \
    && { echo "cell-a$a-nopersons.edn: :persons survived the cut" >&2; exit 1; }
  d=$(( $(wc -l < "$out/cell-a$a-persons.edn") - $(wc -l < "$out/cell-a$a-nopersons.edn") ))
  [ "$d" -eq 1 ] || { echo "cell-a$a-nopersons.edn: $d lines cut, expected 1" >&2; exit 1; }
  n=$(grep -c ':beds [0-9]* :surge-slots [0-9]*' "$out/cell-a$a-persons.edn")
  [ "$n" -eq 4 ] || { echo "cell-a$a-persons.edn: $n ward lines, expected 4" >&2; exit 1; }
done

for f in 3 9; do
  n=$(grep -c ':beds [0-9]* :surge-slots [0-9]*' "$out/cell-a7500-w$f-persons.edn")
  [ "$n" -eq 4 ] || { echo "cell-a7500-w$f-persons.edn: $n ward lines, expected 4" >&2; exit 1; }
  grep -q ":beds $((180 * f)) :surge-slots $((40 * f))" "$out/cell-a7500-w$f-persons.edn" \
    || { echo "cell-a7500-w$f-persons.edn: Emergency did not scale by $f" >&2; exit 1; }
done

# THE STRONGEST CHECK: the 7,500 persons-on cell is the provenance,
# byte for byte. It proves the `:persons` rewrite is a pure
# substitution and that nothing else in this script touches the file.
cmp -s "$src" "$out/cell-a7500-persons.edn" \
  || { echo "cell-a7500-persons.edn differs from its provenance" >&2; exit 1; }

echo "OK: eight cells derived from demos/scenarios/dense-7500/config.edn"
