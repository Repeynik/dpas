;; Given an alphabet in form of a list containing 1-character strings and a number N. Define a function that
;; returns all the possible strings of length N based on this alphabet and containing no equal subsequent
;; characters.
;; Use map/reduce/filter/remove operations and basic operations for lists such as str, cons, .concat, etc.
;; No recursion, generators or advanced functions such as flatten!
;; Example: for the alphabet ("а" "b " "c") and N=2 the result bust be ("ab" "ac" "ba" "bc" "ca" "cb") up to
;; permutation.

(defn generate-strings [alphabet n]
  (cond
    (<= n 0)
    '()

    (= n 1)
    (map str alphabet)

    :else
    (let [initial (map str alphabet)
          step (fn [strings]
                 (apply concat
                        (map (fn [s]
                               (map (fn [ch]
                                      (str s ch))
                                    (filter (fn [ch]
                                              (not= (last s) (first ch)))
                                            alphabet)))
                             strings)))]
      (reduce (fn [acc _] (step acc))
              initial
              (range (dec n))))))

(println (generate-strings '("a", "b", "c") 2))


