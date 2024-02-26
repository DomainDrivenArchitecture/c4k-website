(ns dda.c4k-website.website
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-website.website.website-internal :as int]))

(s/def ::unique-name ::int/unique-name)
(s/def ::issuer ::int/issuer)
(s/def ::volume-size ::int/volume-size)
(s/def ::authtoken ::int/authtoken)
(s/def ::fqdns ::int/fqdns)
(s/def ::forgejo-host ::int/forgejo-host)
(s/def ::forgejo-repo ::int/forgejo-repo)
(s/def ::branchname ::int/branchname)
(s/def ::username ::int/username)
(s/def ::build-cpu-request ::int/build-cpu-request)
(s/def ::build-memory-request ::int/build-memory-request)
(s/def ::build-cpu-limit ::int/build-cpu-limit)
(s/def ::build-memory-limit ::int/build-memory-limit)

(def websiteconfig? (s/keys :req-un [::unique-name
                                     ::fqdns
                                     ::forgejo-host
                                     ::forgejo-repo
                                     ::branchname]
                            :opt-un [::issuer
                                     ::volume-size
                                     ::build-cpu-request
                                     ::build-cpu-limit
                                     ::build-memory-request
                                     ::build-memory-limit]))

(def websiteauth? (s/keys :req-un [::unique-name ::username ::authtoken]))

(def config-defaults {:issuer "staging"
                      :build-cpu-request "500m" 
                      :build-cpu-limit "1700m" 
                      :build-memory-request "256Mi" 
                      :build-memory-limit "512Mi"
                      :volume-size "3"})

(defn-spec generate-nginx-deployment map?
  [config websiteconfig?]
  (let [final-config (merge config-defaults
                            config)]
    (int/generate-nginx-deployment final-config)))
