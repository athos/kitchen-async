(ns kitchen-async.utils
  (:require [cljs.env :as env]))

(defn fixup-alias [sym]
  (or (when-let [prefix (some-> (namespace sym) symbol)]
        (let [ns-name (ns-name *ns*)
              ns (get (:cljs.analyzer/namespaces @env/*compiler*)
                      ns-name)]
          (when-let [full-name (or (get-in ns [:require-macros prefix])
                                   (get-in ns [:requires prefix]))]
            (symbol (name full-name) (name sym)))))
      sym))
