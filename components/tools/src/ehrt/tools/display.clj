(ns ehrt.tools.display
  "Pretty rendering for eyes, never wire format (ADR-0013): the render
  layer `ehrt show` sits on top of. Deliberately shaped for reuse by
  the future corpus player's ticker sink (docs/dev/ design lineage,
  founding chat 2026-07-27): the per-message renderer below takes one
  message and knows nothing about a stream; the stream-level split/join
  lives in `render-er7-stream`, not inside the per-message renderer, so
  a future pacer can call the per-message renderer directly, once per
  event, at whatever cadence it computes.

  The ER7 message-boundary rule is the same one
  `ehrt.corpus-io.framing`'s own `:er7-multi` codec uses
  (MSH-line-start detection) -- this namespace calls `framing/decode`
  directly rather than inventing a second splitter; a test asserts the
  two agree on the shared er7-multi fixtures.

  Display renderings are NOT wire format: LF-joined ER7 segments are
  nonconformant ER7 by construction (the wire separator is a bare CR,
  ADR-0013's own display-vs-wire doctrine) -- never pipe this
  namespace's own output anywhere a real HL7 v2 consumer sits. Every
  function here is pure and read-only by construction: none of them
  touch the filesystem."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [ehrt.corpus-io.interface :as corpus-io]
            [ehrt.kernel.interface :as kernel]))

(defn render-er7-message
  "One ER7 message (a string, CR-separated segments) -> segments one
  per line: CR -> LF, with exactly one trailing separator stripped when
  present. This is the exact call shape the future player's ticker
  sink will make -- one message in, one rendered block out, no
  knowledge of a stream."
  [message]
  (let [trimmed (if (str/ends-with? message "\r")
                  (subs message 0 (dec (count message)))
                  message)]
    (str/replace trimmed "\r" "\n")))

(defn split-er7-multi
  "Splits raw ER7 text into a seq of message strings via the same
  MSH-line-start boundary `ehrt.corpus-io.framing/decode`'s
  `:er7-multi` codec uses -- not a second splitter. Public: this is
  also the corpus player's own input-adapter seam (ADR-0014), reused
  from the CLI base through this interface rather than re-split a
  second time. Returns kernel/ok [message strings...], or
  framing/decode's own :malformed-er7-multi-frame rejection,
  propagated unchanged."
  [content]
  (let [decoded (corpus-io/decode :er7-multi (.getBytes ^String content "UTF-8"))]
    (if-not (kernel/ok? decoded)
      decoded
      (kernel/ok (mapv #(String. ^bytes % "UTF-8") (:payload decoded))))))

(defn render-er7-stream
  "content (raw ER7 text: one or more MSH-led messages) -> kernel/ok a
  single rendered string, each message through `render-er7-message`,
  joined by a blank line between messages -- the split and the join
  live here, in the stream layer, never inside `render-er7-message`.
  Propagates `split-er7-multi`'s own rejection (no MSH-led message
  found) unchanged."
  [content]
  (let [messages-result (split-er7-multi content)]
    (if-not (kernel/ok? messages-result)
      messages-result
      (kernel/ok (str/join "\n\n" (map render-er7-message (:payload messages-result)))))))

(defn render-fhir-json
  "FHIR JSON text -> kernel/ok pretty-printed JSON text. Rejects
  :malformed-fhir-json when content doesn't parse."
  [content]
  (try
    (kernel/ok (json/write-str (json/read-str content) :indent true))
    (catch Exception e
      (kernel/rejected :malformed-fhir-json
                        {:hint (str "not parseable JSON: " (or (ex-message e) (str e)))}))))
