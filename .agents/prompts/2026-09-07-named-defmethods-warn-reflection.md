# Session prompt — 2026-09-07 — named defmethods for decide and evolve; reflection warnings on

Archived verbatim. Record:
[`../session-records/2026-09-07-named-defmethods-warn-reflection.md`](../session-records/2026-09-07-named-defmethods-warn-reflection.md).

---

Session: named defmethods for decide and evolve; reflection warnings on -- 2026-09-07

Context: decide (32 methods, decide.clj:114) and evolve (27, evolve.clj:91) compile to
gensym classes (decide$eval7081$fn__7084); sites 5 and 6 attributed profile frames by
resolving eval ordinals against source, and site 6 found that deleting two anonymous
fns shifted every later ordinal by 13. defmethod's fn-tail accepts a name, which puts
it in the class (decide$decide_merge): zero runtime cost, zero output effect, stable
attribution for site 7's profiles. Rider: *warn-on-reflection* in dev. Output-identical;
bracket-proven; no measurement. Fresh clone at 836c764b or later. WSL only. No
sub-agents. R-sentinel.

Read first: AGENTS.md; SKILL.md (:87-88, :142); decide.clj:110-120; evolve.clj:85-95; one
method of each; the site-6 record (the ordinal shift); profile-cell.sh:70-110 (the
attribution note); deps.edn:18 (:dev extra-paths "development/src" -- currently empty).

Author rulings, verbatim and binding:
 R-named-methods (D2, 2026-09-07): "every decide method is named decide-<kind>, every
   evolve method evolve-<kind>, kind spelled as the dispatch keyword's name; bodies,
   arglists, and docstrings unchanged."
 R-warn-reflection (D4): "development/src/user.clj sets *warn-on-reflection* true; the
   :dev alias loads it; any warning it surfaces is RECORDED, not fixed, this session."
 R-move-not-improve, R-pins, R-edit, R-cap (standing). No measurement this session.

Steps (one gate each; commit message given):
1. bin/preflight; bracket IDENTICAL at the clean tip. No commit.
2. Name all 59 methods per R-named-methods (script file, R-edit: a sed that matches
   `^(defmethod decide :kind$` shapes is safer than hand edits; verify the count 32+27
   before and after; verify with `poly check` + a real -M:dev load that every method's
   class now carries the name). Invariant: nothing but the name token changes on each
   form -- `git diff --stat` is two files, and `git diff -w` shows one token per method.
   Gate: bracket IDENTICAL + make test. Commit: "engine: name every decide and evolve
   method for stable profile attribution (D2)".
3. development/src/user.clj per R-warn-reflection; load it via -M:dev and run the
   sim-engine and sim-check bricks once; list every reflection warning (namespace,
   line, target) in the session record; none fixed. Gate: make test.
   Commit: "dev: warn-on-reflection in the dev user namespace (D4)".
4. profile-cell.sh:70-110 attribution note updated to say frames now read by name and
   the ordinal resolution is retired (R-edit). Gate: none (comment). Rides step 5's commit.
5. Session record (the warning list; the count check); archive prompt; close ceremony;
   push; verify CI by sentinel; close-marker commit.
   Commit: "docs: record CI success at <sha> -- named methods close".
