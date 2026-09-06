#!/usr/bin/env bash
# Flight-records one cell's generate phase and its check phase, and
# aggregates each recording's `jdk.ExecutionSample` events two ways.
#
#   profile-cell.sh <label> <config> <patients> <out-dir>
#   profile-cell.sh --aggregate <label> <out-dir> <phase>...
#
# The second form re-runs only the AGGREGATION, over `.jfr` files a
# previous invocation already wrote. Recording a 22,500-arrival
# generate phase costs forty minutes and aggregating it costs two, so
# the two halves are separable on purpose: a bug in the reporting half
# must never cost a re-record.
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
set -uo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd -- "$repo_root" || exit 2
raw="$repo_root/.agents/plans/2026-09-05-performance-measurement/raw"
mkdir -p "$raw"
seed=20260824

# The named sites, in the words the two records name them by. Every
# entry is a REAL symbol, resolved out of the source before it was
# written here (Clojure munges `-` to `_` in class names).
read -r -d '' sites <<'SITES' || true
ehrt.sim_engine.fold$apply_events|apply-events (the one fold both phases run)
ehrt.sim_engine.fold$replay|replay -- ADR-0169's 14-calls-per-check-all site
ehrt.sim_check.check$occupancy_within_capacity|occupancy-within-capacity (ADR-0169: 54.9% of check)
ehrt.sim_model.facility$occupancy_board|occupancy-board (ADR-0169 OUT-list, 8.1% of generate)
ehrt.sim_engine.decide$waiting_boarder|decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%)
ehrt.sim_engine.log_index$last_uncancelled_index|last-uncancelled-index (ADR-0169 F-3, 5.9%)
ehrt.sim_engine.evolve|evolve (the per-event patient fold)
ehrt.sim_engine.decide|decide (the generator's dispatch)
ehrt.sim_check.check|sim-check, whole namespace
ehrt.person_simulator|person-simulator (what :persons costs)
ehrt.patient_simulator|patient-simulator (the module cohort's walk)
ehrt.sim_engine.run$select_person|select-person (NOT on any prior list -- full pool scan per arrival)
clojure.lang.LispReader|EDN parsing of the input log (reader, not invariants)
malli.core|malli (residue after the 642d70a validator hoist)
SITES

# `local` is deliberately absent below. Bash expands EVERY word of a
# `local a=$1 b=$a` before it assigns any of them, so the second
# reference sees an unbound name and `set -u` kills the script -- which
# is exactly how the first cut of this file died after recording a
# perfectly good 32 MB profile.
report () {  # <label> <phase> <jfr> <out-dir>
  r_label="$1"; r_phase="$2"; r_jfr="$3"; r_out="$4"
  r_txt="$r_out/$r_label.$r_phase.jfrtxt"
  [ -s "$r_jfr" ] || { echo "  $r_phase: no recording at $r_jfr" >&2; return 1; }
  jfr print --stack-depth 2048 --events jdk.ExecutionSample "$r_jfr" > "$r_txt" || return 1
  r_total=$(grep -c 'stackTrace = \[' "$r_txt")
  [ "$r_total" -gt 0 ] || { echo "  $r_phase: zero samples" >&2; return 1; }
  {
    echo "### \`$r_label\` — $r_phase"
    echo
    echo "\`$(basename "$r_jfr")\` ($(du -h "$r_jfr" | cut -f1)), **$r_total** \`jdk.ExecutionSample\` samples."
    echo
    echo "#### Top-15 self-time frames (the TOP frame of each sample)"
    echo
    echo '| samples | share | frame |'
    echo '|---|---|---|'
    awk '/stackTrace = \[/ {getline; gsub(/^[ \t]+|[ \t]+$/,""); print}' "$r_txt" \
      | sort | uniq -c | sort -rn | head -15 \
      | awk -v t="$r_total" '{n=$1; $1=""; sub(/^ /,""); printf "| %d | %.2f%% | `%s` |\n", n, 100*n/t, $0}'
    echo
    echo "#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)"
    echo
    echo '| site | samples | share |'
    echo '|---|---|---|'
    # ONE PASS FOR ALL TWELVE SITES, not one pass each. The 22,500-cell
    # recording prints to gigabytes of text, and a pass per site turns a
    # two-minute aggregation into half an hour for an identical answer.
    awk -v t="$r_total" -v sites="$sites" \
      'BEGIN{RS="jdk.ExecutionSample \\{"
             n=split(sites, L, "\n")
             for(i=1;i<=n;i++){ p=index(L[i],"|"); if(p){ S[i]=substr(L[i],1,p-1); D[i]=substr(L[i],p+1) } }}
       NR>1 { for(i=1;i<=n;i++) if(S[i] != "" && index($0,S[i])) c[i]++ }
       END  { for(i=1;i<=n;i++) if(S[i] != "")
                printf "| %s | %d | %.2f%% |\n", D[i], c[i]+0, (t>0 ? 100*(c[i]+0)/t : 0) }' \
      "$r_txt"
    echo
    echo "#### Top-20 by INNERMOST PROJECT FRAME"
    echo
    echo 'Each sample attributed to the first `ehrt.` frame in its stack —'
    echo 'the project function actually running, with the `clojure.lang`'
    echo 'machinery it is running THROUGH folded into it. This is the table'
    echo 'that names a fix site: the self-time table above is all'
    echo '`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,'
    echo 'and the named-site table only sees sites someone thought to name.'
    echo
    echo '| samples | share | innermost `ehrt.` frame |'
    echo '|---|---|---|'
    awk 'BEGIN{RS="jdk.ExecutionSample \\{"}
         NR>1 { n=split($0, L, "\n")
                for(i=1;i<=n;i++) if (L[i] ~ /ehrt\./) {
                  f=L[i]; sub(/^[ \t]+/,"",f); sub(/\(.*$/,"",f); print f; break } }' \
      "$r_txt" | sort | uniq -c | sort -rn | head -20 \
      | awk -v t="$r_total" '{n=$1; $1=""; sub(/^ /,""); printf "| %d | %.2f%% | `%s` |\n", n, 100*n/t, $0}'
    echo
  } > "$raw/$r_label.$r_phase.profile.md"
  rm -f "$r_txt"
  echo "  $r_phase: $r_total samples -> raw/$r_label.$r_phase.profile.md"
}

if [ "${1:-}" = "--aggregate" ]; then
  label="${2:?}"; out="${3:?}"; shift 3
  for phase in "$@"; do report "$label" "$phase" "$out/$label.$phase.jfr" "$out"; done
  exit 0
fi

label="${1:?usage: profile-cell.sh <label> <config> <patients> <out-dir>}"
config="${2:?}"
patients="${3:?}"
out="${4:?}"
mkdir -p "$out"

echo "== profiling $label: $patients arrivals =="

# `-Xlog:disable` IS LOAD-BEARING, not tidiness. `StartFlightRecording`
# announces itself on STDOUT ("Started recording 1..."), and stdout here
# is where the ground-truth log goes -- so without it the profiled run's
# own output is 218 bytes of JFR banner followed by the corpus, which no
# reader of that file will parse.
#
# `maxsize=2g` for the same class of reason. The default is 250 MB and
# the recording is a RING: a forty-minute generate phase overruns it and
# the OLDEST samples are silently discarded, so the profile would
# describe the end of the run while claiming to describe the run.
jfr_opts="settings=profile,maxsize=2g"

if [ ! -s "$out/$label.gen.jfr" ]; then
  clojure -J-Xlog:disable \
    -J-XX:StartFlightRecording=filename="$out/$label.gen.jfr","$jfr_opts" \
    -M:ehrt sim run --seed "$seed" --patients "$patients" --churn --config "$config" \
    --format ground-truth > "$out/$label.prof.edn" 2>/dev/null
fi
report "$label" gen "$out/$label.gen.jfr" "$out"

# CHECK READS THE TIMED RUN'S OWN LOG where one exists. `run-cells.sh`
# keeps `<label>.run1.edn`, and that log is the one whose wall and
# digest are recorded in `raw/` -- profiling the check phase over the
# very same bytes keeps the profile and the timing on one corpus rather
# than on two that merely came from the same seed.
check_in="$out/$label.run1.edn"
[ -s "$check_in" ] || check_in="$out/$label.prof.edn"
clojure -J-Xlog:disable \
  -J-XX:StartFlightRecording=filename="$out/$label.check.jfr","$jfr_opts" \
  -M:ehrt sim check --config "$config" < "$check_in" > /dev/null 2>&1
report "$label" check "$out/$label.check.jfr" "$out"
