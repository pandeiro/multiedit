(ns query)

(defn $
  "A basic query selector similar to Sizzle/jQuery. Uses document.querySelectorAll(),
  only available in modern browsers"
  ([selector] ($ selector js/document))
  ([selector ctx]
     (let [result (. js/document (querySelectorAll selector ctx))]
       (if (and result (= 1 (.-length result)))
         (aget result 0)
         result))))
