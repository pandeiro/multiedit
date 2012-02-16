(ns query)

(defn $
  "Basic query selector similar to Sizzle/jQuery. Uses document.querySelectorAll(),
  only available in modern browsers"
  ([selector] ($ selector js/document))
  ([selector ctx]
     (let [selector (name selector)
           result   (.querySelectorAll ctx selector)]
       (if (and result (= 1 (.-length result)))
         (aget result 0)
         (array-seq result 0)))))
