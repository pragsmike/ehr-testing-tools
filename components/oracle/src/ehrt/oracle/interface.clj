(ns ehrt.oracle.interface
  "Public seam for `ehrt.oracle` (standing-equipment promotion,
  2026-08-05, `notes/ADRs.md` promotion ADR, AR-P-2). The ONLY real
  caller is `bin/regression-oracle`'s own synthetic per-worktree
  classpath, which now invokes `-m ehrt.oracle.interface` (repointed
  from `-m ehrt.oracle.digest` directly, AR-P-3) -- a dev-tool entry
  point through its own component's interface rather than reaching an
  implementation namespace directly, the same discipline every other
  cross-component call in this workspace already follows. Digest logic
  itself lives entirely in `ehrt.oracle.digest`; this namespace re-
  exports only the one thing an external caller needs."
  (:require [ehrt.oracle.digest :as digest]))

(defn -main [out-dir] (digest/-main out-dir))
