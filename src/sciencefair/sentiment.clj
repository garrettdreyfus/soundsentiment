(ns sciencefair.sentiment
   (:require [sciencefair.soundcloud :refer :all]
             [clojure.string :as str ]
             [clj-http.client :as client]
             [fuzzy-string.core :as fuzzy]
             [cheshire.core :refer :all]
             [sciencefair.core :refer :all]
             [clojure.string :refer [join]])

   (:gen-class)
  (:import [uk.ac.wlv.sentistrength SentiStrength]))


(def prepositions 
  (str/split "about below in spite of regarding above beneath instead of  since according to  beside  into  through across  between like  throughout after beyond  near  to against but of  toward along by  off under amid  concerning  on  underneath among down  on account of until around  during  onto  up at  except  out upon atop  for out of  with because of  from  outside within before  in  over  without behind  inside  past" #"\s")
  )
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
  [
    (/ (reduce + (map first arr)) (count arr))
    (/ (reduce + (map second arr)) (count arr))
  ])

(defn std-dev [samples]
    (let [n (count samples)
            mean (/ (reduce + samples) n)
            intermediate (map #(Math/pow (- %1 mean) 2) samples)]
              (Math/sqrt 
                     (/ (reduce + intermediate) n))))    
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

(fuzzy/levenshtein "bangers" "bangerz")
(averageArrays (smartAverage (wordSentiment "sick") 1))
(averageArrays (wordSentiment "hot") )
