(ns mt.generative
  (:refer-clojure :exclude [remove])
  (:import [java.util Random Set]))

;; Bad Inc {{{

(defn bad-inc
  [n]
  (if (zero? (mod n 37))
    n
    (+ n 1)))

(def r (Random.))

(defn gen-int [] (.nextInt r))

;; For 1000 random ints, check whether the incremented result is 1 greater
;; than the input.
(defn test-inc-random-ints
  []
  (doseq [n-to-test (repeatedly 1000 gen-int)]
    (let [res (bad-inc n-to-test)]
      (when-not (= 1 (- res n-to-test))
        (throw (ex-info "FAIL" {:n n-to-test}))))))

;; }}}

;; Setup {{{

;; Kinda a set
(defprotocol SetIsh
  (add [this item])
  (remove [this item])
  (as-list [this]))

(deftype BadSet [items]
  SetIsh
  (add [_ item] (BadSet. (update items (.hashCode item) #(if % (conj % item) [item]))))
  (remove [_ item] (BadSet. (dissoc items (.hashCode item))))
  (as-list [_] (into () (comp (map second) cat (distinct)) items)))

(def empty-bad-set (BadSet. {}))

;; }}}

;; A test! {{{
(defn test-set
  []
  (assert (= (as-list empty-bad-set) ()))
  (assert (= (as-list (add empty-bad-set 123)) '(123)))
  (assert (= (as-list (-> empty-bad-set (add 123) (add 456) (remove 123)))
                      '(456)))
  (assert (= (-> empty-bad-set (add "abc") (add "def") (add "whoo") as-list sort)
             (-> empty-bad-set (add "abc") (add "def") (add "whoo") as-list sort)))
  'Yay)
#_(test-set)
;; }}}

;; Boring stuff {{{
(defn string-gen
  []
  (let [chars    (map char (range 32 127))
        one-char #(->> chars shuffle first)
        n-chars  (inc (rand-int 4))]
    (->>
      (for [_ (range n-chars)]
        (one-char))
      (apply str))))

#_(string-gen)

(extend-type clojure.lang.IPersistentSet
  SetIsh
  (add [this item] (conj this item))
  (remove [this item] (disj this item))
  (as-list [this] (seq this)))
;; }}}

;; Actions {{{
(defn apply-action
  [set [f arg]]
  (let [actualf @(resolve f)]
    (actualf set arg)))

(defn some-actions
  []
  (for [_ (range 10000)]
    (if (< (rand) 0.5)
      ['add (string-gen)]
      ['remove (string-gen)])))

(defn apply-actions->list
  [s actions]
  (->> (reduce apply-action s actions)
       as-list
       sort))

(defn consistent?
  [actions]
  (= (apply-actions->list #{} actions)
     (apply-actions->list empty-bad-set actions)))
;; }}}

;; Test actions {{{
(defn test-set-is-a-set
  []
  (loop [n 1000000]
    (if (> n 0)
      (let [actions (some-actions)]
        (when-not (consistent? actions)
          (throw (ex-info "BLIMEY" {:actions actions})))
        (recur (dec n)))
      "Boo")))
;; }}}

;; {{{ Shrinkining
(defn shrink-that
  [actions]
  (loop [shrinked-actions actions n 10000]
    (if (<= n 0)
      shrinked-actions
      (let [to-remove    (rand-int (count shrinked-actions))
            new-actions  (concat (take to-remove shrinked-actions)
                                 (drop (inc to-remove) shrinked-actions))]
        (if-not (consistent? new-actions)
          (recur new-actions (dec n))
          (recur shrinked-actions (dec n)))))))
;; }}}

;; Execute in a REPL {{{
(comment
  ;; Find series of actions that breaks the model
  (def bad (try (test-set-is-a-set)
             (catch clojure.lang.ExceptionInfo e
               (-> e ex-data :actions))))
  ;; Shrink those 10000 actions down to the ones that actually triggered the
  ;; bug. N.B. Inefficient / naïve! Takes a while
  (def shrunk (shrink-that bad))
  ;; Demonstrate this has found the intentional hashCode bug
  (map #(.hashCode (second %)) shrunk))
;; }}}
