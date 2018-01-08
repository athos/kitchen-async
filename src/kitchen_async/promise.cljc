(ns kitchen-async.promise
  (:refer-clojure :exclude [promise let loop while -> ->>])
  (:require [clojure.core :as cc]
            [clojure.spec.alpha :as s]
            [kitchen-async.specs.promise-macros :as specs]))

(defmacro promise [[resolve reject] & body]
  (cc/let [params (cond-> []
                    resolve (conj resolve)
                    reject (conj reject))]
    `(cc/let [p# (promise-impl)]
       (new p#
         (fn ~params
           ~@body)))))

(defmacro let [bindings & body]
  (letfn [(rec [[name init & bindings] body]
            (if-not name
              `(do ~@body)
              `(then ~init (fn [~name] ~(rec bindings body)))))]
    `(->promise ~(rec bindings body))))

(defmacro plet [bindings & body]
  (cc/let [pairs (partition 2 2 bindings)
           names (mapv first pairs)
           inits (mapv (fn [[_ e]] `(->promise ~e)) pairs)]
    `(then (goog.Promise.all (cc/clj->js ~inits))
           (fn [~names] ~@body))))

(def ^:private LOOP_FN_NAME (gensym 'loop-fn))

(defmacro loop [bindings & body]
  (cc/let [pairs (partition 2 2 bindings)
           names (mapv first pairs)
           inits (mapv second pairs)
           gensyms (map (fn [_] (gensym)) names)]
    `(letfn [(~LOOP_FN_NAME [~@gensyms]
               (plet [~@(interleave names gensyms)]
                 ~@body))]
       (~LOOP_FN_NAME ~@inits))))

(defmacro recur [& args]
  (cc/let [gensyms (mapv (fn [_] (gensym)) args)]
    `(plet [~gensyms ~(vec args)]
       (~LOOP_FN_NAME ~@gensyms))))

(defmacro while [cond & body]
  `(loop [v# ~cond]
     (when v#
       (let [_# (do ~@body)]
         (kitchen-async.promise/recur ~cond)))))

(defmacro -> [x & forms]
  (if forms
    (cc/let [[form & forms] forms]
      `(-> (then ~x
                 ~(if (seq? form)
                    `(fn [v#] (~(first form) v# ~@(rest form)))
                    form))
           ~@forms))
    x))

(defmacro ->> [x & forms]
  (if forms
    (cc/let [[form & forms] forms]
      `(->> (then ~x
                  ~(if (seq? form)
                     `(fn [v#] (~@form v#))
                     form))
            ~@forms))
    x))

(defmacro try [& body]
  (cc/let [conformed (s/conform ::specs/try-args body)
           try-body (s/unform ::specs/try-body (:try-body conformed))
           err (gensym 'err)]
    (letfn [(emit-catch [{:keys [error-type error-name catch-body]}]
              [(if (= (first error-type) :default)
                 :else
                 `(instance? ~(second error-type) ~err))
               `(cc/let [~error-name ~err] ~@catch-body)])]
      `(cc/-> (try
                ~@try-body
                (catch :default e#
                  (reject e#)))
              (catch*
                (fn [~err]
                  (cond ~@(mapcat emit-catch (:catch-clauses conformed))
                        :else (reject ~err))))
              ~@(when-let [clause (:finally-clause conformed)]
                  `((then (fn [_#] ~@(:finally-body clause)))
                    (catch* (fn [_#] ~@(:finally-body clause)))))))))

(defmacro catch [classname name & expr*]
  (throw (ex-info "Can't call kitchen-async.promise/catch outside of kitchen-async.promise/try" {})))

(defmacro finally [& expr*]
  (throw (ex-info "Can't call kitchen-async.promise/finally outside of kitchen-async.promise/try" {})))
