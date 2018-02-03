(ns kitchen-async.promise-test
  (:require [clojure.test :refer [deftest is async]]
            goog.Promise
            [kitchen-async.promise :as p]))

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

(deftest ->promise-from-number-test
  (async done
    (let [p (p/->promise 42)]
      (is (instance? (p/promise-impl) p))
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

(deftest timeout-test
  (async done
         (let [t (js/Date.)]
           (p/then (p/timeout 100 42)
                   (fn [x]
                     (is (>= (- (js/Date.) t) 100))
                     (is 42 x)
                     (done))))))

(deftest all-test
  (async done
    (let [p (p/all [(p/resolve 42) (p/timeout 0 43)])]
      (p/then p (fn [x]
                  (= [42 43] x)
                  (done))))))

(deftest race-test
  (async done
    (p/then (p/race [(p/resolve 42) (p/promise [])])
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest do-test
  (let [v (volatile! 40)
        p (p/do (p/promise [res]
                  (vswap! v + 1)
                  (res nil))
                (p/promise [res]
                  (vswap! v + 2)
                  (res 42)))]
    (async done
      (p/then p (fn [x]
                  (is (= 42 x))
                  (is (= 43 @v))
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

(deftest plet-test
  (async done
    (let [p (p/plet [x (p/resolve 21)
                     y (p/timeout 0 21)]
              (+ x y))]
      (p/then p (fn [x]
                  (is (= 42 x))
                  (done))))))

(deftest loop-test
  (async done
    (let [v (volatile! 18)
          f (fn []
              (vswap! v + 2)
              (p/resolve @v))
          p (p/loop [v (f)
                     sum 0]
              (if (> sum 40)
                sum
                (p/recur (f) (+ sum v))))]
      (p/then p (fn [x]
                  (is (= x 42))
                  (done))))))

(deftest while-test
  (async done
    (let [i (volatile! 0)
          f #(p/resolve (< % 2))
          p (p/while (f @i)
              (vswap! i inc))]
      (p/then p (fn [_]
                  (is (= 2 @i))
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

(deftest some->-non-nil-test
  (async done
    (p/then (p/some-> (p/resolve 43)
                      inc
                      (- 2))
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest some->-nil-test
  (async done
    (p/then (p/some-> (p/resolve 43)
                      inc
                      ((constantly nil))
                      (- 2))
            (fn [x]
              (is (= nil x))
              (done)))))

(deftest some->>-non-nil-test
  (async done
    (p/then (p/some->> (p/resolve 1)
                       inc
                       (- 44))
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest some->>-nil-test
  (async done
    (p/then (p/some->> (p/resolve 1)
                       inc
                       ((constantly nil))
                       (- 44))
            (fn [x]
              (is (= nil x))
              (done)))))

(deftest try-success-test
  (async done
    (p/then (p/try 42)
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest try-fail-test
  (async done
    (let [msg "somthing wrong has happened!!"]
      (p/catch* (p/try (throw (ex-info msg {})))
                (fn [e]
                  (is (= msg (ex-message e)))
                  (done))))))

(deftest try-catch-test
  (async done
    (let [msg "something wrong has happened!!"]
      (p/try
        (throw (js/RangeError. msg))
        (p/catch ExceptionInfo e
          (assert false "not reached"))
        (p/catch :default e
          (is (= (ex-message e) msg))
          (done))))))

(deftest try-finally-test
  (async done
    (p/then (p/try
              (p/resolve 41)
              (p/catch :default e
                (assert false "not reached"))
              (p/finally 42))
            (fn [x]
              (is (= 42 x))
              (done)))))

(deftest try-catch-finally-test
  (async done
    (let [v (volatile! 39)]
      (p/then (p/try
                (throw (ex-info "something wrong has happened!!" {}))
                (p/catch :default e
                  (vswap! v + 1))
                (p/finally
                  (vswap! v + 2)
                  @v))
              (fn [x]
                (is (= 42 x))
                (done))))))

(deftest nested-try-catch-test
  (async done
    (let [msg "something wrong has happened!!"]
      (p/try
        (p/try
          (throw (ex-info msg {}))
          (p/catch js/RangeError e
            (assert false "not reached")))
        (p/catch :default e
          (is (= msg (ex-message e)))
          (done))))))
