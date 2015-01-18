(defproject sciencefair "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"] 
                  [clj-http "1.0.1"]
                   [cheshire "5.2.0"]
                  [com.soundcloud/java-api-wrapper "1.3.1"]
                [ring/ring-servlet "1.2.0-RC1"]
                 [uk.ac.wlv.sentistrength/sentistrength "0.0"]
                 [fuzzy-string "0.1.2-SNAPSHOT"]
                 [com.novemberain/monger "2.0.0"]
  ]
  :plugins [
            [lein-localrepo "0.4.0"]]

  :main sciencefair.core
)
