(ns ehrt.docs-tooling.exit-truthfulness-test
  "ADR-0155, closing register rows L2-2, L2-3, L2-4, L2-5 and L2-6 --
  review 3's H-2/H-3 watch item firing in the NEW shape it was written
  to watch for.

  The exit-code law is stated on four surfaces and all four forbid *a
  pipe or `tail` on the GATE command*. ADR-0152's mask was in the
  WRAPPER's last command instead: `false > /dev/null 2>&1;
  MAKE_EXIT=$?; echo \"MAKE_EXIT=$MAKE_EXIT\" | tee /dev/null` prints
  `MAKE_EXIT=1` and exits 0. The law was obeyed and the exit was still
  masked, because the mask migrated to a construct the law does not
  mention. That is this review's cross-dimension pattern exactly: a gate
  whose population is narrower than the class it is read as enforcing.

  Four claims, each red at this session's own red-first commit:

  (a) A TAUGHT IDIOM MUST EXIT ITS STATUS (register L2-2) -- a lint over
      `.agents/skills/**` and its `.claude/skills/**` mirror. A taught
      shell snippet that RENDERS a captured status with `echo` -- either
      `echo ...$?` directly, or `echo \"$VAR\"` after `VAR=$?` -- and
      never `exit`s it teaches a block whose own exit code is 0.
      `.agents/skills/extraction-stage/SKILL.md:95` taught exactly that:
      *\"capture ... full log and exit code directly (`> file 2>&1; echo
      EXITCODE:$?`)\"*, in the skill whose whole purpose is un-masked
      verification. `printf 'false > /tmp/x.log 2>&1; echo EXITCODE:$?'
      | bash` prints `EXITCODE:1` and the block exits 0.

      The rule deliberately covers BOTH shapes, not only the one found:
      the `VAR=$?` + `echo \"$VAR\"` half is what actually masked
      ADR-0152, and a lint that caught only a literal `$?` inside an
      echo would repeat the error this whole review is about. A bare
      `VAR=$?` with no echo is NOT flagged -- capturing a status is
      correct practice; appearing to have reported it is the defect.

  (b) `bin/preflight` TELLS THE TRUTH WHEN ITS CI QUERY FAILS (L2-3) AND
      ITS EXIT CODE IS A CLAIM (L2-4) -- the FIRST behavioral test of
      that script; it had none at all. A failed `gh run list` used to set
      `runs_out=\"\"`, which made `if [ -n \"$runs_out\" ]` false, which
      left `any_red`/`any_pending` at 0, which reached the `else` and
      printed **`OK: last five runs all green (or none found)`**.
      ADR-0155's Step 0 witnessed it verbatim against a failing `gh`:

          FAIL: gh run list failed:
          HTTP 401: Bad credentials (https://api.github.com/graphql)
          OK: last five runs all green (or none found)
          == bin/preflight complete ==        (exit 0)

      A failed CI query rendering as a green CI report, in the one
      script whose Step-0 job is to establish CI colour and that
      `build-session/SKILL.md` tells every session to run. Author ruling
      R4-Q2 (c): fail-closed AND an `UNKNOWN:` branch.

      Scoping note, deliberate: the CI assertions read only the
      `-- 1. --` section, and nothing here asserts that preflight exits 0
      in the ambient checkout. Checks 3-5 measure the AMBIENT
      environment (tree cleanliness, HEAD-vs-remote, reachability of
      origin), so an exit-0 assertion would assert a property of the
      machine rather than of the script -- and would go red on every
      `pull_request` run, whose checkout is a merge ref that cannot equal
      `origin/main`. What IS asserted is the script's behaviour under a
      controlled `gh`, plus that exit 0 is still reachable.

  (c) `bin/post-push-verify` CHECK 3 RENDERS `UNKNOWN:` (L2-6) -- a
      non-zero `gh` had its stderr folded INTO the status field by a
      `2>&1` capture, rendering as `status=error: HTTP 401: Bad
      credentials conclusion=<pending>`, which skims as \"pending\".
      Author ruling R4-Q3 (a): the check STAYS ADVISORY per AR-CI-4 --
      the script still exits 0 unconditionally -- and a failed query
      reports `UNKNOWN:` instead of a plausible-looking status line.

      Asserted BEHAVIORALLY, on the same throwaway-repo shape
      `post-push-verify-range-test` established for review 3's D1-6: a
      bare origin reached by path, the real script copied into the
      fixture's own `bin/` so its `${BASH_SOURCE[0]}/..` root resolution
      lands there, and an ASCII-only history so checks 1 and 2 (both
      correctly fail-closed, both ahead of check 3) pass and check 3 is
      actually reached. No network.

  (d) THE `use-cases` LOOP FAILS LOUD (L2-5) -- `Makefile:88-92`'s
      `@for f in target/use-cases/*.txt; do python3 ...; done` was the
      only masking construct in the Makefile: a for-loop's exit status is
      its LAST iteration's, so a converter that failed on an early file
      and succeeded on a later one left the target green. The test runs
      the committed loop TEXT verbatim -- behaviour, not prose -- in a
      scratch directory against a stub converter that fails on the first
      of two inputs and succeeds on the second: the exact masking shape."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.docs-tooling.make-graph :as mg]))

;; ---------------------------------------------------------------------
;; shared scratch helpers
;; ---------------------------------------------------------------------

(defn- temp-dir ^java.io.File [prefix]
  (.toFile (java.nio.file.Files/createTempDirectory
            prefix (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- delete-tree! [^java.io.File f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)] (delete-tree! child)))
  (.delete f))

(defn- stub-dir!
  "A directory holding an executable `name` whose body is `body`, meant
  to be put first on PATH."
  [^java.io.File root name body]
  (let [d (io/file root (str "stub-" name))
        f (io/file d name)]
    (.mkdirs d)
    (spit f (str "#!/usr/bin/env bash\n" body "\n"))
    (.setExecutable f true)
    (.getPath d)))

(defn- with-path
  "Run `args` with `dir` first on PATH. `env` PREPENDS to the inherited
  environment; `clojure.java.shell`'s own `:env` would REPLACE it,
  taking HOME and the rest of PATH with it and breaking the `git` these
  scripts call."
  [dir & args]
  (apply shell/sh "env" (str "PATH=" dir ":" (System/getenv "PATH")) args))

;; ---------------------------------------------------------------------
;; (a) the taught-idiom lint
;; ---------------------------------------------------------------------

(def ^:private skill-roots [".agents/skills" ".claude/skills"])

(defn- markdown-files [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".md"))))

