(ns sciencefair.sentiment
   (:require [sciencefair.soundcloud :refer :all]
             [clojure.string :as str ]
             [clj-http.client :as client]
            [fuzzy-string.core :as fuzzy]
             [cheshire.core :refer :all]
             [clojure.string :refer [join]])

   (:gen-class)
  (:import [uk.ac.wlv.sentistrength SentiStrength]))


(defn parse-int [s]
     (Integer. (re-find  #"\d+" s )))

(def analyzer (new uk.ac.wlv.sentistrength.SentiStrength (into-array ["sentidata" "./src/sentistrength/" "text" "initialize+this+string"] )))

(defn scoreText
  [comnt]
   (str/split (.computeSentimentScores analyzer comnt) #"\s"))

(defn urbanApiCall [term]
  (->
    (client/get (str "http://api.urbandictionary.com/v0/define?term=" term ))
    (:body)
    (parse-string true)))

(defn parseResponse
  [response]
    (map #(% :definition) (response :list)))

(defn getDefinitions
  [term]
    (parseResponse (urbanApiCall term)))

(defn averageArrays
  [arr]
    (if (> (count arr ) 0)
      [(/ (reduce + (map first arr)) (count arr))
      (/ (reduce + (map second arr)) (count arr))]
      0))

(defn std-dev [samples]
    (if (> (count samples) 0) 
        (let [n (count samples)
              mean (/ (reduce + samples) n)
              intermediate (map #(Math/pow (- %1 mean) 2) samples)]
                (Math/sqrt 
                       (/ (reduce + intermediate) n))) 
        0))    

(defn std-devArrays
 [arr] 
  [(std-dev (map first arr)) (std-dev (map second arr))] )

(defn smartAverage 
  [arr stds]
  (let [average (averageArrays arr) newarr '[] deviation (std-devArrays arr)]
      (filter (complement nil?) (map #(if 
              (or
                  (< (- (first %) (first average)) (* (first deviation) stds ))
                  (< (- (second %) (second average)) (* (second deviation) stds) ))
              % )
           arr))))

(defn wordSentiment 
  [word]
  (map #(map parse-int (scoreText %)) (getDefinitions word)))

;(averageArrays (wordSentiment "hot") )
