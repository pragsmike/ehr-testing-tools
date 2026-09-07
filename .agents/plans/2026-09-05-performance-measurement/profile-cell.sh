#!/usr/bin/env bash
# Flight-records one cell's generate phase and its check phase, and
# aggregates each recording's `jdk.ExecutionSample` events four ways.
#
#   profile-cell.sh <label> <config> <patients> <out-dir>
#   profile-cell.sh --aggregate <label> <out-dir> <phase>...
#   profile-cell.sh --alloc     <label> <out-dir> <phase>...
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
# THE FOUR AGGREGATIONS ANSWER DIFFERENT QUESTIONS.
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
#
#   PROJECT FRAMES, self AND inclusive in one table -- each sample
#   attributed to its innermost `ehrt.` frame for the SELF column, and
#   to every distinct `ehrt.` frame in its stack for the INCLUSIVE
#   one. The two columns together separate a frame that is doing the
#   work from a frame that is merely on the way to it, which neither
#   of the two tables above can do: the self-time table names no
#   project frame at all and the named-site table only sees sites
#   someone thought to name. Added 2026-09-06, the post-program
#   profile session; before it this table carried the self column
#   only, at top-20.
#
#   THE `apply-events` CONCERN BREAKDOWN -- of the samples beneath
#   `fold/apply-events`, which of the fold's own per-concern functions
#   they are in. ADR-0180 moved four indexes into that one fold, so
#   `apply-events`' inclusive share is now a SUM of things that used
#   to be charged separately, and this table is what un-sums it. Only
#   the concerns with a NAMED function can appear: `:bed-index`,
#   `:boarder-index`, `:board` and `:cancel-index` have `update-*`
#   functions, `:patient-state` runs `evolve/evolve` and
#   `:encounter-stamp` runs `encounters/stamp-encounter`. The other
#   ten of `full-algebra`'s sixteen are inline `assoc`/`reduce`/`conj!`
#   forms inside `apply-events` itself with no frame of their own, and
#   they land in the residual row rather than being silently dropped.
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
clojure.lang.EdnReader|EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20%
malli.core|malli (residue after the 642d70a validator hoist)
SITES

