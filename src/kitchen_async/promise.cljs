(ns kitchen-async.promise
  (:refer-clojure :exclude [resolve])
  (:require-macros [kitchen-async.promise :as p]
                   [cljs.core.async.macros :refer [go]])
  (:require [clojure.core.async :as a]
            [clojure.core.async.impl.channels :refer [ManyToManyChannel]]
            [goog.Promise :as Promise])
  (:import goog.async.Deferred
           goog.Promise))

(defn resolve [x]
  (Promise.resolve x))

(defn reject [x]
  (Promise.reject x))

(declare ->promise)

(defn then [p f]
  (.then p f))

(defn catch* [p f]
  (.catch p f))

(defn all [ps]
  (Promise.all (clj->js (map ->promise ps))))

(defprotocol Promisable
  (->promise* [this]))

(extend-protocol Promisable
  Promise
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

(defn ->promise [x]
  (->promise* x))

(defn callback->promise [f & args]
  (p/promise [resolve reject]
    (letfn [(callback [err val]
              (if err
                (reject err)
                (resolve val)))]
      (apply f (concat args [callback])))))
