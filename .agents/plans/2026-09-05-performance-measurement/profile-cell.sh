#!/usr/bin/env bash
# Flight-records one cell's generate phase and its check phase, and
# aggregates each recording's `jdk.ExecutionSample` events two ways.
#
#   .agents/plans/2026-09-05-performance-measurement/profile-cell.sh \
#       <label> <config> <patients> <out-dir>
#
# ZERO NEW DEPENDENCY, which is why JFR and not an agent profiler: the
# recorder is in the JVM already (`-XX:StartFlightRecording`) and the
# reader is `jfr`, a JDK 21 tool on the same PATH `clojure` is on.
# `bin/ehrt` does not pass `-J` options through, so this calls
# `clojure -M:ehrt` directly -- the same alias that wrapper execs,
# from the same working directory it cds to.
#
# THE TWO AGGREGATIONS ANSWER DIFFERENT QUESTIONS, and the second is
# the one this session was commissioned for.
#
#   SELF TIME, by TOP frame -- where the CPU actually is. It is the
#   honest ranking, and it is also nearly useless for naming a SITE,
#   because a Clojure hot loop's top frame is almost always something
#   like `clojure.lang.RT.seqFrom` that belongs to no site in
#   particular.
#
#   INCLUSIVE SHARE, by ANY frame -- the fraction of samples taken
#   anywhere beneath a named function. This is what answers "does
#   `occupancy-board` still show, and at what share", which is the
#   question `roadmap.md`'s `performance-residual-sites` row asks. The
#   shares do NOT sum to 100%: a sample beneath `occupancy-board`
#   inside `apply-events` is counted under both, which is the point.
set -euo pipefail

label="${1:?usage: profile-cell.sh <label> <config> <patients> <out-dir>}"
config="${2:?}"
patients="${3:?}"
out="${4:?}"

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd -- "$repo_root" || exit 2
raw="$repo_root/.agents/plans/2026-09-05-performance-measurement/raw"
mkdir -p "$out" "$raw"
seed=20260824

# The named sites, in the words the two records name them by. Every
# entry is a REAL symbol, resolved out of the source before it was
# written here (Clojure munges `-` to `_` in class names).
sites="ehrt.sim_engine.fold\$apply_events:apply-events (the one fold both phases run)
ehrt.sim_engine.fold\$replay:replay -- ADR-0169's 14-calls-per-check-all site
ehrt.sim_check.check\$occupancy_within_capacity:occupancy-within-capacity (ADR-0169: 54.9% of check)
ehrt.sim_model.facility\$occupancy_board:occupancy-board (ADR-0169 OUT-list, 8.1% of generate)
ehrt.sim_engine.decide\$waiting_boarder:decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%)
ehrt.sim_engine.log_index\$last_uncancelled_index:last-uncancelled-index (ADR-0169 F-3, 5.9%)
ehrt.sim_engine.evolve:evolve (the per-event patient fold)
ehrt.sim_engine.decide:decide (the generator's dispatch)
ehrt.sim_check.check:sim-check, whole namespace
ehrt.person_simulator:person-simulator (what :persons costs)
ehrt.patient_simulator:patient-simulator (the module cohort's walk)
malli.core:malli (residue after the 642d70a validator hoist)"

report () {  # <jfr> <phase>
  local jfr="$1" phase="$2" txt="$out/$label.$phase.jfrtxt"
  jfr print --events jdk.ExecutionSample "$jfr" > "$txt"
  local total
  total=$(grep -c 'stackTrace = \[' "$txt" || true)
  {
    echo "### $label -- $phase"
    echo
    echo "\`$(basename "$jfr")\`, **$total** \`jdk.ExecutionSample\` samples."
    echo
    echo "#### Top-15 self-time frames (TOP frame of each sample)"
    echo
    echo '| samples | share | frame |'
    echo '|---|---|---|'
    awk '/stackTrace = \[/ {getline; gsub(/^[ \t]+|[ \t]+$/,""); print}' "$txt" \
      | sort | uniq -c | sort -rn | head -15 \
      | awk -v t="$total" '{n=$1; $1=""; sub(/^ /,""); printf "| %d | %.2f%% | `%s` |\n", n, 100*n/t, $0}'
    echo
    echo "#### Named sites, INCLUSIVE share (any frame in the sample)"
    echo
    echo '| site | samples | share |'
    echo '|---|---|---|'
    while IFS= read -r line; do
      [ -n "$line" ] || continue
      local sym="${line%%:*}" desc="${line#*:}"
      local n
      n=$(awk -v s="$sym" 'BEGIN{RS="jdk.ExecutionSample \\{"} NR>1 && index($0,s){c++} END{print c+0}' "$txt")
      awk -v d="$desc" -v n="$n" -v t="$total" \
        'BEGIN{printf "| %s | %d | %.2f%% |\n", d, n, (t>0 ? 100*n/t : 0)}'
    done <<< "$(printf '%s\n' "$sites")"
    echo
  } > "$raw/$label.$phase.profile.md"
  echo "  $phase: $total samples -> raw/$label.$phase.profile.md"
  ls -l "$jfr" | awk '{printf "  %s: %.1f MB\n", "'"$phase"'.jfr", $5/1048576}'
}

echo "== profiling $label: $patients arrivals =="

clojure -J-XX:StartFlightRecording=filename="$out/$label.gen.jfr",settings=profile \
  -M:ehrt sim run --seed "$seed" --patients "$patients" --churn --config "$config" \
  --format ground-truth > "$out/$label.prof.edn" 2>/dev/null
report "$out/$label.gen.jfr" generate

clojure -J-XX:StartFlightRecording=filename="$out/$label.check.jfr",settings=profile \
  -M:ehrt sim check --config "$config" < "$out/$label.prof.edn" > /dev/null 2>&1 || true
report "$out/$label.check.jfr" check