(def ^:private anti-pattern-marker
  "A skill may SHOW the masking shape in order to name it -- HISTORY.md's
  own account of ADR-0152 quotes the exact wrapper that masked. That is
  worth keeping, and it is still the shape a reader could copy, so the
  exemption is DECLARED rather than inferred: the prose introducing such
  a snippet carries this token, which makes every negative example in
  the skill tree greppable. An undeclared occurrence stays red -- which
  is what caught this very paragraph when it was first written."
  "ANTI-PATTERN")

(def ^:private context-window
  "How much preceding prose counts as the snippet's introduction."
  240)

(defn- spans-with-context
  "Every taught snippet as {:snippet :context}, `context` being the prose
  immediately before it. Uses an explicit matcher rather than `re-seq`
  because the exemption above needs each match's POSITION, not just its
  text."
  [text pattern group]
  (let [m (re-matcher pattern text)]
    (loop [out []]
      (if (.find m)
        (recur (conj out {:snippet (.group m ^int group)
                          :context (subs text (max 0 (- (.start m) context-window)) (.start m))}))
        out))))

(defn- taught-snippets [^java.io.File file]
  (let [text (slurp file)
        without-fences (str/replace text #"(?s)```[A-Za-z0-9]*\n.*?```" "")]
    (concat (spans-with-context text #"(?s)```[A-Za-z0-9]*\n(.*?)```" 1)
            (spans-with-context without-fences #"(?s)`([^`]+)`" 1))))

(defn- captured-vars
  "Variables the snippet assigns from `$?`."
  [snippet]
  (set (map second (re-seq #"([A-Za-z_][A-Za-z0-9_]*)=\$\?" snippet))))

(defn- renders-a-status?
  "True when the snippet ECHOES a captured exit status -- a literal `$?`,
  or a variable it assigned from `$?`."
  [snippet]
  (let [echo-args (map second (re-seq #"echo\s+([^\n;]*)" snippet))
        vars (captured-vars snippet)]
    (boolean
     (some (fn [arg]
             (or (str/includes? arg "$?")
                 (some #(or (str/includes? arg (str "$" %))
                            (str/includes? arg (str "${" %)))
                       vars)))
           echo-args))))

(defn- propagates?
  "True when the snippet ends by exiting a status -- any `exit $VAR`,
  `exit \"$VAR\"` or `exit $?`."
  [snippet]
  (boolean (re-find #"exit\s+\"?\$" snippet)))

(defn- violations-in [^java.io.File file]
  (->> (taught-snippets file)
       (remove #(str/includes? (:context %) anti-pattern-marker))
       (filter #(renders-a-status? (:snippet %)))
       (remove #(propagates? (:snippet %)))
       (map (fn [s] {:file (.getPath file) :snippet (str/trim (:snippet s))}))))

(deftest a-taught-shell-idiom-that-reports-a-status-must-exit-it-test
  (let [files (mapcat markdown-files skill-roots)
        snippets (mapcat taught-snippets files)
        violations (mapcat violations-in files)]
    (is (pos? (count files))
        (str "sanity: no markdown found under " (pr-str skill-roots)
             " -- an empty scan is a pass that proves nothing (rulings.md#R-empty-population-is-red)"))
    (is (pos? (count snippets))
        "sanity: the skill tree must contain taught shell snippets -- an empty extraction makes the claim below vacuous")
    (is (empty? violations)
        (str "a tracked skill TEACHES an idiom that reports an exit status and never exits it.\n"
             "The block's own exit code is 0, so a session following it verbatim reports a failure "
             "in prose and a success to its caller -- the shape that masked ADR-0152, and that "
             "`extraction-stage/SKILL.md` taught at :95 until ADR-0155.\n"
             "Capture the status, then END the wrapper with `exit \"$VAR\"`.\n"
             (str/join "\n" (map (fn [{:keys [file snippet]}] (str "  " file ": " (pr-str snippet)))
                                 violations))))))

;; ---------------------------------------------------------------------
;; (b) bin/preflight -- its first behavioral test
;; ---------------------------------------------------------------------

(defn- ci-section
  "Only the `-- 1. Last five CI runs --` section. Checks 2-5 report on
  the ambient environment; pinning these assertions to section 1 is what
  keeps them assertions about the script."
  [out]
  (let [after (second (str/split out #"-- 1\. Last five CI runs[^\n]*\n" 2))]
    (first (str/split (or after out) #"\n-- 2\." 2))))

(defn- run-preflight-with-gh [root gh-body]
  (let [stub (stub-dir! root "gh" gh-body)]
    (with-path stub "bin/preflight")))

(deftest preflight-reports-unknown-and-fails-closed-when-the-ci-query-fails-test
  (let [root (temp-dir "ehrt-preflight-fail")]
    (try
      (let [{:keys [exit out]} (run-preflight-with-gh root "echo 'HTTP 401: Bad credentials' >&2; exit 1")
            section (ci-section out)]
        (is (string? section) "sanity: preflight must print its CI-runs section")
        (is (str/includes? section "UNKNOWN:")
            (str "a failed `gh run list` must render as UNKNOWN. CI-runs section was:\n" section))
        (is (not (str/includes? section "OK: last five"))
            (str "a failed CI query must NEVER fall through to the OK branch -- that is a failed "
                 "query rendering as a green CI report, in the script whose Step-0 job is to "
                 "establish CI colour. CI-runs section was:\n" section))
        (is (not (zero? exit))
            (str "preflight must be fail-closed on a query it could not answer (R4-Q2 c). Exit was "
                 exit)))
      (finally (delete-tree! root)))))

(deftest preflight-fails-closed-on-a-red-run-test
  (let [root (temp-dir "ehrt-preflight-red")]
    (try
      (let [{:keys [exit out]}
            (run-preflight-with-gh
             root "printf 'completed\\x1ffailure\\x1fdeadbeef\\x1f2026-01-01T00:00:00Z\\x1fa red run\\n'")
            section (ci-section out)]
        (is (str/includes? section "FINDING: a red")
            (str "a completed non-success run among the last five must be reported as a FINDING. "
                 "CI-runs section was:\n" section))
        (is (not (zero? exit))
            (str "preflight must exit non-zero when it prints a FINDING (R4-Q2 c). Every other "
                 "ceremony script in bin/ is fail-closed, and an always-zero exit invites a caller "
                 "to trust $?. Exit was " exit)))
      (finally (delete-tree! root)))))

(deftest preflight-reports-ok-when-the-ci-query-succeeds-test
  (let [root (temp-dir "ehrt-preflight-green")]
    (try
      (let [{:keys [out]}
            (run-preflight-with-gh
             root "printf 'completed\\x1fsuccess\\x1fcafe1234\\x1f2026-01-01T00:00:00Z\\x1fa green run\\n'")
            section (ci-section out)]
        (is (str/includes? section "OK: last five runs all green")
            (str "a successful query over green runs must still report OK -- the UNKNOWN branch must "
                 "not swallow the good case. CI-runs section was:\n" section))
        (is (not (str/includes? section "UNKNOWN:"))
            (str "a successful query must not report UNKNOWN. CI-runs section was:\n" section)))
      (finally (delete-tree! root)))))

(deftest preflight-exit-zero-is-still-reachable-test
  ;; `--help` is the one path whose outcome does not depend on the
  ;; ambient checkout, so it is the honest witness that fail-closed did
  ;; not quietly become fail-always.
  (let [{:keys [exit out]} (shell/sh "bin/preflight" "--help")]
    (is (zero? exit) (str "`bin/preflight --help` must exit 0. Exit was " exit))
    (is (str/includes? out "Usage: bin/preflight") "and print its usage")))

(deftest preflight-still-reports-ok-lines-in-the-ambient-checkout-test
  (let [{:keys [out]} (shell/sh "bin/preflight")]
    (is (str/includes? out "OK:")
        (str "preflight must still print OK lines for the checks that pass -- fail-closed is about "
             "the exit code and the UNKNOWN branch, never about withdrawing the report. Output:\n"
             out))))

;; ---------------------------------------------------------------------
;; (c) bin/post-push-verify check 3 -- behaviour, on a throwaway repo
;; ---------------------------------------------------------------------

(defn- git!
  [dir & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" (concat args [:dir dir]))]
    (when-not (zero? exit)
      (throw (ex-info (str "fixture git " (pr-str args) " failed: " err) {:exit exit :err err})))
    (str/trim out)))

(defn- build-pushed-fixture!
  "A bare origin plus a work clone with an ASCII-only two-commit history,
  both pushed. Checks 1 and 2 of `post-push-verify` therefore PASS, which
  is the point: they are fail-closed and run first, so check 3 is only
  reachable past them. Same shape as `post-push-verify-range-test`."
  [^java.io.File root]
  (let [origin (io/file root "origin.git")
        work (io/file root "work")]
    (git! root "init" "--bare" "-q" "--initial-branch=main" (.getPath origin))
    (git! root "init" "-q" "--initial-branch=main" (.getPath work))
    (git! work "config" "user.email" "fixture@example.invalid")
    (git! work "config" "user.name" "fixture")
    (git! work "config" "commit.gpgsign" "false")
    (git! work "remote" "add" "origin" (.getAbsolutePath origin))
    (spit (io/file work "f") "a\n")
    (git! work "add" "f")
    (git! work "commit" "-q" "-m" "A plain ASCII message")
    (git! work "push" "-q" "origin" "main")
    (let [base (git! work "rev-parse" "HEAD")]
      (spit (io/file work "f") "a\nb\n")
      (git! work "commit" "-qam" "B plain ASCII message")
      (git! work "push" "-q" "origin" "main")
      (let [dest-dir (io/file work "bin")
            dest (io/file dest-dir "post-push-verify")]
        (.mkdirs dest-dir)
        (io/copy (io/file "bin/post-push-verify") dest)
        (.setExecutable dest true))
      {:work work :base base :tip (git! work "rev-parse" "HEAD")})))

(defn- run-post-push-verify [root gh-body]
  (let [{:keys [work base tip]} (build-pushed-fixture! root)
        stub (stub-dir! root "gh" gh-body)
        {:keys [exit out err]} (with-path stub "bin/post-push-verify" base tip :dir work)]
    {:exit exit :out (str out err)}))

(deftest post-push-verify-renders-unknown-when-the-ci-query-fails-test
  (let [root (temp-dir "ehrt-ppv-unknown")]
    (try
      (let [{:keys [exit out]} (run-post-push-verify root "echo 'HTTP 401: Bad credentials' >&2; exit 1")]
        (is (str/includes? out "-- 3. CI run at tip")
            (str "sanity: checks 1 and 2 must have passed so check 3 is reached -- if they did not, "
                 "everything below is vacuous. Output was:\n" out))
        (is (str/includes? out "UNKNOWN:")
            (str "a non-zero `gh` must render as UNKNOWN. Its stderr used to be folded into the "
                 "status field as `status=error: HTTP 401: Bad credentials conclusion=<pending>`, "
                 "which skims as 'pending' (R4-Q3 a). Output was:\n" out))
        (is (not (re-find #"status=\S*(?:error|401|credentials)" out))
            (str "`gh`'s error text must NOT appear as the value of the status field -- that is the "
                 "defect L2-6 names. Output was:\n" out))
        (is (zero? exit)
            (str "check 3 stays ADVISORY per AR-CI-4: the script still exits 0. R4-Q3 (a) "
                 "deliberately does not reopen that ruling -- only the rendering changes. Exit was "
                 exit)))
      (finally (delete-tree! root)))))

(deftest post-push-verify-renders-a-not-yet-indexed-run-as-no-run-test
  ;; ADR-0156, closing ADR-0155's own out-of-fence note. A FOURTH
  ;; exit-rendering shape, distinct from the three L2-6 named: `gh`
  ;; SUCCEEDS, but no run matches the tip yet, so jq's `.[0]` is null and
  ;; `[null,null,null] | join("\x1f")` yields two bare separators rather
  ;; than the empty string. Neither the `-z` guard nor the `"null"` guard
  ;; caught it, and check 3 printed `status= conclusion=<pending>` with
  ;; every field empty -- which reads as a run that exists and is still
  ;; going, in the one line a session quotes to say CI was reached.
  (let [root (temp-dir "ehrt-ppv-unindexed")]
    (try
      (let [{:keys [exit out]} (run-post-push-verify root "printf '\\x1f\\x1f'")]
        (is (str/includes? out "-- 3. CI run at tip")
            (str "sanity: checks 1 and 2 must have passed so check 3 is reached. Output was:\n"
                 out))
        (is (str/includes? out "no CI run found yet")
            (str "a not-yet-indexed run must render as NO RUN, not as a pending one. Output "
                 "was:\n" out))
        (is (not (re-find #"status= " out))
            (str "an empty status field must never be printed -- `status= conclusion=<pending>` "
                 "is the shape this test exists to keep out. Output was:\n" out))
        (is (zero? exit)
            (str "check 3 stays advisory per AR-CI-4; only the rendering changes. Exit was "
                 exit)))
      (finally (delete-tree! root)))))

(deftest post-push-verify-still-reports-a-successful-query-test
  (let [root (temp-dir "ehrt-ppv-ok")]
    (try
      (let [{:keys [exit out]}
            (run-post-push-verify
             root "printf 'completed\\x1fsuccess\\x1fhttps://example.invalid/run/1\\n'")]
        (is (str/includes? out "conclusion=success")
            (str "a successful query must still report the run's status and conclusion -- the "
                 "UNKNOWN branch must not swallow the good case. Output was:\n" out))
        (is (not (str/includes? out "UNKNOWN:"))
            (str "a successful query must not report UNKNOWN. Output was:\n" out))
        (is (str/includes? out "AR-CI-4")
            "check 3 must keep disclosing that it reports once and does not await conclusion")
        (is (zero? exit) (str "the script exits 0 (AR-CI-4). Exit was " exit)))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------
;; (d) the use-cases loop -- behaviour, run from the committed recipe
;; ---------------------------------------------------------------------

(defn- use-cases-loop-text
  "The `for ... done` block of the committed `use-cases:` recipe, as
  runnable shell: leading tabs and make's `@` echo-suppression stripped,
  and make's `$$` un-escaped to a single `$`."
  [makefile-text]
  (let [recipe (mg/target-recipe makefile-text "use-cases")
        from (drop-while #(not (re-find #"for\s+\w+\s+in" %)) recipe)
        block (concat (take-while #(not (str/includes? % "done")) from)
                      (take 1 (drop-while #(not (str/includes? % "done")) from)))]
    (when (seq (remove str/blank? block))
      (-> (str/join "\n" block)
          (str/replace #"(?m)^\t" "")
          (str/replace #"(?m)^@" "")
          (str/replace "$$" "$")))))

(deftest the-use-cases-converter-loop-fails-when-a-converter-fails-test
  (let [loop-text (use-cases-loop-text (slurp mg/makefile))]
    (is (some? loop-text)
        "sanity: the `use-cases:` recipe must still contain a for-loop over target/use-cases/*.txt -- if this is nil the claim below never runs")
    (when loop-text
      (let [root (temp-dir "ehrt-use-cases-loop")]
        (try
          (let [cases (io/file root "target/use-cases")]
            (.mkdirs cases)
            ;; Two inputs. The stub converter FAILS on the first and
            ;; SUCCEEDS on the second: a for-loop's exit status is its
            ;; last iteration's, so this is precisely the shape that let
            ;; a failed conversion leave `make use-cases` green.
            (spit (io/file cases "a-bad.txt") "")
            (spit (io/file cases "z-good.txt") "")
            (let [stub (stub-dir! root "python3"
                                  "for a in \"$@\"; do case \"$a\" in *a-bad*) exit 3;; esac; done\nexit 0")
                  {:keys [exit]} (with-path stub "bash" "-c" loop-text :dir root)]
              (is (not (zero? exit))
                  (str "the committed converter loop returned " exit " when a converter FAILED on "
                       "one of its inputs. A for-loop's exit is its LAST iteration's, so a failure "
                       "on an early file is erased by a success on a later one, and `make use-cases` "
                       "-- hence `make docsgen` -- reports success while a conversion failed. Add "
                       "`|| exit 1` inside the loop.\nLoop text as committed:\n" loop-text))))
          (finally (delete-tree! root)))))))

;; ---------------------------------------------------------------------
;; (e) ADR-0157 -- environment residue at its root, and the ASCII
;;     commit-message law gated BEFORE the commit exists
;;
;; Review 4's D3-1: the executable-bit class bit three times in one
;; window (ADR-0147 S-7, ADR-0149's own CI red, ADR-0154's preflight
;; disclosure) and was gated at the SYMPTOM every time --
;; `executable_bits_test` reads the index precisely because
;; `core.fileMode=false` makes the worktree bit lie. The cause is two
;; lines of `/mnt/c`-era git config surviving in an ext4 clone's own
;; `.git/config`, which no ADR had ever named and which no gate could
;; see, because `.git/config` is not tracked. A tracked test cannot
;; assert a property of an untracked file in someone else's clone; what
;; it CAN do is assert that the script every session runs at Step 0
;; reports the residue when it is there. That is what (i) below does.
;;
;; `roadmap.md#commit-msg-ascii-hook`: the ASCII commit-message law
;; (AR-RL2-5) is gated by `bin/post-push-verify` check 2, which by its
;; own name runs AFTER the push -- so for this one class the check
;; reports a fault that `rulings.md#R-amend-unpushed-message-only` has
;; already put out of reach. ADR-0156's second addendum is the live
;; instance: commit `04b6f66`'s message carries a U+2026 where three
;; ASCII dots belonged, found post-push and therefore unfixable in
;; place. (ii) and (iii) gate the same law one step earlier, in the
;; commit path, where refusing costs a re-write instead of a record.
;;
;; The scan itself is EXTRACTED, not copied: `bin/ascii-scan` is the one
;; implementation and both callers invoke it, so the pre-commit twin and
;; the post-push check cannot drift into disagreeing about what "ASCII"
;; means. (iv) is the gate on that.
;; ---------------------------------------------------------------------

(def ^:private green-gh-stub
  "A `gh run list` that answers with one completed, successful run, so
  preflight's check 1 is a constant and every verdict below is
  attributable to check 2."
  "printf 'completed\\x1fsuccess\\x1fcafe1234\\x1f2026-01-01T00:00:00Z\\x1fa green run\\n'")

(defn- environment-section
  "Only the `-- 2. Edit-root confirmation --` section. Same scoping
  discipline as `ci-section` above: checks 1 and 3-5 report on other
  things, so pinning these assertions to section 2 is what keeps them
  assertions about the residue checks."
  [out]
  (let [after (second (str/split out #"-- 2\. Edit-root confirmation[^\n]*\n" 2))]
    (first (str/split (or after out) #"\n-- 3\." 2))))

(defn- build-preflight-fixture!
  "A clone in which preflight's checks 1, 3, 4 and 5 ALL pass: a bare
  origin reached by path with `main` pushed, a clean tree, a `stable-*`
  tag on HEAD, and the real `bin/preflight` copied in so its
  `${BASH_SOURCE[0]}/..` root resolution lands inside the fixture. Only
  check 2's verdict is left free to vary, which is what makes an EXIT
  CODE measured here attributable to the environment residue and to
  nothing else -- the ambient checkout could not support that claim,
  which is why `preflight-exit-zero-is-still-reachable-test` above has
  to fall back to `--help`."
  [^java.io.File root]
  (let [origin (io/file root "origin.git")
        work (io/file root "work")]
    (git! root "init" "--bare" "-q" "--initial-branch=main" (.getPath origin))
    (git! root "init" "-q" "--initial-branch=main" (.getPath work))
    (git! work "config" "user.email" "fixture@example.invalid")
    (git! work "config" "user.name" "fixture")
    (git! work "config" "commit.gpgsign" "false")
    (git! work "remote" "add" "origin" (.getAbsolutePath origin))
    (let [bin-dir (io/file work "bin")
          dest (io/file bin-dir "preflight")]
      (.mkdirs bin-dir)
      (io/copy (io/file "bin/preflight") dest)
      (.setExecutable dest true))
    (spit (io/file work "f") "a\n")
    (git! work "add" "-A")
    (git! work "commit" "-q" "-m" "A plain ASCII fixture commit")
    (git! work "push" "-q" "origin" "main")
    (git! work "tag" "stable-fixture-0")
    work))

(defn- run-preflight-in [root config-pairs]
  (let [work (build-preflight-fixture! root)]
    (doseq [[k v] config-pairs]
      (if (nil? v)
        (shell/sh "git" "config" "--local" "--unset" k :dir (.getPath work))
        (git! work "config" "--local" k v)))
    (let [stub (stub-dir! root "gh" green-gh-stub)
          {:keys [exit out err]} (with-path stub "bin/preflight" :dir (.getPath work))]
      {:exit exit :out (str out err)})))

;; ---- (i) the residue checks, beside the /mnt/* check ----

(deftest preflight-flags-filemode-and-ignorecase-residue-test
  (let [root (temp-dir "ehrt-preflight-residue")]
    (try
      (let [{:keys [exit out]} (run-preflight-in root {"core.fileMode" "false"
                                                       "core.ignorecase" "true"})
            section (environment-section out)]
        (is (string? section)
            (str "sanity: preflight must print its edit-root section. Output was:\n" out))
        (is (str/includes? section "core.fileMode")
            (str "preflight must check core.fileMode beside the /mnt/* check -- it is the SAME "
                 "class of residue (D3-1), and the executable-bit class it causes was gated at "
                 "the symptom three times without anyone reading the cause. Section 2 was:\n"
                 section))
        (is (str/includes? section "core.ignorecase")
            (str "preflight must check core.ignorecase too -- no recorded hit yet, equally wrong "
                 "on ext4, and it can mask a case-only rename. Section 2 was:\n" section))
        (is (= 2 (count (re-seq #"FINDING:" section)))
            (str "each residue setting is its own FINDING -- one line naming one setting, so the "
                 "remedy printed beside it is the remedy for THAT setting. Section 2 was:\n"
                 section))
        (is (str/includes? section "git config --local core.fileMode true")
            (str "the FINDING must print the one-line remedy, not merely name the defect. "
                 "Section 2 was:\n" section))
        (is (str/includes? section "git config --local --unset core.ignorecase")
            (str "same for core.ignorecase. Section 2 was:\n" section))
        (is (not (zero? exit))
            (str "preflight must be fail-closed on residue it FOUND (ADR-0155, R4-Q2 c). Every "
                 "other check that prints a FINDING exits non-zero. Exit was " exit
                 ", output was:\n" out)))
      (finally (delete-tree! root)))))

(deftest preflight-reports-a-clean-environment-as-clean-test
  ;; The control. Without it, a check that simply always printed FINDING
  ;; would pass the test above -- and preflight is the script every
  ;; session runs at Step 0, so a permanently-red check would be read as
  ;; noise and then ignored, which is worse than no check.
  (let [root (temp-dir "ehrt-preflight-clean")]
    (try
      (let [{:keys [exit out]} (run-preflight-in root {"core.fileMode" "true"
                                                       "core.ignorecase" nil})
            section (environment-section out)]
        (is (not (str/includes? section "FINDING:"))
            (str "a clone with core.fileMode=true and core.ignorecase unset -- what `git clone` "
                 "itself produces on ext4 -- must report NO finding. Section 2 was:\n" section))
        (is (str/includes? section "OK: core.fileMode")
            (str "and must say so positively: the report is the artifact, not just the exit code. "
                 "Section 2 was:\n" section))
        (is (zero? exit)
            (str "with checks 1 and 3-5 all satisfied by this fixture and no residue, preflight "
                 "must exit 0 -- fail-closed must not have become fail-always. Exit was " exit
                 ", output was:\n" out)))
      (finally (delete-tree! root)))))

;; ---- (ii) the commit-msg hook, invoked directly ----

(def ^:private ellipsis
  "U+2026, built from its code point rather than written literally, so
  this test file stays pure ASCII and so the byte under test cannot be
  altered by anyone's editor or by a re-encoding of this source file.
  It is the exact character that produced ADR-0156's own violation."
  (str (char 0x2026)))

(defn- run-commit-msg-hook [message]
  (let [root (temp-dir "ehrt-commit-msg")]
    (try
      (let [f (io/file root "COMMIT_EDITMSG")]
        (spit f message)
        (let [{:keys [exit out err]} (shell/sh ".githooks/commit-msg" (.getPath f))]
          {:exit exit :out (str out err)}))
      (finally (delete-tree! root)))))

(deftest commit-msg-hook-refuses-a-non-ascii-message-test
  (let [{:keys [exit out]}
        (run-commit-msg-hook (str "docs: ADR-0156 addendum -- CI green at the tip\n"
                                  "\n"
                                  "841fb75" ellipsis " exactly\n"))]
    (is (not (zero? exit))
        (str "`.githooks/commit-msg` must REFUSE a message carrying a non-ASCII byte -- that is "
             "the whole point of moving the check into the commit path. Exit was " exit
             ", output was:\n" out))
    (is (str/includes? out "3:")
        (str "the refusal must name the offending LINE, so the author can find it without "
             "re-reading the whole message. Output was:\n" out))
    (is (re-find #"(?i)\be2 80 a6\b" out)
        (str "and must name the BYTES -- `e2 80 a6` is a U+2026 that renders as three dots and "
             "is invisible in a diff otherwise. That invisibility is exactly how ADR-0156's own "
             "addendum commit acquired one. Output was:\n" out))))

(deftest commit-msg-hook-passes-a-pure-ascii-message-test
  (let [{:keys [exit out]}
        (run-commit-msg-hook (str "docs: ADR-0157 -- review-4 fix 3/5\n"
                                  "\n"
                                  "841fb75 exactly, with three ASCII dots... and no ellipsis.\n"))]
    (is (zero? exit)
        (str "a pure-ASCII message must pass untouched. A hook that refuses good messages would "
             "be routed around with --no-verify within a session, which is worse than no hook. "
             "Exit was " exit ", output was:\n" out))))

(deftest commit-msg-hook-fails-closed-when-it-cannot-scan-test
  ;; ADR-0155's own law, applied to the hook: a check that could not
  ;; MEASURE is not a check that passed. Without this the hook's failure
  ;; mode on a missing scanner is a silent green commit.
  (let [{:keys [exit out err]} (shell/sh ".githooks/commit-msg" "/nonexistent/COMMIT_EDITMSG")
        text (str out err)]
    (is (not (zero? exit))
        (str "the hook must refuse the commit when it cannot read the message file at all, "
             "rather than treating an unscannable message as a clean one. Exit was " exit
             ", output was:\n" text))
    (is (str/includes? text "refused")
        (str "and must say the commit was refused, not merely emit a scanner error. Output "
             "was:\n" text))))

;; ---- (iii) installation: the hooksPath mechanism reaches commit-msg ----

(deftest the-hookspath-mechanism-installs-the-commit-msg-hook-test
  ;; Step 0(d): installation in this repo is `git config core.hooksPath
  ;; .githooks` -- a per-clone opt-in pointing git at the TRACKED
  ;; directory (AGENTS.md, and `ehrt doctor`'s own hooksPath check).
  ;; Nothing anywhere enumerates the hooks by name, so a third file in
  ;; `.githooks/` is installed by the same act that installed the first
  ;; two -- and this test is what proves that claim rather than assuming
  ;; it, by driving a real `git commit` through the real wiring.
  ;;
  ;; WSL_DISTRO_NAME is set for the fixture commit because `.githooks/
  ;; pre-commit` runs FIRST and refuses non-WSL shells; CI's Ubuntu
  ;; runners are not WSL, so without this the commit would be refused
  ;; for the wrong reason and the positive case below would be vacuous.
  ;; Satisfying pre-commit's own documented condition is the honest way
  ;; to reach the hook under test.
  (let [root (temp-dir "ehrt-hookspath")
        hooks-dir (.getAbsolutePath (io/file ".githooks"))]
    (try
      (let [work (io/file root "work")
            commit! (fn [msg]
                      (let [f (io/file root "msg.txt")]
                        (spit f msg)
                        (let [{:keys [exit out err]}
                              (shell/sh "env" "WSL_DISTRO_NAME=fixture"
                                        "git" "commit" "-F" (.getPath f)
                                        :dir (.getPath work))]
                          {:exit exit :out (str out err)})))]
        (git! root "init" "-q" "--initial-branch=main" (.getPath work))
        (git! work "config" "user.email" "fixture@example.invalid")
        (git! work "config" "user.name" "fixture")
        (git! work "config" "commit.gpgsign" "false")
        (git! work "config" "core.hooksPath" hooks-dir)
        (spit (io/file work "f") "a\n")
        (git! work "add" "f")
        (let [{:keys [exit out]} (commit! (str "test: a sha abbreviated with an ellipsis 841fb75"
                                               ellipsis "\n"))]
          (is (not (zero? exit))
              (str "a non-ASCII message must be refused THROUGH the real hooksPath wiring, not "
                   "only when the hook is invoked by hand -- otherwise the hook exists and is "
                   "never reached. Exit was " exit ", output was:\n" out))
          (is (str/includes? out "non-ASCII")
              (str "and the refusal a committer actually sees must say why. Output was:\n" out)))
        (is (not (zero? (:exit (shell/sh "git" "rev-parse" "--verify" "HEAD"
                                         :dir (.getPath work)))))
            "the refused commit must not exist -- a hook that reports and commits anyway is worse than none")
        (let [{:keys [exit out]} (commit! "test: a plain ASCII fixture message\n")]
          (is (zero? exit)
              (str "and an ASCII message must still commit through the same wiring. Exit was "
                   exit ", output was:\n" out))))
      (finally (delete-tree! root)))))

;; ---- (iv) one scan, two callers ----

(deftest the-ascii-byte-scan-lives-in-exactly-one-place-test
  (let [scanner (slurp "bin/ascii-scan")
        hook (slurp ".githooks/commit-msg")
        ppv (slurp "bin/post-push-verify")
        pattern "[^ -~]"]
    (is (str/includes? scanner pattern)
        "bin/ascii-scan is the one implementation and must hold the byte pattern itself")
    (testing "both callers invoke it"
      (is (str/includes? hook "bin/ascii-scan")
          ".githooks/commit-msg must invoke bin/ascii-scan, not re-derive the scan")
      (is (str/includes? ppv "bin/ascii-scan")
          "bin/post-push-verify check 2 must invoke bin/ascii-scan, not re-derive the scan"))
    (testing "and neither carries a second copy of it"
      (is (not (str/includes? hook pattern))
          (str "a copied pattern is a pattern that can drift: the pre-commit twin and the "
               "post-push check would then disagree about what ASCII means, and only one of "
               "them runs before the commit is unfixable."))
      (is (not (str/includes? ppv pattern))
          (str "same for post-push-verify -- including in its header prose, which is where a "
               "reader looks to learn what check 2 does.")))))
