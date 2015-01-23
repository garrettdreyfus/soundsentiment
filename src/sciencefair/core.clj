(ns sciencefair.core
   (:require [sciencefair.soundcloud :refer :all] [clojure.string :as str ] [clojure.test :as test])
   (:require [monger.core :as mg])
   (:require [monger.collection :as mc])
   (:require [monger.conversion :refer [from-db-object]])

   (:gen-class)
  (:import [uk.ac.wlv.sentistrength SentiStrength] )
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import [org.bson.types ObjectId] [com.mongodb DB WriteConcern]))

(def empties 0)
(def settings {:client-id "86d37185f1e5dcdb022276e7f9801ac3" :client-secret "3089b64302a442d9667b2e63368541b3"})
(def conn (mg/connect))
(def db (mg/get-db conn "monger-test"))
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
(defn pull-down-tracks-genres [genres pagesize offset date_to]
  (if 
    (< empties 10) 
      (doall (pmapcat #(tracks settings {"genres" %, "order" "created_at", "limit" pagesize, "offset" offset "created_at[to]" date_to}) genres))))

(defn useful-format-tracks [tracks kees]
  ;(test/is (> (count tracks) 0))
  (pmap #(assoc (select-keys % kees) :comments (get-comments (% :id))) tracks))


(defn insert-batch-nodups [batch]
  (test/is (> (count batch) 0))
  (let [a (filter #(not (mc/find-one-as-map db "documents" {:id (% :id)})) batch )]
    (if (not= 0 (count a))
      (mc/insert-batch db "documents" a)(def empties (inc empties)))))

(defn formatDate [year month day hour minute sec] (str (str/join "-" [year month day]) " " (str/join ":" [hour minute sec])))
         
(defn harvestTracks 
  [howmany pagesize start genres kees date_to]
      (dorun (pmap #(insert-batch-nodups  (useful-format-tracks (pull-down-tracks-genres genres pagesize (* pagesize (+ start %)) date_to ) kees)) (range 1 howmany))))

(defn fSD [x]
  (if (< x 10) (str "0" x) (str x)))

(defn -main
  [& args]
  (doseq [year (range 2014 2012 -1) ]
    (doseq [month (range 12 1 -1)]
      (println (formatDate (fSD year) (fSD month) (fSD 1) (fSD 1) (fSD 1) (fSD 00)))
      (let [howmany 800 pagesize 10 start 0] 
        (do
          (println "SWITCH")
          (Thread/sleep 1000)
          (def empties 0)
          (dorun 
            (harvestTracks 
              howmany 
              pagesize 
              start 
              '("Hiphop" "hiphop" "jazz" "Electronic" "dubstep" "country" "rock" "rockandroll" "blues" "theblues")  
              [:id :genre :bpm :description :user_id :download_count ] 
              (formatDate (fSD year) (fSD month) (fSD 1) (fSD 6) (fSD 12) (fSD 00))))
          )))))