# THE `apply-events` CONCERNS THAT HAVE A FRAME. Each row is
# `<awk regex>|<concern keyword> -- <function>`. The regex ends in
# `[.$]` for a REASON that is not decoration: `update_board` is a
# proper prefix of `update_boarders`, so a plain substring test would
# charge site 3 with every one of site 1's samples. The class-name
# character right after a Clojure fn's munged name is `.` (a method)
# or `$` (an inner fn), and never a letter.
#
# `:patient-state` IS MATCHED ON ITS NAMESPACE AND NOT ON A FUNCTION
# NAME, alone among the six, because `evolve` is a `defmulti`
# (`evolve.clj:91`): its methods compile to `ehrt.sim_engine.evolve$
# eval<n>$fn__<n>` and NO `evolve$evolve` class is ever on a stack.
# Matched by function name it reported 0.00% of a fold that runs it on
# every event, which is a zero produced by the instrument and not by
# the code. Only `evolve`'s own methods live in that namespace, so the
# namespace IS the concern here.
#
# THIS LIST REACHES awk THROUGH A FILE AND NOT THROUGH `-v`, and that
# is the whole reason the temporary exists. gawk runs ESCAPE
# PROCESSING over a `-v` assignment's value before the program ever
# sees it, so `\$` arrives as a bare `$` -- an anchor, not a literal
# dollar -- and every one of these regexes matches nothing. The first
# recording of the post-program session reported all six concerns at
# 0.00% and a 100% residual because of exactly that, which is what a
# silently-wrong instrument looks like when it still produces a
# well-formed table.
read -r -d '' concerns <<'CONCERNS' || true
fold\$update_beds[.$]|:bed-index -- `update-beds` (arc 3b sweep 2)
fold\$update_boarders[.$]|:boarder-index -- `update-boarders` (ADR-0180 site 1)
fold\$update_board[.$]|:board -- `update-board` (ADR-0180 site 3)
fold\$update_cancel_index[.$]|:cancel-index -- `update-cancel-index` (ADR-0180 site 4)
sim_engine\.evolve\$|:patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`)
encounters\$stamp_encounter[.$]|:encounter-stamp -- `encounters/stamp-encounter`
CONCERNS

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
    echo "#### Top-15 PROJECT frames, self AND inclusive"
    echo
    echo 'SELF attributes each sample to the first `ehrt.` frame in its stack —'
    echo 'the project function actually running, with the `clojure.lang`'
    echo 'machinery it is running THROUGH folded into it. INCLUSIVE counts'
    echo 'each sample once for every DISTINCT `ehrt.` frame anywhere in it,'
    echo 'so a caller carries everything under it. Ranked by SELF; inclusive'
    echo 'shares do not sum to 100%. This is the table that names a fix site:'
    echo 'the self-time table above is all `KeywordLookupSite`/`Util.equiv`'
    echo 'and belongs to no site at all, and the named-site table only sees'
    echo 'sites someone thought to name.'
    echo
    echo '| self | self % | incl % | `ehrt.` frame |'
    echo '|---|---|---|---|'
    # ONE PASS FOR BOTH COLUMNS AND THE CONCERN TABLE BELOW. The 22,500
    # recording is gigabytes of text; a pass per table is a pass too many.
    printf '%s\n' "$concerns" > "$r_out/$r_label.$r_phase.concerns"
    awk -v cfile="$r_out/$r_label.$r_phase.concerns" '
      BEGIN{ # THE CONCERN FILE IS READ BEFORE `RS` MOVES, and the order is
             # load-bearing rather than tidy. `getline var < file` splits
             # its input on RS like every other read, so reading the list
             # under the SAMPLE separator returns the whole file as one
             # record and leaves exactly ONE concern defined -- which is
             # what the second aggregation of the post-program session
             # reported, one populated row and five silently missing.
             while ((getline line < cfile) > 0) {
               p=index(line,"|"); if(!p) continue
               nc++; CR[nc]=substr(line,1,p-1); CD[nc]=substr(line,p+1) }
             RS="jdk.ExecutionSample \\{" }
      NR>1 {
        delete seen; inner=""
        n=split($0, L, "\n")
        for(i=1;i<=n;i++) if (L[i] ~ /ehrt\./) {
          f=L[i]; sub(/^[ \t]+/,"",f); sub(/\(.*$/,"",f)
          if (inner=="") { inner=f; self[f]++ }
          if (!(f in seen)) { seen[f]=1; incl[f]++ }
        }
        if ($0 ~ /sim_engine\.fold\$apply_events[.$]/) {
          A++; hit=0
          for(i=1;i<=nc;i++) if (CR[i] != "" && $0 ~ CR[i]) { CC[i]++; hit=1 }
          if (!hit) UN++
        }
      }
      END{ for (f in self) printf "SELF\t%d\t%d\t%s\n", self[f], incl[f]+0, f
           printf "AE\t%d\t%d\n", A+0, UN+0
           for(i=1;i<=nc;i++) if (CR[i] != "") printf "CONCERN\t%d\t%s\n", CC[i]+0, CD[i] }' \
      "$r_txt" > "$r_out/$r_label.$r_phase.agg"
    grep '^SELF' "$r_out/$r_label.$r_phase.agg" | cut -f2- | sort -rn | head -15 \
      | awk -F'\t' -v t="$r_total" \
          '{printf "| %d | %.2f%% | %.2f%% | `%s` |\n", $1, 100*$1/t, 100*$2/t, $3}'
    echo
    echo "#### \`apply-events\` concern breakdown"
    echo
    r_ae=$(grep '^AE' "$r_out/$r_label.$r_phase.agg" | cut -f2)
    r_un=$(grep '^AE' "$r_out/$r_label.$r_phase.agg" | cut -f3)
    echo "Of the **$r_ae** samples anywhere beneath \`fold/apply-events\`"
    echo "($(awk -v a="$r_ae" -v t="$r_total" 'BEGIN{printf "%.2f", 100*a/t}')% of the phase),"
    echo 'which of the fold'"'"'s own named per-concern functions they are in. A'
    echo 'sample in two concerns counts in both, so these do not sum to the'
    echo 'total; the residual row is the samples in NONE of them, which is'
    echo '`apply-events`'"'"' own machinery plus the ten concerns of'
    echo '`full-algebra` that are inline forms with no frame of their own.'
    echo
    echo '| concern | samples | % of `apply-events` | % of phase |'
    echo '|---|---|---|---|'
    grep '^CONCERN' "$r_out/$r_label.$r_phase.agg" | cut -f2- | sort -rn \
      | awk -F'\t' -v t="$r_total" -v a="$r_ae" \
          '{printf "| %s | %d | %.2f%% | %.2f%% |\n", $2, $1, (a>0 ? 100*$1/a : 0), 100*$1/t}'
    awk -v n="$r_un" -v t="$r_total" -v a="$r_ae" \
      'BEGIN{printf "| **residual** -- no named concern frame | %d | %.2f%% | %.2f%% |\n", n, (a>0 ? 100*n/a : 0), 100*n/t}'
    echo
  } > "$raw/$r_label.$r_phase.profile.md"
  rm -f "$r_txt" "$r_out/$r_label.$r_phase.agg" "$r_out/$r_label.$r_phase.concerns"
  echo "  $r_phase: $r_total samples -> raw/$r_label.$r_phase.profile.md"
}

# ALLOCATION, from the SAME recordings the CPU tables come from.
# `settings=profile` already enables `jdk.ObjectAllocationSample` at a
# `300/s` throttle with stack traces
# (`$JAVA_HOME/lib/jfr/profile.jfc`), so nothing had to be re-recorded
# to get this: the allocation profile and the CPU profile describe one
# run rather than two that merely came from the same seed.
#
# WHAT `weight` IS, because summing it wrongly is the easy mistake. A
# throttled allocation sample carries the estimated bytes that sample
# STANDS FOR, not the size of the one object that tripped it. Summing
# `weight` therefore estimates total allocation and counting samples
# does not, which is why every figure below is weight-summed and the
# sample count rides along as a second column rather than as the
# ranking.
alloc_report () {  # <label> <phase> <jfr> <out-dir>
  a_label="$1"; a_phase="$2"; a_jfr="$3"; a_out="$4"
  a_txt="$a_out/$a_label.$a_phase.alloctxt"
  [ -s "$a_jfr" ] || { echo "  $a_phase: no recording at $a_jfr" >&2; return 1; }
  jfr print --stack-depth 2048 --events jdk.ObjectAllocationSample "$a_jfr" > "$a_txt" || return 1
  a_n=$(grep -c 'jdk.ObjectAllocationSample {' "$a_txt")
  [ "$a_n" -gt 0 ] || { echo "  $a_phase: zero allocation samples" >&2; rm -f "$a_txt"; return 1; }
  awk 'BEGIN{ RS="jdk.ObjectAllocationSample \\{" }
       NR>1 { cls="(unknown)"; w=0; inner="(no project frame)"
              n=split($0, L, "\n")
              for(i=1;i<=n;i++){
                if (L[i] ~ /objectClass = /) {
                  cls=L[i]; sub(/.*objectClass = /,"",cls); sub(/ \(classLoader.*$/,"",cls)
                } else if (L[i] ~ /weight = /) {
                  s=L[i]; sub(/.*weight = /,"",s); split(s, W, " ")
                  v=W[1]+0; u=W[2]
                  w = (u=="GB") ? v*1073741824 : (u=="MB") ? v*1048576 : (u=="kB") ? v*1024 : v
                } else if (inner=="(no project frame)" && L[i] ~ /ehrt\./) {
                  f=L[i]; sub(/^[ \t]+/,"",f); sub(/\(.*$/,"",f); inner=f
                }
              }
              FW[inner]+=w; FN[inner]++; CW[cls]+=w; CN[cls]++; T+=w; N++ }
       END{ printf "TOTAL\t%.0f\t%d\n", T, N
            for(f in FW) printf "FRAME\t%.0f\t%d\t%s\n", FW[f], FN[f], f
            for(c in CW) printf "CLASS\t%.0f\t%d\t%s\n", CW[c], CN[c], c }' \
    "$a_txt" > "$a_out/$a_label.$a_phase.allocagg"
  a_bytes=$(awk -F'\t' '/^TOTAL/{print $2}' "$a_out/$a_label.$a_phase.allocagg")
  {
    echo "### \`$a_label\` — $a_phase, ALLOCATION"
    echo
    echo "\`$(basename "$a_jfr")\`, **$a_n** \`jdk.ObjectAllocationSample\` events"
    echo "(\`settings=profile\`'s own \`300/s\` throttle, stack traces on),"
    echo "**$(awk -v b="$a_bytes" 'BEGIN{printf "%.1f", b/1073741824}') GB** of estimated total allocation."
    echo
    echo "#### Top-15 by ALLOCATING PROJECT FRAME (innermost \`ehrt.\`)"
    echo
    echo '| est. alloc | share | samples | `ehrt.` frame |'
    echo '|---|---|---|---|'
    grep '^FRAME' "$a_out/$a_label.$a_phase.allocagg" | cut -f2- | sort -rn | head -15 \
      | awk -F'\t' -v t="$a_bytes" \
          '{printf "| %.2f GB | %.2f%% | %d | `%s` |\n", $1/1073741824, 100*$1/t, $2, $3}'
    echo
    echo "#### Top-15 by CLASS"
    echo
    echo '| est. alloc | share | samples | class |'
    echo '|---|---|---|---|'
    grep '^CLASS' "$a_out/$a_label.$a_phase.allocagg" | cut -f2- | sort -rn | head -15 \
      | awk -F'\t' -v t="$a_bytes" \
          '{printf "| %.2f GB | %.2f%% | %d | `%s` |\n", $1/1073741824, 100*$1/t, $2, $3}'
    echo
  } > "$raw/$a_label.$a_phase.alloc.md"
  rm -f "$a_txt" "$a_out/$a_label.$a_phase.allocagg"
  echo "  $a_phase alloc: $a_n samples -> raw/$a_label.$a_phase.alloc.md"
}

if [ "${1:-}" = "--alloc" ]; then
  label="${2:?}"; out="${3:?}"; shift 3
  for phase in "$@"; do alloc_report "$label" "$phase" "$out/$label.$phase.jfr" "$out"; done
  exit 0
fi

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
