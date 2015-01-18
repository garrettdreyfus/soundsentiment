(ns sciencefair.core
   (:require [sciencefair.soundcloud :refer :all] [clojure.string :as str ])
   (:gen-class)
  (:import [uk.ac.wlv.sentistrength SentiStrength] ))

(def settings {:client-id "86d37185f1e5dcdb022276e7f9801ac3" :client-secret "3089b64302a442d9667b2e63368541b3"})

(defn get-attrs
 "Takes a list of maps and an attribute and returns a list of all those values"
 [items attr]
 (map #(% attr) items))

(defn ey [a] 
  (map type a))


(defn get-comments
 "Takes a list or vector of ids and returns a map of ids to comments"
 [id]
  (->>
    (tracks settings {} id "comments")
    (map #(select-keys % '(:body :user_id)))
  ))

(defn analyze-sentiment
 [a]
 (println "hey")
 1.0)

(defn sentize
 [comments]
 (let [sentized {}]
        (map #(assoc sentized (first %) (map analyze-sentiment (second %)))
         comments)))

(defn pull-down-tracks-genres [genres pagesize offset]
  (mapcat #(tracks settings {"genres" %, "order" "created_at", "limit" pagesize, "offset" offset}) genres))

(defn useful-format-tracks [tracks kees]
  (map #(assoc (select-keys % kees) :comments (get-comments (% :id))) tracks)
  )
(defn -main
  "I don't do a whole lot."
  [& args]
  (first (useful-format-tracks (pull-down-tracks-genres '("Hip Hop" "hiphop") 1 0) [:id :genre :bpm :description :user_id]))
)
