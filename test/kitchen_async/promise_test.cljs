(ns kitchen-async.promise-test
  (:require [clojure.core.async :as a]
            [clojure.test :refer [deftest is async]]
            [kitchen-async.promise :as p :include-macros true])
  (:import goog.Promise))

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
      (is (instance? Promise p))
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
