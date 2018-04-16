(ns kitchen-async.promise
  (:refer-clojure :exclude [promise resolve let loop while -> ->> some-> some->>])
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

(defmacro ^:private with-error-handling [& body]
  `(try
     ~@body
     (catch :default e#
       (reject e#))))

(defmacro ^:private do* [& exprs]
  (when-not (empty? exprs)
    (letfn [(rec [expr exprs]
              (if (empty? exprs)
                expr
                `(then ~expr (fn [_#] ~(rec (first exprs) (rest exprs))))))]
      (rec (first exprs) (rest exprs)))))

(defmacro do [& body]
  (if (empty? body)
    `(resolve nil)
    `(with-error-handling
       (->promise (do* ~@body)))))

(defmacro let [bindings & body]
  (letfn [(rec [[name init & bindings] body]
            (if-not name
              `(do* ~@body)
              `(then ~init (fn [~name] ~(rec bindings body)))))]
    `(with-error-handling
       (->promise ~(rec bindings body)))))

(defmacro plet [bindings & body]
  (cc/let [pairs (partition 2 2 bindings)
           names (mapv first pairs)
           inits (mapv (fn [[_ e]] `(->promise ~e)) pairs)]
    `(with-error-handling
       (then (goog.Promise.all (into-array ~inits))
             (fn [~names] (do* ~@body))))))

(def ^:private LOOP_FN_NAME (gensym 'loop-fn))

(defmacro loop [bindings & body]
  (cc/let [pairs (partition 2 2 bindings)
           names (mapv first pairs)
           inits (mapv second pairs)
           gensyms (map (fn [_] (gensym)) names)]
    `(letfn [(~LOOP_FN_NAME [~@gensyms]
               (plet [~@(interleave names gensyms)]
                 ~@body))]
       (with-error-handling
         (~LOOP_FN_NAME ~@inits)))))

(defmacro recur [& args]
  (if-not (contains? (:locals &env) LOOP_FN_NAME)
    (throw (ex-info "Can't call kitchen-async.promise/recur outside of kitchen-async.promise/loop" {}))
    (cc/let [gensyms (mapv (fn [_] (gensym)) args)]
      `(plet [~gensyms ~(vec args)]
             (~LOOP_FN_NAME ~@gensyms)))))

(defmacro while [cond & body]
  `(loop [v# ~cond]
     (when v#
       (let [_# (do* ~@body)]
         (kitchen-async.promise/recur ~cond)))))

(defn- interop? [form]
  (and (symbol? form) (= (nth (name form) 0) \.)))

(defmacro -> [x & forms]
  (if forms
    (cc/let [[form & forms] forms]
      `(with-error-handling
         (-> (then ~x
                   ~(cond (seq? form)
                          `(fn [v#] (~(first form) v# ~@(rest form)))

                          (interop? form)
                          `(fn [v#] (~form v#))

                          :else form))
             ~@forms)))
    x))

(defmacro ->> [x & forms]
  (if forms
    (cc/let [[form & forms] forms]
      `(with-error-handling
         (->> (then ~x
                    ~(cond (seq? form)
                           `(fn [v#] (~@form v#))

                           (interop? form)
                           `(fn [v#] (~form v#))

                           :else form))
              ~@forms)))
    x))

(defmacro some-> [expr & forms]
  (cc/let [g (gensym)]
    `(-> ~expr
         ~@(for [form forms]
             `((fn [~g] (if (nil? ~g) nil (cc/-> ~g ~form))))))))

(defmacro some->> [expr & forms]
  (cc/let [g (gensym)]
    `(->> ~expr
          ~@(for [form forms]
              `((fn [~g] (if (nil? ~g) nil (cc/->> ~g ~form))))))))

(defmacro try [& body]
  (cc/let [conformed (s/conform ::specs/try-args body)
           try-body (s/unform ::specs/try-body (:try-body conformed))
           err (gensym 'err)]
    (letfn [(emit-catch [{:keys [error-type error-name catch-body]}]
              [(if (= (first error-type) :default)
                 :else
                 `(instance? ~(second error-type) ~err))
               `(cc/let [~error-name ~err] (do* ~@catch-body))])
            (emit-catches [clauses]
              `((catch*
                 (fn [~err]
                   (cond ~@(mapcat emit-catch clauses)
                         ~@(when-not (some #(= (:error-type %) :default)
                                           clauses)
                             `[:else (reject ~err)]))))))]
      `(cc/-> (~'kitchen-async.promise/do ~@try-body)
              ~@(when-let [clauses (:catch-clauses conformed)]
                  (emit-catches clauses))
              ~@(when-let [clause (:finally-clause conformed)]
                  `((finally*
                     (fn [v#] (do* ~@(:finally-body clause) v#)))))))))

(defmacro catch [classname name & expr*]
  (throw (ex-info "Can't call kitchen-async.promise/catch outside of kitchen-async.promise/try" {})))

(defmacro finally [& expr*]
  (throw (ex-info "Can't call kitchen-async.promise/finally outside of kitchen-async.promise/try" {})))
