(ns kitchen-async.promise
  (:refer-clojure :exclude [resolve])
  (:require-macros [kitchen-async.promise :as p]
                   [cljs.core.async.macros :refer [go]])
  (:require [clojure.core.async :as a]
            [clojure.core.async.impl.channels :refer [ManyToManyChannel]]
            goog.Promise)
  (:import goog.async.Deferred))

(def ^:private %promise-impl
  (let [init (if (exists? js/Promise)
               js/Promise
               goog.Promise)]
    (atom init)))

(defn promise-impl []
  @%promise-impl)

(defn set-promise-impl! [impl]
  (reset! %promise-impl impl))

(defn resolve [x]
  (let [p (promise-impl)]
    (new p (fn [resolve] (resolve x)))))

(defn reject [x]
  (let [p (promise-impl)]
    (new p (fn [_ reject] (reject x)))))

(declare ->promise)

(defn then
  ([p f]
   (.then (->promise p) f))
  ([p f g]
   (then (then p f) g))
  ([p f g & more]
   (reduce #(then %1 %2) (then p f g) more)))

(defn catch* [p f]
  ;; use .then rather than .catch since goog.Promise doesn't have it
  (.then (->promise p) nil f))

(defn all [ps]
  (goog.Promise.all (clj->js (map ->promise ps))))

(defn race [ps]
  (goog.Promise.race (clj->js (map ->promise ps))))

(defn timeout
  ([ms] (timeout ms nil))
  ([ms v]
   (p/promise [resolve]
     (js/setTimeout #(resolve v) ms))))

(defprotocol Promisable
  (->promise* [this]))

(extend-protocol Promisable
  goog.Promise
  (->promise* [p] p)

  Deferred
  (->promise* [d]
    (p/promise [resolve reject]
      (.addCallbacks d resolve reject)))

  ManyToManyChannel
  (->promise* [c]
    (p/promise [resolve reject]
      (go
        (let [x (a/<! c)]
          (if (instance? js/Error x)
            (reject x)
            (resolve x))))))

  default
  (->promise* [x]
    (resolve x)))

(when (exists? js/Promise)
  (extend-type js/Promise
    Promisable
    (->promise* [p] p)))

(defn ->promise [x]
  (->promise* x))

(defn callback->promise [f & args]
  (p/promise [resolve reject]
    (letfn [(callback [err val]
              (if err
                (reject err)
                (resolve val)))]
      (apply f (concat args [callback])))))
