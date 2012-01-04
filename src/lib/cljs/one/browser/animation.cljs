(ns ^{:doc "Provides support for basic animations. Allows effects to be
  represented as Clojure data."}
  one.browser.animation
  (:use [one.color :only (color bg-color rgb IColorSource)]
        [one.core :only (start dispose)])
  (:require [goog.style :as style]
            [goog.string :as gstring]
            [goog.fx.AnimationQueue :as queue]
            [goog.fx.easing :as easing]
            [goog.fx.dom :as anim]
            [clojure.browser.event :as event]
            [clojure.browser.dom :as dom]
            [domina :as d]))

(def ^:dynamic *default-time* 1000)

(defn- get-element
  "Accepts a keyword, string or element and returns an
  element. Strings are assumed to be xpath format."
  [e]
  (cond (keyword? e) (d/by-id (name e))
        (string? e) (first (d/nodes (d/xpath e)))
        :else e))

(defprotocol IPosition
  (position [this] "Return the position of the passed object as a 2D array, `[X Y]`."))

(extend-protocol IPosition

  cljs.core.Vector
  (position [this] this)

  js/Array
  (position [this] (js->clj this))

  js/HTMLElement
  (position [this]
    (let [p (js->clj (style/getPosition this) :keywordize-keys true)]
      [(:x p) (:y p)])))

(defprotocol IScroll
  (scroll [this] "Return the scroll position of an element as `[X Y]`."))

(extend-protocol IScroll

  js/Number
  (scroll [this] [0 this])

  cljs.core.Vector
  (scroll [this] this)

  js/HTMLElement
  (scroll [this]
    [(.scrollLeft this) (.scrollTop this)]))

(defprotocol ISize
  (size [this] "Return the size of an element as `[W H]`.")
  (width [this] "Return the width of an element.")
  (height [this] "Return the height of an element."))

(extend-protocol ISize

  js/Number
  (size [this] [this this])
  (width [this] this)
  (height [this] this)

  cljs.core.Vector
  (size [this] this)
  (width [this] (first this))
  (height [this] (second this))

  js/HTMLElement
  (size [this]
    (let [s (js->clj (style/getSize this)
                     :keywordize-keys true)]
      [(:width s) (:height s)]))
  (width [this]
    (width (size this)))
  (height [this]
    (height (size this))))

(defprotocol IOpacity
  (opacity [this] "Return the elements current opacity."))

(extend-protocol IOpacity

  js/String
  (opacity [this]
    (js/parseFloat this))

  js/Number
  (opacity [this] this)

  js/HTMLElement
  (opacity [this]
    (opacity (style/getComputedStyle this "opacity"))))

(extend-type goog.fx.AnimationQueue
  
  one.core/Startable
  (start [this] (.play this ()))
  
  one.core/Disposable
  (dispose [this] (.dispose this ())))

(extend-type goog.fx.dom.PredefinedEffect
  
  one.core/Startable
  (start [this] (.play this ()))
  
  one.core/Disposable
  (dispose [this] (.dispose this ()))
  
  event/EventType
  (event-types [this]
    (into {}
          (map
           (fn [[k v]]
             [(keyword (. k (toLowerCase)))
              v])
           (merge (js->clj goog.fx.Animation.EventType))))))

(defmulti acceleration
  "Get the acceleration function associated with a given
  keyword. Implementing this as a multimethod allows developers to add new
  functions and still represent effects as data."
  identity :default :ease-out)

(defmethod acceleration :ease-out [name]
  easing/easeOut)

(defmethod acceleration :ease-in [name]
  easing/easeIn)

(defmethod acceleration :in-and-out [name]
  easing/inAndOut)

(defn- accel
  "Given a map which represents an effect. Return the acceleration
  function or `nil`."
  [m]
  (when-let [a (:accel m)]
    (if (fn? a)
      a
      (acceleration a))))

(defrecord Effect [effect start end time accel]
  
  one.color.IColorSource
  (color [this] (:end this))
  (bg-color [this] (:end this))
  
  IOpacity
  (opacity [this] (:end this))

  IPosition
  (position [this] (:end this))

  ISize
  (size [this] (:end this))
  (width [this] (width (:end this)))
  (height [this] (height (:end this)))

  IScroll
  (scroll [this] (:end this)))

(defn- effect-dispatch
  "Dispatch function for effect multimethods. Accepts an element and a
  map describing an effect and returns the effect name as a keyword."
  [_ {effect :effect}] effect)

(defmulti standardize
  "Accepts an element and an effect map and returns a standardized
  effect map which must contain the four keys: `:start`, `:end`,
  `:time` and `:accel`.

  The element argument can either be an HTML element or an effect map
  which describes the previous effect."
  effect-dispatch)

(defmethod standardize :color [element m]
  (Effect. :color
           (color (or (:start m) element))
           (color (or (:end m) element))
           (or (:time m) *default-time*)
           (accel m)))

(defmulti effect
  "Accepts an element and a map and returns an effect. The returned
  effect may be run or composed with other effects.

  Available effects include: `:color`, `:fade`, `:fade-in`, `:fade-out`,
  `:fade-in-and-show`, `:fade-out-and-hide`, `:slide`, `:swipe`, `:bg-color`,
  `:resize`, `:resize-width` and `:resize-height`."
  effect-dispatch)

(defmethod effect :color [element m]
  (let [{:keys [start end time accel]} (standardize element m)]
    (goog.fx.dom.ColorTransform. element
                                 (apply array (rgb start))
                                 (apply array (rgb end))
                                 time
                                 accel)))

(comment ;; Color effect examples

  (def label (get-element "//label[@id='name-input-label']/span"))
  (def label-color (color label))

  (def red [255 0 0])
  (def green [0 255 0])

  (start (effect label {:effect :color :end red}))
  (start (effect label {:effect :color :end green}))
  (start (effect label {:effect :color :end label-color}))
  
  (start (bind label
               {:effect :color :end red}
               {:effect :color :end green}
               {:effect :color :end label-color}))
  )

(defmethod standardize :fade [element m]
  (Effect. :fade
           (opacity (or (:start m) element))
           (opacity (:end m))
           (or (:time m) *default-time*)
           (accel m)))

(defmethod effect :fade [element m]
  (let [{:keys [start end time accel]} (standardize element m)]
    (goog.fx.dom.Fade. element start end time accel)))

(defmethod standardize :fade-in [element m]
  (Effect. :fade-in 0 1 (or (:time m) *default-time*) (accel m)))

(defmethod effect :fade-in [element m]
  (let [{:keys [time accel]} (standardize element m)]
    (goog.fx.dom.FadeIn. element time accel)))

(defmethod standardize :fade-out [element m]
  (Effect. :fade-out 1 0 (or (:time m) *default-time*) (accel m)))

(defmethod effect :fade-out [element m]
  (let [{:keys [time accel]} (standardize element m)]
    (goog.fx.dom.FadeOut. element time accel)))

(defmethod standardize :fade-in-and-show [element m]
  (Effect. :fade-in-and-show 0 1 (or (:time m) *default-time*) (accel m)))

(defmethod effect :fade-in-and-show [element m]
  (let [{:keys [time accel]} (standardize element m)]
    (goog.fx.dom.FadeInAndShow. element time accel)))

(defmethod standardize :fade-out-and-hide [element m]
  (Effect. :fade-out-and-hide 1 0 (or (:time m) *default-time*) (accel m)))

(defmethod effect :fade-out-and-hide [element m]
  (let [{:keys [time accel]} (standardize element m)]
    (goog.fx.dom.FadeOutAndHide. element time accel)))

(comment ;; Fade effect examples

  (def label (get-element "//label[@id='name-input-label']/span"))
  (def title (get-element "//div[@id='form']/h1"))
  (def title-opacity (opacity title))
  (def label-opacity (opacity label))

  (start (effect label {:effect :fade :end 0.2}))
  (start (effect title {:effect :fade :end label}))
  (start (effect label {:effect :fade :end label-opacity}))
  (start (effect title {:effect :fade :end title-opacity}))

  (start (effect label {:effect :fade-out}))
  (start (effect label {:effect :fade-in}))

  (start (effect label {:effect :fade-out-and-hide}))
  (start (effect label {:effect :fade-in-and-show}))

  (start (bind label
               {:effect :fade :end 0 :time 2000}
               {:effect :fade :end 1 :time 2000}))
  
  ;; mix fade effects - cannot mix both fade-in-and-show and
  ;; fade-out-and-hide in the same animation.
  (start (apply bind label
                (map #(assoc % :time 2000)
                     [{:effect :fade-out}
                      {:effect :fade :end 1}
                      {:effect :fade :end 0}
                      {:effect :fade-in}
                      {:effect :fade :end 0}
                      {:effect :fade :end 1}])))
  )

(defmethod standardize :bg-color [element m]
  (let [start (or (:start m) element)
        end (or (:end m) element)]
    (Effect. :bg-color
             (bg-color start)
             (bg-color end)
             (or (:time m) *default-time*)
             (accel m))))

(defmethod effect :bg-color [element m]
  (let [{:keys [start end time accel]} (standardize element m)]
    (goog.fx.dom.BgColorTransform. element
                                   (apply array (rgb start))
                                   (apply array (rgb end))
                                   time
                                   accel)))

(comment ;; Background color effect examples

  (def input (get-element :name-input))

  (def red [255 0 0])
  (def green [0 255 0])

  (def input-bg-color (bg-color input))
  (def input-color (color input))

  (start (effect input {:effect :bg-color :end red}))
  (start (effect input {:effect :bg-color :end green}))
  (start (effect input {:effect :bg-color :end input-bg-color}))

  (start (bind input
               {:effect :bg-color :end red}
               {:effect :bg-color :end green}
               {:effect :bg-color :end input-bg-color}))
  )

(defn- calculate-slide-end
  "Calculate the end of a slide based on the start value and the
  passed `:left`, `:right`, `:up` and `:down` values."
  [[x y] m]
  (vector (+ (- x (:left m 0)) (:right m 0))
          (+ (- y (:up m 0)) (:down m 0))))

(defmethod standardize :slide [element m]
  (let [start (position (or (:start m) element))
        end (or (:end m) (calculate-slide-end start m))]
    (Effect. :slide
             start
             end
             (or (:time m) *default-time*)
             (accel m))))

(defmethod effect :slide [element m]
  (let [{:keys [start end time accel]} (standardize element m)]
    (goog.fx.dom.Slide. element
                        (apply array start)
                        (apply array end)
                        time
                        accel)))

(comment ;; Slide effect examples
  
  (def label (get-element "//label[@id='name-input-label']/span"))
  
  (start (effect label {:effect :slide :up 40 :time 100}))
  (start (effect label {:effect :slide :down 40 :time 100}))

  ;; Easing examples
  (start (effect label {:effect :slide :up 200 :accel :ease-out}))
  (start (effect label {:effect :slide :down 200 :accel :ease-in}))

  ;; slide up and then down
  (start (bind label
               {:effect :slide :up 200 :time 2000 :accel :ease-out}
               {:effect :slide :down 200 :time 2000 :accel :ease-in}))
  )

(defmethod standardize :resize-height [element m]
  (let [start (when-let [h (:start m)] [(width element) (height h)])
        end (when-let [h (:end m)] [(width element) (height h)])]
    (Effect. :resize-height
            (size (or start element))
            (size (or end element))
            (or (:time m) *default-time*)
            (accel m))))

(defmethod effect :resize-height [element m]
  (let [{:keys [start end time accel]} (standardize element m)]
    (goog.fx.dom.ResizeHeight. element (height start) (height end) time accel)))

(defmethod standardize :resize-width [element m]
  (let [start (when-let [w (:start m)] [(width w) (height element)])
        end (when-let [w (:end m)] [(width w) (height element)])]
    (Effect. :resize-width
            (size (or start element))
            (size (or end element))
            (or (:time m) *default-time*)
            (accel m))))

(defmethod effect :resize-width [element m]
  (let [{:keys [start end time accel]} (standardize element m)]
    (goog.fx.dom.ResizeWidth. element (width start) (width end) time accel)))

(defmethod standardize :resize [element m]
  (Effect. :resize
           (size (or (:start m) element))
           (size (or (:end m) element))
           (or (:time m) *default-time*)
           (accel m)))

(defmethod effect :resize [element m]
  (let [{:keys [start end time accel]} (standardize element m)]
    (goog.fx.dom.Resize. element
                         (apply array start)
                         (apply array end)
                         time
                         accel)))

(comment ;; Resize examples

  (def button (get-element :greet-button))
  (def button-size (size button))
  (def button-height (height button))
  (def button-width (width button))

  (start (effect button {:effect :resize :end [200 200]}))
  (start (effect button {:effect :resize :end button-size}))

  (start (effect button {:effect :resize-height :end 200}))
  (start (effect button {:effect :resize-height :end button-height}))

  (start (effect button {:effect :resize-width :end 200}))
  (start (effect button {:effect :resize-width :end button-width}))

  (start (bind button
               {:effect :resize :end [200 200]}
               {:effect :resize-height :end 300}
               {:effect :resize-width :end 300}
               {:effect :resize-height :end 200}
               {:effect :resize-width :end 200}
               {:effect :resize :end button-size}))
  )

(defmethod standardize :scroll [element m]
  (let [start (or (:start m) element)
        end (:end m)]
    (Effect. :scroll
             (scroll start)
             (scroll end)
             (or (:time m) *default-time*)
             (accel m))))

(defmethod effect :scroll [element m]
  (let [{:keys [start end time accel]} (standardize element m)]
    (goog.fx.dom.Scroll. element
                         (apply array start)
                         (apply array end)
                         time
                         accel)))

(comment ;; Scroll examples

  (def doc (get-element "//body"))

  ;; Make the window small before trying this.
  (start (effect doc {:effect :scroll :end [500 500]}))
  (start (effect doc {:effect :scroll :end [0 0]}))

  (start (effect doc {:effect :scroll :end 300}))
  (start (effect doc {:effect :scroll :end 0}))

  (start (bind doc
               {:effect :scroll :end [500 500]}
               {:effect :scroll :end [0 0]}
               {:effect :scroll :end 300}
               {:effect :scroll :end 0}))
  )

(defmethod standardize :swipe [element m]
  (let [start (or (:start m) [0 0])
        end (or (:end m) element)]
    (Effect. :swipe
             (size start)
             (size end)
             (or (:time m) *default-time*)
             (accel m))))

(defmethod effect :swipe [element m]
  (let [{:keys [start end time accel]} (standardize element m)]
    (goog.fx.dom.Swipe. element
                        (apply array start)
                        (apply array end)
                        time
                        accel)))

(comment ;; Swipe examples

  (def button (get-element :greet-button))
  (def button-size (size button))

  (style/setStyle button "position" "absolute")
  
  (start (effect button {:effect :swipe :start [100 0] :time 300}))
  (start (effect button {:effect :swipe :start [0 45] :time 300}))
  (start (effect button {:effect :swipe :time 300}))
  
  (style/setStyle button "position" "")
  )

(defn parallel
  "Cause the passed animations to run in parallel."
  [& effects]
  (let [parallel (goog.fx.AnimationParallelQueue.)]
    (doseq [effect effects] (.add parallel effect))
    parallel))

(defn serial
  "Cause the passed animations to be run in order."
  [& effects]
  (let [serial (goog.fx.AnimationSerialQueue.)]
    (doseq [effect effects]
      (.add serial effect))
    serial))

(def ^{:doc "Mapping of specific effects to a more general category of
  effect. For example, there are multiple size and opacity
  effects. Within a single animation, each type of effect should
  influence subsequent effects of the same type."
       :private true}
  effect-types
  {:color             :color
   :fade              :opacity
   :fade-in           :opacity
   :fade-out          :opacity
   :fade-in-and-show  :opacity
   :fade-out-and-hide :opacity
   :bg-color          :bg-color
   :slide             :position
   :resize            :size
   :resize-height     :size
   :resize-width      :size
   :scroll            :scroll
   :swipe             :size})

(defn- standardize-in-env
  "Standardize an effect within the scope of previous effects. Return
  a vector containing the new environment and the standardized
  effect. An effect may be a single map or a vector of maps"
  [element env effect]
  (if (vector? effect)
    (let [coll (map #(standardize-in-env element env %) effect)]
      [(apply merge (map first coll)) (vec (map second coll))])
    (let [effect-type ((:effect effect) effect-types)
          effect (standardize (get env effect-type element) effect)
          env (assoc env effect-type effect)]
      [env effect])))

(defn- standardize-all-effects
  "Accepts an element and a list of effects and vectors of effects and
  returns the same structure with all effect map standardized. Missing
  values will be calculated based on previous effects."
  [element & effects]
  (loop [env {}
         effects effects
         std-effects []]
    (if (seq effects)
      (let [effect (first effects)
            [env effect] (standardize-in-env element env effect)]
        (recur env
               (rest effects)
               (conj std-effects effect)))
      std-effects)))

(defn bind
  "Bind effects to an element returning an animation. Accepts an HTML
  element and any number of effects. Effects can be Maps or a Vector
  of Maps. Each effect is run in order. Each effect within a Vector is
  run in parallel."
  [element & effects]
  (let [element (get-element element)
        effects (apply standardize-all-effects element effects)
        serial (goog.fx.AnimationSerialQueue.)]
    (doseq [sequential-effect effects]
      (if (vector? sequential-effect)
        (let [parallel (goog.fx.AnimationParallelQueue.)]
          (doseq [parallel-effect sequential-effect]
            (.add parallel (effect element parallel-effect)))
          (.add serial parallel))
        (.add serial (effect element sequential-effect))))
    serial))

(comment ;; Bind examples

  (def label-color (color (get-element "//label[@id='name-input-label']/span")))
  (def label (get-element "//label[@id='name-input-label']/span"))
  (def input (get-element :name-input))
  (def red [255 0 0])
  (def green [0 255 0])
  (def blue [0 0 255])
  (def input-bg-color (bg-color input))
  (def input-color (color input))
  (def button (get-element :greet-button))
  (def button-size (size button))

  (def move-label (bind label
                        [{:effect :slide :up 200 :time 2000}
                         {:effect :color :end red :time 2000}]
                        [{:effect :slide :down 200 :time 2000}
                         {:effect :color :end label-color :time 2000}]))
  (start move-label)

  (def background (bind input
                        {:effect :bg-color :end red}
                        {:effect :bg-color :end green}
                        {:effect :bg-color :end blue}
                        {:effect :bg-color :end input-bg-color}))
  (start background)

  ;; Serial and parallel animations on different elements
  (def big-button (bind button
                        {:effect :resize :end [200 200] :time 2000}
                        {:effect :resize :end button-size :time 2000}))
  (start big-button)

  (start (serial move-label background big-button))
  (start (parallel move-label background big-button))
  )

(comment ;; Events
  
  ;; You may listen for "begin" and "finish" events
  (def label-up (bind "//label[@id='name-input-label']/span"
                      {:effect :color :end "#53607b" :time 200}
                      {:effect :slide :up 40 :time 200}))
  (event/listen-once label-up
                     "finish"
                     #(js/alert "Anaimation finished."))
  (start label-up)
  )