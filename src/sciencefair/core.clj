(ns sciencefair.core
   (:require [sciencefair.soundcloud :refer :all] [clojure.string :as str ])
   (:require [monger.core :as mg])
   (:require [monger.collection :as mc])
   (:require [monger.conversion :refer [from-db-object]])

   (:gen-class)
  (:import [uk.ac.wlv.sentistrength SentiStrength] )
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import [org.bson.types ObjectId] [com.mongodb DB WriteConcern]))

(def settings {:client-id "8237ee81242a696b85cf2a39b62e709a" :client-secret "a861b572812862f20d4f17f5f7c1f6dd"})

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
(defn pmapcat [f batches]
    (->> batches
             (pmap f)
             (apply concat)
             doall))
(defn pull-down-tracks-genres [genres pagesize offset]
  (print "__" pagesize offset "___\n")
  (pmapcat #(tracks settings {"genres" %, "order" "created_at", "limit" pagesize, "offset" offset "created_at[to]" "2015-01-19 09:40:00"}) genres))

(defn useful-format-tracks [tracks kees]
  (map #(assoc (select-keys % kees) :comments (get-comments (% :id))) tracks))

(defn insert-batch-nodups [batch]
    (let [conn (mg/connect) db (mg/get-db conn "monger-test") a (filter #(not (mc/find-one-as-map db "documents" {:id (% :id)})) batch )]
      (if (not (= 0 (count a)))
        (mc/insert-batch db "documents" a))))

         
(defn harvestTracks 
  [howmany pagesize start genres kees ]
      (dorun (pmap #(insert-batch-nodups  (useful-format-tracks (pull-down-tracks-genres genres pagesize (* pagesize (+ start %)) ) kees)) (vec (range 1 howmany))))
)

(defn -main
  [& args]
  (let [howmany 800 pagesize 10 start 0] 
    (harvestTracks howmany pagesize start '("Electronic" "House" )  [:id :genre :bpm :description :user_id :download_count ] ))
)

