(ns sciencefair.core
  (:require [clojure.string :as str ] [clojure.test :as test]
    [monger.core :as mg]
    [monger.collection :as mc]
    [monger.conversion :refer [from-db-object]]
    [sciencefair.retrain :as rt ]
    [sciencefair.sentiment :refer :all]
    [fuzzy-string.core :as fuzzy]
    [incanter.core :refer :all]
    [incanter.charts :refer :all]
    [clojure.math.numeric-tower :as math])
  (:gen-class)
  (:import [uk.ac.wlv.sentistrength SentiStrength] 
    [com.mongodb MongoOptions ServerAddress]
    [org.bson.types ObjectId] [com.mongodb DB WriteConcern]))

(def version "1.0")

(def genres ["hiphop" "jazz" "electronic" "dubstep" "country" "rock" "blues"])

(defn add-arrays
  ([arr1 arr2]
    (map #(test/is (instance? String %)) arr1)
    (let [arr1 (if (instance? String (first arr1)) (map parse-int arr1) arr1) arr2 (if (instance? String (first arr2 )) (map parse-int arr2) arr2)]
      [(+ (first arr1) (first arr2)) (+ (second arr1) (second arr2))]
    ))
  ([]
    (vector 0 0)))

(defn track-total
  [track]
  (let [scores (pmap #(scoreText (% :body)) (track :comments))]
    (reduce add-arrays scores)))

(defn track-scores
  [track ]
  (frequencies (map #(map parse-int (scoreText (% :body))) (track :comments))))

(defn closest-genre
  [genre genres]
  ((apply min-key :score (pmap #(hash-map :score (fuzzy/levenshtein genre %) :genre %) genres)) :genre))

(defn simple-sum
 [trax genre] 
  (hash-map :total (reduce add-arrays (pmap track-total trax)) :count (count trax) :genre genre))

(defn print-return
  [data]
  (println data)
  data)

(def arr-num {[0 0 ] "0"
               [0 1] "1"
               [0 2] "2"
               [0 3] "3"
               [0 4] "4"
               [0 5] "5"
               [1 0] "6"
               [1 1] "7"
               [1 2] "8"
               [1 3] "9"
               [1 4] "10"
               [1 5] "11"
               [2 0] "12"
               [2 1] "13"
               [2 2] "14"
               [2 3] "15"
               [2 4] "16"
               [2 5] "17"
               [3 0] "18"
               [3 1] "19"
               [3 2] "20"
               [3 3] "21"
               [3 4] "22"
               [3 5] "23"
               [4 0] "24"
               [4 1] "25"
               [4 2] "26"
               [4 3] "27"
               [4 4] "28"
               [4 5] "29"
               [5 0] "30"
               [5 1] "31"
               [5 2] "32"
               [5 3] "33"
               [5 4] "34"
               [5 5] "35"})

(defn average-array
  [arr cnt]
  (pmap #(float (/ % cnt)) arr))

(defn data-heat-function 
  [data x y]
    (data ((clojure.set/map-invert arr-num) [x y])))

(defn ready-map-for-mongo
  [data]
  (zipmap (map #(arr-num %) (keys data)) (map int (vals data))))

(defn store-track-sentiscore
  [track]  
  (let [data (hash-map :score (ready-map-for-mongo (track-scores track)) :genre (closest-genre (track :genre) genres) :id (track :id))]
    (mc/insert rt/db "scores" data)
    0))

(defn collect-scores
  []
  (pmap store-track-sentiscore (mc/find-maps rt/db "documents")))

(defn reduce-scores
  []
  (let [data (for [genre genres :let [tracks-scores (pmap #(% :score) (mc/find-maps rt/db "scores" {:genre genre}))]]
                (hash-map 
                  :score (doall (reduce (partial merge-with (comp int +)) tracks-scores))
                  :genre genre
                  :version version))]
    
    (mc/insert-batch rt/db "end_data"  data)))

(defn back-to-map
  [data]
  (zipmap (map #((clojure.set/map-invert arr-num) (name %)) (keys data)) (vals data)))

(defn tile-function
  [data x y]
  (let [veca (vector (math/floor x) (math/floor y)) ]
    (if (contains? data veca) (data veca) 0)))

(def temp (back-to-map { "14"  60, "27"  8, "31"  97, "11"  10, "10"  41, "21"  17, "23"  1, "13"  2572, "26"  21, "16"  18, "7"  2496, "8"  202, "22"  10, "25"  885, "9"  97, "20"  78, "32"  2, "28"  5, "19"  2048, "15"  22, "33"  1 }))

(defn make-heat-map 
  [objs]
  (for [data objs]
    (save 
      (heat-map 
        (partial tile-function (back-to-map (data :score))) 
        0 5 0 5 
        :y-label "Negativity" :x-label "Positivity" :title (data :genre))
    (str (data :genre ) ".png"))))

(defn -main
  [& args]
  (make-heat-map (mc/find-maps rt/db "end_data")))
