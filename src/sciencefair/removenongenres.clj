;TODO
;[ ] GET ALL COMMENTS
;[ ] SEPERATE WORDS AND FUZZY SEARCH DB
;[ ] INTEGRATE URBANDICTIONARY SEARCHING
;[ ] write word db scores to sentistrength file no DUPS

(ns sciencefair.removenongenres
     (:require  
                [sciencefair.soundcloud :refer :all]
                [clojure.string :as str ]
                [clojure.test :as test]
                [monger.core :as mg]
                [monger.collection :as mc]
                [monger.operators :refer :all]
                [fuzzy-string.core :as fuzzy]
                [monger.conversion :refer [from-db-object]])

     (:gen-class)
    (:import [uk.ac.wlv.sentistrength SentiStrength] )
    (:import [com.mongodb MongoOptions ServerAddress])
    (:import [org.bson.types ObjectId] [com.mongodb DB WriteConcern]))

(def settings {:client-id "86d37185f1e5dcdb022276e7f9801ac3" :client-secret "3089b64302a442d9667b2e63368541b3"})
(def conn (mg/connect))
(def tracksdb (mg/get-db conn "empty"))
(def wordsdb (mg/get-db conn "words"))

(def empties (atom 0))
(defn normalize 
    [string]
    (if-not string
        " "
        (str/lower-case (str/join (str/split string #" "))) ))


(defn closest-genre 
    [value genres threshold]
        (first (filter #(<= (fuzzy/levenshtein (normalize value) % ) threshold)  genres)))

(defn update-genre
    [id genre]
    (mc/update tracksdb "documents" {:id id} {$set {:genre genre}}))

(defn remove-by-id [id]
 (mc/remove tracksdb "documents" {:id id})
)

(defn filter-genres [values genres threshold]
       (dorun (pmap
            (fn [value] 
                (let [closest (closest-genre (value :genre) genres threshold)]
                    (if 
                        closest 
                        (+ 1 1)
                        (remove-by-id (value :id)))
                    closest))
            values)))






