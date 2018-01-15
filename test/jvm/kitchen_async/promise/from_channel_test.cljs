(ns kitchen-async.promise.from-channel-test
  (:require [clojure.core.async :as a]
            [clojure.test :refer [deftest is async]]
            [kitchen-async.promise :as p]
            kitchen-async.promise.from-channel))

(deftest ->promise-from-chan-test
  (async done
         (let [ch (a/chan)
               p (p/->promise ch)]
           (js/setTimeout #(a/put! ch 42) 0)
           (.then p (fn [x]
                      (is (= 42 x))
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

(deftest let-test
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
