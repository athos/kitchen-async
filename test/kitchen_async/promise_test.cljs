(ns kitchen-async.promise-test
  (:require [clojure.core.async :as a]
            [clojure.test :refer [deftest is async]]
            goog.Promise
            [kitchen-async.promise :as p :include-macros true]))

(deftest promise-resolve-test
  (async done
    (.then (p/promise [resolve]
             (resolve 42))
           (fn [x]
             (is (= 42 x))
             (done)))))

(deftest promise-reject-test
  (async done
    (let [msg "something wrong has happened!!"]
      (.then (p/promise [resolve reject]
               (reject (ex-info msg {})))
             nil
             (fn [e]
               (is (= msg (ex-message e)))
               (done))))))

(deftest resolve-test
  (async done
    (.then (p/resolve 42)
           (fn [x]
             (is (= 42 x))
             (done)))))

(deftest reject-test
  (async done
    (let [msg "something wrong has happened!!"]
      (.then (p/reject (ex-info msg {}))
             nil
             (fn [e]
               (is (= msg (ex-message e)))
               (done))))))

(deftest resolved-test
  (async done
    (.then (p/promise []
             (p/resolved 42))
           (fn [x]
             (is (= x 42))
             (done)))))

(deftest rejected-test
  (async done
    (let [msg "something wrong has happened!!"]
      (.then (p/promise []
               (p/rejected (ex-info msg {})))
             nil
             (fn [e]
               (is (= msg (ex-message e)))
               (done))))))

(deftest ->promise-from-number-test
  (async done
    (let [p (p/->promise 42)]
      (is (instance? (p/promise-impl) p))
      (.then p (fn [x]
                 (is (= 42 x))
                 (done))))))

(deftest ->promise-from-chan-test
  (async done
    (let [ch (a/chan)
          p (p/->promise ch)]
      (js/setTimeout #(a/put! ch 42) 0)
      (.then p (fn [x]
                 (is (= 42 x))
                 (done))))))

(deftest then-test
  (async done
    (p/then (p/resolve 42)
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest then-multiple-test
  (async done
    (p/then (p/resolve 40)
            inc
            inc
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest then-from-number-test
  (async done
    (p/then 42
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest catch*-test
  (async done
    (let [msg "something wrong has happened!!"]
      (p/catch* (p/reject (ex-info msg {}))
                (fn [e]
                  (is (= msg (ex-message e)))
                  (done))))))

(deftest catch*-from-chan-test
  (async done
    (let [msg "something wrong has happened!!"
          ch (a/chan)]
      (js/setTimeout #(a/put! ch (ex-info msg {})) 0)
      (p/catch* ch
                (fn [e]
                  (is (= msg (ex-message e)))
                  (done))))))

(deftest all-test
  (async done
    (let [ch (a/chan)
          p (p/all [(p/resolve 42) ch])]
      (a/put! ch 43)
      (p/then p (fn [x]
                  (= [42 43] x)
                  (done))))))

(deftest race-test
  (async done
    (p/then (p/race [(p/resolve 42) (a/chan)])
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest timeout-test
  (async done
    (let [t (js/Date.)]
      (p/then (p/timeout 100 42)
              (fn [x]
                (is (>= (- (js/Date.) t) 100))
                (is 42 x)
                (done))))))

(deftest let-simple-test
  (async done
    (let [p (p/let [x (p/resolve 41)]
              (+ x 1))]
      (is (instance? (p/promise-impl) p))
      (p/then p (fn [x]
                  (is (= 42 x))
                  (done))))))

(deftest let-multiple-test
  (async done
    (let [p (p/let [x (p/resolve 40)
                    y (p/promise [resolve]
                        (js/setTimeout #(resolve (inc x)) 0))]
              (inc y))]
      (p/then p (fn [x]
                  (is (= 42 x))
                  (done))))))

(deftest let-polymorphic-test
  (async done
    (let [ch (a/chan)
          p (p/let [x 40
                    y ch]
              (+ x y))]
      (a/put! ch 2)
      (p/then p (fn [x]
                  (is (= 42 x))
                  (done))))))

(deftest plet-test
  (async done
    (let [ch (a/chan)
          p (p/plet [x (p/resolve 21)
                     y ch]
              (+ x y))]
      (a/put! ch 21)
      (p/then p (fn [x]
                  (is (= 42 x))
                  (done))))))

(deftest loop-test
  (async done
    (let [ch (a/chan)
          p (p/loop [v ch
                     sum 0]
              (if (> v 0)
                (p/recur ch (+ sum v))
                sum))]
      (a/put! ch 20)
      (a/put! ch 22)
      (a/put! ch 0)
      (p/then p (fn [x]
                  (is (= x 42))
                  (done))))))

(deftest while-test
  (async done
    (let [ch (a/chan)
          a (atom 0)
          p (p/while (<= @a 30)
              (p/let [v ch]
                (swap! a + v)))]
      (a/put! ch 20)
      (a/put! ch 22)
      (p/then p (fn [_]
                  (is (= 42 @a))
                  (done))))))

(deftest ->-test
  (async done
    (p/then (p/-> (p/timeout 0 39)
                  inc
                  (+ 2))
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest ->>-test
  (async done
    (p/then (p/->> (p/resolve 41)
                   (p/timeout 0)
                   inc)
            (fn [x]
              (is (= 42 x))
              (done)))))
