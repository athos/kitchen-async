(ns kitchen-async.specs.promise-macros
  (:require [clojure.spec.alpha :as s]
            [kitchen-async.utils :as utils]))

(defn catch? [sym]
  (or (= sym 'catch)
      (= (utils/fixup-alias sym) 'kitchen-async.promise/catch)))

(s/def ::error-type symbol?)
(s/def ::error-name simple-symbol?)
(s/def ::catch-op
  (s/and symbol? catch?))

(s/def ::non-catch-expr
  (s/and seq?
         (s/cat :op #(not (s/valid? ::catch-op %))
                :args (s/* any?))))
(s/def ::try-body
  (s/* (s/alt :simple-expr (complement seq?)
              :non-catch-expr ::non-catch-expr)))
(s/def ::catch-clause
  (s/and seq?
         (s/cat :catch ::catch-op
                :error-type ::error-type
                :error-name ::error-name
                :catch-body (s/* any?))))
(s/def ::try-args
  (s/cat :try-body ::try-body
         :catch-clauses (s/* ::catch-clause)))

(s/fdef kitchen-async.promise/try
  :args ::try-args)
