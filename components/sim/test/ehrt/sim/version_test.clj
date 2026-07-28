(ns ehrt.sim.version-test
  "The single version source (go-public session, Task 2): `version` is
  read once from resources/version.edn; `git-sha` is a best-effort,
  never-throwing read of .git/HEAD (no subprocess); `generator-sha256`
  is always a valid 64-hex string regardless of whether a .git
  directory is readable."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim.version :as version]))

(deftest version-is-the-pinned-string
  (is (string? version/version))
  (is (= "0.1.0-pre" version/version)))

(deftest git-sha-is-nil-or-a-plausible-hex-commit-id
  (testing "never throws; either nil (no readable .git) or a hex string"
    (let [sha (version/git-sha)]
      (is (or (nil? sha) (re-matches #"[0-9a-f]{40,64}" sha))))))

(deftest generator-sha256-is-always-valid-64-hex
  (testing "regardless of whether git is present, this must stay a
            schema-valid :sha256 value (MirroredManifest's regex)"
    (is (re-matches #"[0-9a-f]{64}" (version/generator-sha256)))))

(deftest generator-sha256-is-deterministic-for-the-same-git-sha
  (is (= (version/generator-sha256) (version/generator-sha256))))

(deftest generator-sha256-is-not-the-all-zero-placeholder-when-git-is-present
  (testing "this checkout has a readable .git -- the hashed-real-sha branch,
            not the placeholder branch, is what's actually exercised here"
    (when (version/git-sha)
      (is (not= (apply str (repeat 64 "0")) (version/generator-sha256))))))
