(ns user
  "Dev-only entry namespace. Clojure loads `user.clj` from the CLASSPATH
  ROOT during runtime init, before `clojure.main` sees an argument, and
  `development/src` is on the classpath only under the `:dev` alias
  (`deps.edn`, `:dev :extra-paths [\"development/src\"]`). So this file
  runs for `clojure -M:dev ...` and for the development REPL, and for
  nothing that ships: no project's `deps.edn` names `development/src`,
  `clojure -M:poly test` does not use `:dev`, and CI runs neither.

  It exists for ONE reason (D4, 2026-09-07): to turn REFLECTION WARNINGS
  on for every namespace compiled after it in the same process, so that a
  dev load says out loud where the compiler could not resolve a call
  statically. It is a warning SURFACE, not a gate -- a namespace already
  loaded when this file runs is not re-analysed and reports nothing, and
  nothing here fails a build. The session that added it RECORDED every
  warning the surface produced and fixed none, deliberately.

  WHY `alter-var-root` AND NOT `set!`. `clojure.lang.Compiler/load` pushes
  a thread binding for `*warn-on-reflection*` around every file it loads
  -- this one included -- and pops it on the way out. A bare `set!` here
  would therefore set the flag for the remaining forms of THIS file and
  for nothing else: the binding it wrote is gone before the first
  `require` of real code. Writing the var's ROOT is what survives, and
  every later `load` takes its initial value from that root.")

(alter-var-root #'*warn-on-reflection* (constantly true))
