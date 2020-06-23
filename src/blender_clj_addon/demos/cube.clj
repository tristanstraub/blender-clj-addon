(ns blender-clj-addon.demos.cube
  (:require [blender-clj-addon.core :refer [with-context]]
            [libpython-clj.python :refer [py..] :as py]))

(defn cubes
  []
  (with-context
    (fn [defaults]
      (let [bpy (py/import-module "bpy")]
        (dotimes [_ 10]
          (let [location (vec (repeatedly 3 #(- (rand-int 20) 10)))]
            (py.. bpy -ops -mesh
                  (primitive_cube_add defaults
                                      :size 3
                                      :location location))))))))

(comment
  (cubes)

  )
