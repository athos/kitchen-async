(ns kitchen-async.promise
  (:refer-clojure :exclude [promise let])
  (:require [clojure.core :as cc]))

(defmacro promise [[resolve reject] & body]
  (cc/let [resolve (or resolve '&resolve)
           reject (or reject '&reject)]
    `(js/Promise.
       (fn [~resolve ~reject]
         ~@body))))

(defmacro resolved [x]
  `(~'&resolve ~x))

(defmacro rejected [x]
  `(~'&reject ~x))

(defmacro let [bindings & body]
  ((reduce (fn [f [name expr]]
             (cc/let [p (f expr)]
               (fn [e]
                 `(then ~p (fn [~name] ~e)))))
           identity
           (partition 2 bindings))
   `(do ~@body)))

(defmacro plet [[name expr] & body]
  `(then (js/Promise.all ~expr)
         (fn [~name]~@body)))
