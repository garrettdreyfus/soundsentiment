(ns sciencefair.core
   (:require [sciencefair.soundcloud :refer :all] [clojure.string :as str ])
   (:require [monger.core :as mg])
   (:require [monger.collection :as mc])

   (:gen-class)
  (:import [uk.ac.wlv.sentistrength SentiStrength] )
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import [org.bson.types ObjectId] [com.mongodb DB WriteConcern]))

(def settings {:client-id "86d37185f1e5dcdb022276e7f9801ac3" :client-secret "3089b64302a442d9667b2e63368541b3"})

(defn get-attrs
 "Takes a list of maps and an attribute and returns a list of all those values"
 [items attr]
 (map #(% attr) items))

(defn get-comments
 "Takes a list or vector of ids and returns a map of ids to comments"
 [id]
  (->>
    (tracks settings {} id "comments")
    (map #(select-keys % '(:body :user_id)))))

(defn pull-down-tracks-genres [genres pagesize offset]
  (mapcat #(tracks settings {"genres" %, "order" "created_at", "limit" pagesize, "offset" offset,"created_at[to]" "2015-01-18 11:20:00"}) genres))

(defn useful-format-tracks [tracks kees]
  (map #(assoc (select-keys % kees) :comments (get-comments (% :id))) tracks))

(defn harvestTracks 
  [howmany pagesize genres kees ]
  (let [conn (mg/connect) db (mg/get-db conn "monger-test")]
    (doseq [i (range 1 howmany)]
      (mc/insert-batch db "documents" (useful-format-tracks (pull-down-tracks-genres genres pagesize (* pagesize (- i 1)) ) kees))
    )
  ))

(defn -main
  [& args]

  (let [conn (mg/connect) db (mg/get-db conn "monger-test")]
    (count (mc/find-maps db "documents"))
    ;(map #(% :id) (mc/find-maps db "documents"))
      ;(mc/remove db "documents")
  )
  (harvestTracks 3 10 '("Hip Hop" "hiphop")  [:id :genre :bpm :description :user_id :download_count ] )

)
