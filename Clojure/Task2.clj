;; Define the infinite sequence of prime numbers. Use Sieve of Eratosthenes algorithm with infinite cap.
;; Cover code with unit tests.

(ns Task2
  (:require [clojure.test :refer :all]))


(defn sieve [s]
  (lazy-seq
   (let [p (first s)]
     (cons p
           (sieve (filter #(not= 0 (mod % p))
                          (rest s)))))))

(println (take 10 (sieve (iterate inc 2))))

(deftest first-primes-test
  (testing "Первые 10 простых чисел"
    (is (= [2 3 5 7 11 13 17 19 23 29]
           (take 10 (sieve (iterate inc 2)))))))

(deftest more-primes-test
  (testing "11-15-е простые числа"
    (is (= [31 37 41 43 47]
           (->> (sieve (iterate inc 2))
                (drop 10)
                (take 5))))))

(deftest prime-property-test
  (testing "Каждое из первых 20 простых не делится на числа от 2 до p-1"
    (doseq [p (take 20 (sieve (iterate inc 2)))]
      (is (every? #(not= 0 (mod p %))
                  (range 2 p))))))

(run-tests)
