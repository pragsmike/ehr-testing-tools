#!/usr/bin/env bash
# Runs `scenario_census.clj` over one cell's ground-truth log.
#
#   .agents/plans/2026-09-05-performance-measurement/scenario-census.sh \
#       <ground-truth.edn> <config.edn>
#
# CLASSPATH IS `:dev`, PLUS ONE AD-HOC PATH, and it is worth saying why
# it is not `bin/event-census`'s pattern. That script names its own
# single dependency and overrides `:paths`, which it can afford because
# it loads no component at all -- it parses EDN with Clojure and
# nothing else. This one requires `ehrt.sim-engine.interface`, and a
# Polylith component's own `deps.edn` does NOT declare its brick
# dependencies (the workspace does), so naming `poly/sim-engine` alone
# resolves and then fails at load on `ehrt.sim-model.interface`. `:dev`
# is the alias that already carries the whole component graph; the
# `-Sdeps` here adds this directory to it and changes nothing else.
set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)"
src="$repo_root/.agents/plans/2026-09-05-performance-measurement/census-src"
cd -- "$repo_root" || exit 2

exec clojure -Sdeps "{:aliases {:perf-census {:extra-paths [\"$src\"]}}}" \
     -M:dev:perf-census -m ehrt.perf.scenario-census "$@"
