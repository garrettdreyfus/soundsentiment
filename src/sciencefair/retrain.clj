(ns sciencefair.retrain
     (:require  [sciencefair.sentiment :refer :all] 
                [clojure.string :as str ]
                [clojure.test :as test]
                [clojure.core.reducers :as r]
                [monger.core :as mg]
                [clojure.java.io :as io]
                [monger.collection :as mc]
                [monger.search :as ms]
                [monger.query :as mq]
                [monger.operators :refer :all]
                [fuzzy-string.core :as fuzzy]
                [monger.conversion :refer [from-db-object]])

     (:gen-class)
    (:import [uk.ac.wlv.sentistrength SentiStrength] )
    (:import [com.mongodb MongoOptions ServerAddress])
    (:import [org.bson.types ObjectId] [com.mongodb DB WriteConcern]))

(def settings {:client-id "86d37185f1e5dcdb022276e7f9801ac3" :client-secret "3089b64302a442d9667b2e63368541b3"})
(def conn (mg/connect))
(def db (mg/get-db conn "monger-test"))
(def blacklist 
  (str/split "about below in spite of regarding above beneath instead of  since according to  beside  into  through across  between like  throughout after beyond  near  to against but of  toward along by  off under amid  concerning  on  underneath among down  on account of until around  during  onto  up at  except  out upon atop  for out of  with because of  from  outside within before  in  over  without behind  inside  past i" #"\s"))
(defn abs [n] (max n (- n)))

(defn split-on-space
  [word] 
    (str/split word #"\s"))

(defn normalize 
    [string]
    (if-not string
        " "
        (str/lower-case (str/join (str/split string #" "))) ))


(defn extract-line
  ([line howmany]
  (take howmany (->> line 
    split-on-space 
    (filter #(not (str/blank? %))))))
  ([line]
    (->> line 
        split-on-space 
        (filter #(not (str/blank? %))))))

(defn store-score
  [line-value]
  (let [dict {"word" (first line-value) "score" (second line-value)}]
    (if (empty? (mc/find-maps db "words" {"word" (first line-value)}))
      (mc/insert-and-return db "words" dict)
      (mc/update db "words" {:word (first line-value)} dict ) 
    )))

(defn file-to-db 
  [filepath db]
  (with-open [rdr (io/reader filepath)]
      (doseq [line (line-seq rdr)]
            (store-score 
              (extract-line line 2)
              ))))

(defn sign 
  [a b]
  (if-not 
    (= (- a b) 0)
    (/ (- a b) (abs (- a b)))
    0))

(defn smart-quotient
  [x y]
  (abs (cond 
    (> x y) (/ x y)
    (> y x) (/ y x)
    (= y x) 1)))

(defn scale-score
  [[pos neg]]
  (let [sig (sign pos neg) quotient (smart-quotient pos neg) threshold {1 1, 1.5 2, 2 3} ]
     (int (* sig (threshold (last (filter #(>= quotient %) (keys threshold))))))
  ))

(defn insert-word
  "If Its not in the databse and a urbandictionary call exists "
  [word]
  (test/is (instance? String word))
  (if 
    (and (empty? (ms/results-from (ms/search db "words" word))) (empty? (mc/find-maps db "words" {"word" word})) (empty? (mc/find-maps db "words" {"word" (str word "*")})))
    (let [sent (averageArrays (smartAverage (wordSentiment word) 0.25))]
      (if (not= sent 0) 
        (store-score (vector word (scale-score sent)))))))

(defn update-word
  "If Its not in the databse and a urbandictionary call exists "
  [word]
  (test/is (instance? String word))
    (let [sent (averageArrays (smartAverage (wordSentiment word) 0.25))]
      (if (not= sent 0) 
        (store-score (vector word (scale-score sent))))))

(defn insert-batch-words
  [words]
  (dorun (pmap insert-word words)))

(defn is-in?
  [string]
  ((complement nil?) (some #{string} blacklist)))

(defn pmapcat
    "Like mapcat but with parallelized map."
    [f & colls]
    (apply concat (apply pmap f colls)))

(defn strip [coll chars]
    (apply str (remove #(not ((set chars) %)) (str/lower-case coll))))

(defn parse-com
  [coms]
  (pmapcat #(extract-line (strip % "abcdefghijklmnopqrstuvwxyz ")) coms))

(defn strip-comments
  [s]
  (pmap #(% :body) s))

(defn iterate-through-comments
  ([]
    (dorun (pmap #(insert-batch-words (parse-com (strip-comments (% :comments)))) (mc/find-maps db "documents"))))
  ([taken]
    (pmap #(insert-batch-words (parse-com (strip-comments (% :comments)))) (take taken (mc/find-maps db "documents")))))

(defn iterate-through-words
  ([]
    (dorun (pmap #(update-word (% :word)) (mc/find-maps db "words"))))
  ([taken]
    (dorun (pmap #(update-word (% :word)) (take taken (mc/find-maps db "words"))))))

(defn words-to-file
  [words]
  (with-open [wrtr (io/writer "EmotionLookupTable.txt")]
    (doseq [word words]
      (.write wrtr (str (word :word) " " (word :score) "\n"))
    )
  )
)



