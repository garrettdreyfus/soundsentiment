(ns sciencefair.core
   (:require [sciencefair.soundcloud :refer :all] [clojure.string :as str ])
   (:gen-class)
  (:import [uk.ac.wlv.sentistrength SentiStrength] ))

(def analyzer (new uk.ac.wlv.sentistrength.SentiStrength (into-array ["sentidata" "./src/sentistrength/" "text" "initialize+this+string"] )))
(defn scoreComment
  [comment]
   (str/split (.computeSentimentScores analyzer comment #"\s"))
)

(def settings {:client-id "86d37185f1e5dcdb022276e7f9801ac3" :client-secret "3089b64302a442d9667b2e63368541b3"})

(defn get-attrs
 "Takes a list of maps and an attribute and returns a list of all those values"
 [items attr]
 (map #(% attr) items))

(defn get-comments
 "Takes a list or vector of ids and returns a map of ids to comments"
 [ids]
 (let [comments {}]
        (map #(assoc comments % (get-attrs (tracks settings {} % "comments") :body)) ids)
  )
)
(defn analyze-sentiment
 [a]
 (println "hey")
 1.0
 )
(defn sentize
 [comments]
 (let [sentized {}]
        (map #(assoc sentized (first %) (map analyze-sentiment (second %)))
         comments)
 ))
" For genre sentiment
        download them all with sampling search take only ids using get-attrs
        get-comments
        run sentiment store values
        ----
        genre
        followers"

(defn -main
  "I don't do a whole lot."
  [& args]
  (get-attrs (tracks settings {"genres" "Hip Hop" "order" "created_at" "limit" "1"}) :id)
  ;(println (sentize (get-comments '(123504279 61278329 142285485))))
  ;(println (map #(% :body) (tracks settings (str 167444670) "comments")))
)
