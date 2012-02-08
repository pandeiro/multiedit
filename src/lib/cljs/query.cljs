(ns query)

(defn $
  "A basic query selector similar to Sizzle/jQuery. Uses document.querySelectorAll(),
  only available in modern browsers"
  ([selector] ($ selector js/document))
  ([selector ctx]
     (let [result (. ctx (querySelectorAll selector))]
       (if (and result (= 1 (.-length result)))
         (aget result 0)
         result))))
