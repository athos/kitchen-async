# kitchen-async
[![Clojars Project](https://img.shields.io/clojars/v/kitchen-async.svg)](https://clojars.org/kitchen-async)
[![CircleCI](https://circleci.com/gh/athos/kitchen-async.svg?style=shield)](https://circleci.com/gh/athos/kitchen-async)

A Promise library for ClojureScript, or a poor man's core.async

It features:
- syntactic support for writing asynchronous code handling Promises as easily as with `async/await` in ECMAScript
- also available on self-hosted ClojureScript environments, such as [Lumo](https://github.com/anmonteiro/lumo)/[Planck](https://github.com/mfikes/planck)
- seamless integration with core.async channels

## Example

Assume you are writing some `Promise`-heavy async code in ClojureScript (e.g. [Google's Puppeteer](https://github.com/GoogleChrome/puppeteer) provides such a collection of APIs). Then, if you only use raw JavaScript interop facilities for it, you would have to write something like this:

```clj
(-> (puppeteer/launch)
    (.then (fn [browser]
             (-> (.newPage browser)
                 (.then (fn [page]
                          (.then (.goto page "https://www.google.com")
                                 #(.screenshot page #js{:path "screenshot.png"}))))
                 (.then #(.close browser))))))
```

`kitchen-async` provides more succinct, "direct style" syntactic sugar for those things, which you may find similar to `async/await` in ECMAScript 2017:

```clj
(require '[kitchen-async.promise :as p])

(p/let [browser (puppeteer/launch)
        page (.newPage browser)]
  (.goto page "https://www.google.com")
  (.screenshot page #js{:path "screenshot.png"})
  (.close browser))
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

Moreover, since it's defined as a protocol method, it's possible to extend `p/->promise` to customize its behavior for a specific data type. For details, see the section ["Extension of coercion operator"](#extension-of-coercion-operator). The section ["Integration with core.async channels"](#integration-with-coreasync-channels) may also help you grasp how we can utilize this capability.

### Idiomatic Clojure style syntactic sugar

#### `p/do`
#### `p/let`
#### Threading macros
#### `p/loop`
#### `p/while`
#### Error handling

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
