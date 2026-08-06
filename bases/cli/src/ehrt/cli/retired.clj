(ns ehrt.cli.retired
  "The retired `clojure -M:cli` alias's own redirect (AR-EP-1, ux
  epilogue, `notes/adr/0065-ux-epilogue.md`): `:cli` was the
  pre-monorepo standalone sim CLI's own entry point (`git show
  906a954:.staging/deps.edn`, `:cli {:main-opts [\"-m\"
  \"ehr-testing-sim.cli\"]}`) and died silently at the monorepo
  consolidation -- the alias itself was simply never carried over,
  while every doc teaching its invocation WAS swept later. Left alone,
  `clojure -M:cli run ...` falls through to `clojure.main` with no
  alias to resolve, which treats the bare verb `run` as an init-script
  path and throws FileNotFoundException instead of saying anything
  useful. This namespace exists so that shape gets an operator-facing
  answer in words -- requires nothing beyond `clojure.core` so the
  redirect itself can never fail to resolve.")

(defn retired-message
  "The operator-facing redirect printed when the retired `:cli` alias
  is invoked. Deliberately carries no internal citation, milestone, or
  session shorthand -- this speaks to whoever typed the old command,
  not to a future maintainer (that provenance lives in this ns's own
  docstring instead)."
  []
  (str "clojure -M:cli is retired.\n"
       "\n"
       "The CLI moved: use bin/ehrt instead. For example:\n"
       "\n"
       "  bin/ehrt sim run --seed 42 --patients 200 --config config/busy-weekday.edn\n"
       "\n"
       "run bin/ehrt help for commands"))

(defn -main
  [& _args]
  (binding [*out* *err*]
    (println (retired-message)))
  (System/exit 2))
