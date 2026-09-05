#!/usr/bin/env bash
# The measurement driver. Runs one cell -- warm-up, two timed
# `sim run --format ground-truth` JVMs, two timed `sim check` JVMs --
# and writes its raw `/usr/bin/time -v` output, its per-kind census and
# one `results.tsv` row into the output directory.
#
#   .agents/plans/2026-09-05-performance-measurement/run-cells.sh \
#       <label> <config> <patients> <out-dir>
#
# THE LOGS DO NOT COME BACK INTO THE REPO. A 7,500-arrival
# ground-truth log is 67 MB and the cells above it are multiples of
# that; `<out-dir>` is a path OUTSIDE the worktree and only the
# timings, the digests and the census follow this script home
# (R-commit-cells commits the configs, the driver and the raw timings,
# which is what those three are).
#
# WHAT THE WARM-UP ACTUALLY WARMS, stated because the obvious reading
# is wrong. Every timed run is a FRESH JVM, so a warm-up run cannot
# warm the JIT of anything that is subsequently measured -- each timed
# process re-interprets and re-compiles from cold. What it does warm is
# the OS page cache over the classpath and the module resources, and
# Clojure's own compiled-class cache. A SMALL run warms both exactly as
# well as a large one, so the warm-up here is a 750-arrival run of the
# same command shape rather than a repeat of the cell, which at the top
# of the decade is the difference between a feasible session and an
# infeasible one.
set -uo pipefail

label="${1:?usage: run-cells.sh <label> <config> <patients> <out-dir>}"
config="${2:?}"
patients="${3:?}"
out="${4:?}"

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd -- "$repo_root" || exit 2
mkdir -p "$out"

seed=20260824
raw="$repo_root/.agents/plans/2026-09-05-performance-measurement/raw"
mkdir -p "$raw"

secs () {  # the "Elapsed (wall clock)" line of a `time -v` file, as seconds
  awk -F': ' '/Elapsed \(wall clock\)/ {
    n = split($2, p, ":")
    print (n == 3) ? p[1]*3600 + p[2]*60 + p[3] : p[1]*60 + p[2]
  }' "$1"
}
rss () { awk -F': ' '/Maximum resident set size/ {printf "%.0f", $2/1024}' "$1"; }

echo "== $label: $patients arrivals, $(basename "$config") =="

# --- warm-up (see the header for what this does and does not warm) ---
bin/ehrt sim run --seed "$seed" --patients 750 --churn --config "$config" \
  --format ground-truth > "$out/$label.warmup.edn" 2>/dev/null
rm -f "$out/$label.warmup.edn"

# --- generate, twice, one fresh JVM each ---
for i in 1 2; do
  /usr/bin/time -v bin/ehrt sim run --seed "$seed" --patients "$patients" --churn \
    --config "$config" --format ground-truth \
    > "$out/$label.run$i.edn" 2> "$raw/$label.run$i.time"
  echo "EXIT_STATUS_OBSERVED=$?" >> "$raw/$label.run$i.time"
done

# --- the byte-identity invariant, asserted and not assumed ---
d1=$(sha256sum < "$out/$label.run1.edn" | cut -d' ' -f1)
d2=$(sha256sum < "$out/$label.run2.edn" | cut -d' ' -f1)
{ echo "run1 $d1"; echo "run2 $d2"; } > "$raw/$label.sha256"
if [ "$d1" != "$d2" ]; then
  echo "FAIL: $label's two timed generate runs are NOT byte-identical" >&2
  echo "IDENTICAL=no" >> "$raw/$label.sha256"
  exit 1
fi
echo "IDENTICAL=yes" >> "$raw/$label.sha256"
rm -f "$out/$label.run2.edn"

# --- check, twice, one fresh JVM each, over run 1's log ---
bin/ehrt sim check --config "$config" < "$out/$label.run1.edn" > /dev/null 2>&1
for i in 1 2; do
  /usr/bin/time -v bin/ehrt sim check --config "$config" \
    < "$out/$label.run1.edn" > "$out/$label.check$i.out" 2> "$raw/$label.check$i.time"
  echo "EXIT_STATUS_OBSERVED=$?" >> "$raw/$label.check$i.time"
done

# --- census and event count ---
# The count is read off `bin/event-census`'s own "Total:" line rather
# than counted here a second way -- a private recount would be a second
# definition of "event" for the census tool's answer to disagree with.
bin/event-census "$out/$label.run1.edn" > "$raw/$label.census.md" 2>/dev/null
events=$(sed -n 's/^Total: \*\*\([0-9]*\) events\*\*.*/\1/p' "$raw/$label.census.md")
: "${events:=0}"

g1=$(secs "$raw/$label.run1.time");   g2=$(secs "$raw/$label.run2.time")
c1=$(secs "$raw/$label.check1.time"); c2=$(secs "$raw/$label.check2.time")
gm=$(awk -v a="$g1" -v b="$g2" 'BEGIN{printf "%.2f", (a+b)/2}')
cm=$(awk -v a="$c1" -v b="$c2" 'BEGIN{printf "%.2f", (a+b)/2}')
gr=$(rss "$raw/$label.run1.time"); cr=$(rss "$raw/$label.check1.time")
cx=$(awk -F'=' '/EXIT_STATUS_OBSERVED/{print $2}' "$raw/$label.check1.time")

printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
  "$label" "$patients" "$events" "$g1" "$g2" "$gm" "$c1" "$c2" "$cm" "$gr" "$cr" \
  >> "$raw/results.tsv"
echo "$label: events=$events generate=${gm}s check=${cm}s (check exit $cx) rss=${gr}/${cr} MB"
