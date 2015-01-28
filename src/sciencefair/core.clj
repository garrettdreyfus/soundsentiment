(ns sciencefair.core
     (:require [clojure.string :as str ] [clojure.test :as test])
     (:require [monger.core :as mg])
     (:require [monger.collection :as mc])
     (:require [monger.conversion :refer [from-db-object]])
     (:require [sciencefair.retrain :refer :all])

     (:gen-class)
    (:import [uk.ac.wlv.sentistrength SentiStrength] )
    (:import [com.mongodb MongoOptions ServerAddress])
    (:import [org.bson.types ObjectId] [com.mongodb DB WriteConcern]))


(defn -main
  [& args]
  (file-to-db "src/sentistrength/EmotionLookupTable.txt" db)
  )

