# kitchen-async
[![Clojars Project](https://img.shields.io/clojars/v/kitchen-async.svg)](https://clojars.org/kitchen-async)
[![CircleCI](https://circleci.com/gh/athos/kitchen-async.svg?style=shield)](https://circleci.com/gh/athos/kitchen-async)

A Promise library for ClojureScript, or a poor man's core.async

## Features

- syntactic support for writing asynchronous code handling Promises as easily as with `async/await` in ECMAScript
- also available on self-hosted ClojureScript environments, such as [Lumo](https://github.com/anmonteiro/lumo)/[Planck](https://github.com/mfikes/planck)
- seamless (opt-in) integration with core.async channels

kitchen-async focuses on the ease of Promise handling, and is not specifically intended to be so much performant. If you would rather like such a library, [promesa](https://github.com/funcool/promesa) or [core.async](https://github.com/clojure/core.async) might be more suitable.

## Example

Assume you are writing some `Promise`-heavy async code in ClojureScript (e.g. [Google's Puppeteer](https://github.com/GoogleChrome/puppeteer) provides such a collection of APIs). Then, if you only use raw JavaScript interop facilities for it, you would have to write something like this:

```clj
(def puppeteer (js/require "puppeteer"))

(-> (.launch puppeteer)
    (.then (fn [browser]
             (-> (.newPage browser)
                 (.then (fn [page]
                          (-> (.goto page "https://clojure.org")
                              (.then #(.screenshot page #js{:path "screenshot.png"}))
                              (.catch js/console.error)
                              (.then #(.close browser)))))))))
```

`kitchen-async` provides more succinct, "direct style" syntactic sugar for those things, which you may find similar to `async/await` in ECMAScript 2017:

```clj
(require '[kitchen-async.promise :as p])

(def puppeteer (js/require "puppeteer"))

(p/let [browser (.launch puppeteer)
        page (.newPage browser)]
  (p/try
    (.goto page "https://clojure.org")
    (.screenshot page #js{:path "screenshot.png"})
    (p/catch :default e
      (js/console.error e))
    (p/finally
      (.close browser))))
```

## Installation

Add the following to your `:dependencies`:

[![Clojars Project](https://clojars.org/kitchen-async/latest-version.svg)](https://clojars.org/kitchen-async)

Or, if you'd rather use an unstable version of the library, you can do that easily via [`deps.edn`](https://clojure.org/guides/deps_and_cli) as well:

```clj
athos/kitchen-async {:git/url "https://github.com/athos/kitchen-async.git" :sha <commit sha hash>}
```

## Usage

kitchen-async provides two major categories of APIs:
- [thin wrapper APIs for JS Promise](#thin-wrapper-apis-for-js-promise)
- [idiomatic Clojure style syntactic sugar](#idiomatic-clojure-style-syntactic-sugar)

You can use all these APIs once you `require` `kitchen-async.promise` ns, like the following:

```clj
(require '[kitchen-async.promise :as p])
```

### Thin wrapper APIs for JS Promise

#### * `p/promise` macro

`p/promise` macro creates a new Promise:

```clj
(p/promise [resolve reject]
  (js/setTimeout #(resolve 42) 1000))  
;=> #object[Promise [object Promise]]
```

This code is equivalent to:

```clj
(js/Promise. 
 (fn [resolve reject]
   (js/setTimeout #(resolve 42) 1000)))
```

#### * `p/then` & `p/catch*`

`p/then` and `p/catch*` simply wrap Promise's `.then` and `.catch` methods, respectively. For example:

```clj
(-> (some-promise-fn)
    (p/then (fn [x] (js/console.log x)))
    (p/catch* (fn [err] (js/console.error err))))
```

is almost equivalent to:

```clj
(-> (some-promise-fn)
    (.then (fn [x] (js/console.log x)))
    (.catch (fn [err] (js/console.error err))))
```

#### * `p/resolve` & `p/reject`

`p/resolve` and `p/reject` wraps `Promise.resolve` and `Promise.reject`, respectively. For example:

```clj
(p/then (p/resolve 42) prn)
```

is equivalent to:

```clj
(.then (js/Promise.resolve 42) prn)
```

#### * `p/all` & `p/race`

`p/all` and `p/race` wraps `Promise.all` and `Promise.race`, respectively. For example:

```clj
(p/then (p/all [(p/resolve 21)
                (p/promise [resolve]
                  (js/setTimeout #(resolve 21) 1000))])
        (fn [[x y]] (prn (+ x y))))
```

is almost equivalent to:

```clj
(.then (js/Promise.all #js[(js/Promise.resolve 42)
                           (js/Promise.
                             (fn [resolve]
                               (js/setTimeout #(resolve 42) 1000)))])
       (fn [[x y]] (prn (+ x y))))
```

#### * Coercion operator and implicit coercion

kitchen-async provides a fn named `p/->promise`, which coerces an arbitrary value to a Promise. By default, `p/->promise` behaves as follows:

- For Promises, acts like `identity` (i.e. returns the argument as is)
- For any other type of values, acts like `p/resolve`

In fact, most functions defined as the thin wrapper API (and the macros that will be described below) implicitly apply `p/->promise` to their input values. Thanks to that trick, you can freely mix up non-Promise values together with Promises:

```clj
(p/then 42 prn) 
;; it will output 42 with no error

(p/then (p/all [21 (p/resolve 21)])
        (fn [[x y]] (prn (+ x y))))
;; this also works well
```

Moreover, since it's defined as a protocol method, it's possible to extend `p/->promise` to customize its behavior for a specific data type. For details, see the section ["Extension of coercion operator"](#extension-of-coercion-operator). Also, the section ["Integration with core.async channels"](#integration-with-coreasync-channels) may help you grasp how we can utilize this capability.

### Idiomatic Clojure style syntactic sugar

kitchen-async also provides variant of several macros (including special forms) in `clojure.core` that return a Promise instead of returning the expression value.

#### `p/do`

`p/do` conjoins the expressions of the body with `p/then` ignoring the intermediate values. For example:

```clj
(p/do
  (expr1)
  (expr2)
  (expr3))
```

is equivalent to:

```clj
(p/then (expr1)
        (fn [_]
          (p/then (expr2)
                  (fn [_] (expr3)))))
```

#### `p/let`

`p/let` is almost the same as `p/do` except that it names each intermediate value with the corresponding name. For example:

```clj
(p/let [v1 (expr1)
        v2 (expr2)]
  (expr3))
```

is equivalent to:

```clj
(p/then (expr1)
        (fn [v1]
          (p/then (expr2)
                  (fn [v2] (expr3)))))
```

Note that the body of the `p/let` is implicitly wrapped with `p/do` when it has multiple expressions in it. For example, when you write some code like:

```clj
(p/let [v1 (expr1)]
  (expr2)
  (expr3))
```

the call to `expr3` will be deferred until `(expr2)` is resolved. To avoid this behavior, you must wrap the body with `do` explicitly:

```clj
(p/let [v1 (expr1)]
  (do
    (expr2)
    (expr3)))
```

#### Threading macros

kitchen-async also has its own `->`, `->>`, `some->` and `some->>`. For example:

```clj
(p/-> (expr) f (g c))
```

is equivalent to:

```clj
(-> (expr)
    (p/then (fn [x] (f x)))
    (p/then (fn [y] (g y c))))
```

and

```clj
(p/some-> (expr) f (g c))
```

is equivalent to:

```clj
(-> (expr)
    (p/then (fn [x] (some-> x f)))
    (p/then (fn [y] (some-> y (g c))))
```

#### Loops

For loops, you can use `p/loop` and `p/recur`:

```clj
(defn timeout [ms v]
  (p/promise [resolve]
    (js/setTimeout #(resolve v) ms)))
    
(p/loop [i (timeout 1000 10)]
  (when (> i 0)
    (prn i)
    (p/recur (timeout 1000 (dec i)))))
    
;; Count down the numbers from 10 to 1
```

Note that the body of the `p/loop` is wrapped with `p/do`, as in the `p/let`.

`p/recur` cannot be used outside of the `p/loop`, and also make sure to call `p/recur` at a tail position.

#### Error handling

For error handling, you can use `p/try`, `p/catch` and `p/finally`:

```clj
(p/try
  (expr)
  (p/catch js/Error e
    (js/console.error e))
  (p/finally
    (teardown)))
```

is almost equivalent to:

```clj
(-> (expr)
    (p/catch* 
     (fn [e]
       (if (instance? js/Error e)
         (js/console.error e)
         (throw e))))
    (p/then (fn [v] (p/do (teardown) v))))
```

Note that the body of the `p/try`, `p/catch` and `p/finally` is wrapped with `p/do`, as in the `p/let`.

`p/catch` and `p/finally` (if any) cannot be used outside of the `p/try`, and also make sure to call them at the end of the `p/try`'s body.

### Extension of coercion operator

(TODO)

### Integration with core.async channels

(TODO)

<!--

## Why not use core.async?

[`core.async`](https://github.com/clojure/core.async) also provides similar async functionalities to `kitchen-async` (and as you may know, it's more powerful in fact), while I believe there are still some rooms where `kitchen-async` shines, such as blah blah blah

-->

## License

Copyright © 2017 Shogo Ohta

Distributed under the Eclipse Public License 1.0.
