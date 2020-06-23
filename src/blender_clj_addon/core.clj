(ns blender-clj-addon.core
  (:require [libpython-clj.require :refer [require-python import-python]]
            [libpython-clj.python :refer [py..] :as py]))

(def ^:dynamic *in-timer?*
  false)

(defonce with-context-agent
  (agent nil))

(defn get-defaults
  []
  (let [bpy       (py/import-module "bpy")
        window    (first (seq (py.. bpy -context -window_manager -windows)))
        area      (first (filter #(= "VIEW_3D" (py.. % -type)) (py.. window -screen -areas)))
        region    (first (filter #(= "WINDOW" (py.. % -type)) (py.. area -regions)))
        workspace (first (filter #(= "Layout" (py.. % -name)) (py.. bpy -data -workspaces)))
        ctx       (py.. bpy -context (copy))]
    (py.. ctx (update (py/->py-dict {"window"    window
                                     "screen"    (py.. window -screen)
                                     "area"      area
                                     "region"    region
                                     "workspace" workspace})))
    ctx))

(defn with-context-transaction
  [p _ f]
  (let [bpy      (py/import-module "bpy")
        timer-fn (fn []
                   (try
                     (deliver p (f (get-defaults)))
                     (catch Exception e
                       (deliver p e)))
                   nil)]
    (py.. bpy -app -timers (register (fn []
                                       (binding [*in-timer?* true]
                                         (timer-fn)))))
    p))

(defn resolve-promise
  [p]
  (let [result @p]
    (if (instance? Exception result)
      (throw result)
      result)))

(defn skip-exceptions
  [f]
  (fn [state & args]
    (try
      (apply f state args)
      (catch Exception e
        (println e)
        state))))

(defn with-context
  [f]
  (if *in-timer?*
    (f (get-defaults))
    (let [p (promise)]
      (send with-context-agent (skip-exceptions (partial with-context-transaction p)) f)
      (resolve-promise p))))
