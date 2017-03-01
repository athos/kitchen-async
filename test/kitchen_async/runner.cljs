(ns kitchen-async.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            kitchen-async.core-test))

(doo-tests 'kitchen-async.core-test)
