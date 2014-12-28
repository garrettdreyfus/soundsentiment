(ns sciencefair.core
   (:require [sciencefair.soundcloud :refer :all]))

(def settings {:client-id "86d37185f1e5dcdb022276e7f9801ac3" :client-secret "3089b64302a442d9667b2e63368541b3"})

(defn main
  "I don't do a whole lot."
  []
  (let [ x (tracks settings {"genres" "brostep"})]
        (doseq [i x]
        (println i)))
  ;(println (map #(% :body) (tracks settings (str 167444670) "comments")))
  
)
