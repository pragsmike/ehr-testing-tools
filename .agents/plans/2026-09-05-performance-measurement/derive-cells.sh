#!/usr/bin/env bash
# Derives this directory's six measurement-cell configs from
# `demos/scenarios/dense-7500/config.edn`. Run from the workspace root:
#
#   .agents/plans/2026-09-05-performance-measurement/derive-cells.sh
#
# Every cell is DERIVED, never independently authored -- the same rule
# `demos/scenarios/dense-7500/README.md` states for its own two
# siblings. Edit `config.edn` and re-run this script; never edit a cell
# file by hand. The script is idempotent and rewrites all six.
set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
src="$repo_root/demos/scenarios/dense-7500/config.edn"
out="$repo_root/.agents/plans/2026-09-05-performance-measurement"

[ -f "$src" ] || { echo "missing provenance: $src" >&2; exit 2; }

# The two dials, and NOTHING else moves.
#
#   ward scale -- every `:beds N :surge-slots M` pair in the facility
#   block multiplied by the factor, so the ladder has headroom the
#   arrival count cannot exhaust (this session's step 1).
#
#   :persons -- present verbatim, or its one line deleted. The deletion
#   is the same shape `config-nobed.edn` uses against `:bed-cycle`: one
#   whole line, matched anchored, so a partial match cannot silently
#   widen it.
for f in 1 3 9; do
  perl -pe 's/:beds (\d+) :surge-slots (\d+)/":beds ".($1*'"$f"')." :surge-slots ".($2*'"$f"')/e' \
    "$src" > "$out/cell-x$f-persons.edn"
  grep -v '^ :persons {:count 15000 :years 20}$' \
    "$out/cell-x$f-persons.edn" > "$out/cell-x$f-nopersons.edn"
done

# Each derivation is CHECKED rather than asserted -- if a rule stops
# matching (an upstream rename, a reflowed ward line), the counts below
# move and this script fails rather than emitting a silently-unscaled
# cell.
for f in 1 3 9; do
  n=$(grep -c ':beds [0-9]* :surge-slots [0-9]*' "$out/cell-x$f-persons.edn")
  [ "$n" -eq 4 ] || { echo "cell-x$f-persons.edn: $n ward lines, expected 4" >&2; exit 1; }
  grep -q '^ :persons {:count 15000 :years 20}$' "$out/cell-x$f-persons.edn" \
    || { echo "cell-x$f-persons.edn: :persons line absent" >&2; exit 1; }
  grep -q '^ :persons ' "$out/cell-x$f-nopersons.edn" \
    && { echo "cell-x$f-nopersons.edn: :persons survived the cut" >&2; exit 1; }
  d=$(( $(wc -l < "$out/cell-x$f-persons.edn") - $(wc -l < "$out/cell-x$f-nopersons.edn") ))
  [ "$d" -eq 1 ] || { echo "cell-x$f-nopersons.edn: $d lines cut, expected 1" >&2; exit 1; }
done

# x1 is the provenance UNCHANGED, and that is the strongest of the six
# checks: it proves the ward rewrite is a pure multiply and not a
# reformat riding along with it.
cmp -s "$src" "$out/cell-x1-persons.edn" \
  || { echo "cell-x1-persons.edn differs from its provenance" >&2; exit 1; }

echo "OK: six cells derived from demos/scenarios/dense-7500/config.edn"
