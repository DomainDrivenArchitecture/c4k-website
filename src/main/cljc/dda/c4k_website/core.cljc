(ns dda.c4k-website.core
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.monitoring :as mon]
   [dda.c4k-common.namespace :as ns]
   [dda.c4k-common.ingress :as ing]
   [dda.c4k-website.website :as web]))

(s/def ::mon-cfg ::mon/mon-cfg)
(s/def ::mon-auth ::mon/mon-auth)
(s/def ::unique-name ::web/unique-name)
(s/def ::issuer ::web/issuer)
(s/def ::volume-size ::web/volume-size)
(s/def ::average-rate ::ing/average-rate)
(s/def ::burst-rate ::ing/burst-rate)

(s/def ::authtoken ::web/authtoken)
(s/def ::fqdns ::web/fqdns)
(s/def ::forgejo-host ::web/forgejo-host)
(s/def ::repo-owner ::web/repo-owner)
(s/def ::repo-name ::web/repo-name)
(s/def ::branchname ::web/branchname)
(s/def ::build-cpu-request ::web/build-cpu-request)
(s/def ::build-memory-request ::web/build-memory-request)
(s/def ::build-cpu-limit ::web/build-cpu-limit)
(s/def ::build-memory-limit ::web/build-memory-limit)
(s/def ::redirects ::web/redirects)

(def websiteconfig? (s/keys :req-un [::unique-name
                                     ::fqdns
                                     ::forgejo-host
                                     ::repo-owner
                                     ::repo-name
                                     ::branchname]
                            :opt-un [::issuer
                                     ::volume-size
                                     ::build-cpu-request
                                     ::build-cpu-limit
                                     ::build-memory-request
                                     ::build-memory-limit
                                     ::redirects]))
(def websiteauth? web/websiteauth?)
(def websiteauths? (s/keys :req-un [::websiteauths]))

(s/def ::websiteconfigs (s/coll-of websiteconfig?))
(s/def ::websiteauths (s/coll-of websiteauth?))

(def config? (s/keys :req-un [::websiteconfigs]
                     :opt-un [::issuer
                              ::volume-size
                              ::mon-cfg
                              ::average-rate
                              ::burst-rate]))

(def auth? (s/keys :req-un [::websiteauths]
                   :opt-un [::mon-auth]))

(def config-defaults {:issuer "staging"})


(def website-config-defaults {:build-cpu-request "500m"
                              :build-cpu-limit "1700m"
                              :build-memory-request "256Mi"
                              :build-memory-limit "512Mi"
                              :volume-size "3"
                              :redirects []
                              :average-rate 20
                              :burst-rate 40})

(defn-spec sort-config map?
  [unsorted-config config?]
  (let [sorted-websiteconfigs (into [] (sort-by :unique-name (unsorted-config :websiteconfigs)))]
    (-> unsorted-config
        (assoc-in [:websiteconfigs] sorted-websiteconfigs))))

(defn-spec sort-auth map?
  [unsorted-auth auth?]
  (let [sorted-auth (into [] (sort-by :unique-name (unsorted-auth :websiteauths)))]
    (-> unsorted-auth
        (assoc-in [:websiteauths] sorted-auth))))

(defn-spec flatten-and-reduce-config map?
  [config config?]
  (let
   [first-entry (first (:websiteconfigs config))]
    (conj first-entry
          (when (contains? config :issuer)
            {:issuer (config :issuer)})
          (when (contains? config :volume-size)
            {:volume-size (config :volume-size)})
          (when (contains? config :average-rate)
            {:average-rate (config :average-rate)})
          (when (contains? config :burst-rate)
            {:burst-rate (config :burst-rate)}))))

(defn-spec flatten-and-reduce-auth map?
  [auth auth?]
  (-> auth :websiteauths first))

(defn-spec generate-ingress seq?
  [config websiteconfig?]
  (let [name (web/replace-dots-by-minus (:unique-name config))
        final-config (merge website-config-defaults
                            {:service-name name
                             :service-port 80
                             :namespace name}
                            config)]
    (ing/generate-simple-ingress final-config)))

(defn-spec generate seq?
  [config config?
   auth auth?]
  (loop [sorted-config (sort-config config)
         sorted-auth (sort-auth auth)
         result []]

    (if (and (empty? (sorted-config :websiteconfigs)) (empty? (sorted-auth :websiteauths)))
      result
      (recur (->
              sorted-config
              (assoc-in  [:websiteconfigs] (rest (sorted-config :websiteconfigs))))
             (->
              sorted-auth
              (assoc-in  [:websiteauths] (rest (sorted-auth :websiteauths))))
             (let [curr-flat-websiteconfig
                   (merge
                    website-config-defaults
                    (flatten-and-reduce-config sorted-config))
                   name (web/replace-dots-by-minus (:unique-name curr-flat-websiteconfig))]
               (cm/concat-vec
                result
                (ns/generate (merge {:namespace name} curr-flat-websiteconfig))
                [(web/generate-nginx-deployment curr-flat-websiteconfig)
                 (web/generate-nginx-configmap curr-flat-websiteconfig)
                 (web/generate-nginx-service curr-flat-websiteconfig)
                 (web/generate-content-pvc curr-flat-websiteconfig)
                 (web/generate-hash-state-pvc curr-flat-websiteconfig)
                 (web/generate-build-cron curr-flat-websiteconfig)
                 (web/generate-build-configmap curr-flat-websiteconfig)
                 (web/generate-build-secret (flatten-and-reduce-auth sorted-auth))]
                (generate-ingress curr-flat-websiteconfig)))))))

(defn-spec k8s-objects cp/map-or-seq?
  [config config?
   auth auth?]
  (cm/concat-vec
   (map yaml/to-string
        (filter
         #(not (nil? %))
         (cm/concat-vec
          (generate config auth)
          (when (:contains? config :mon-cfg)
            (mon/generate (:mon-cfg config) (:mon-auth auth))))))))
