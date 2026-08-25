(ns ehrt.person-simulator.not-implemented
  "The skeleton's one behaviour: throwing legibly.

  Arc 2b step 1 lands `interface.clj`'s front door with no
  implementation behind it, so that step 2's limitation tests can be
  born RED for exactly one reason -- `not-implemented` -- rather than
  red for a compile error, which proves nothing about the gate.")

(defn not-implemented
  "Throws `not-implemented`, naming the var that has no body yet."
  [var-sym ctx]
  (throw (ex-info "not-implemented"
                  (assoc ctx :var (str var-sym) :adr "ADR-0172"))))
