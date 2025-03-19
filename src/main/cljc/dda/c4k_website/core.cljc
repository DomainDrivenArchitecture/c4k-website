(ns dda.c4k-website.core
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.monitoring :as mon]
   [dda.c4k-common.namespace :as ns]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-website.website :as web]
   [dda.c4k-common.yaml :as yaml]))

(s/def ::mon-cfg ::mon/mon-cfg)
(s/def ::mon-auth ::mon/mon-auth)
(s/def ::unique-name ::web/unique-name)
(s/def ::issuer ::web/issuer)
(s/def ::volume-size ::web/volume-size)

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

(def config-defaults {:namespace "web"
                      :issuer "staging"})

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

(defn-spec config-objects cp/map-or-seq?
  [config config?]
  (let [resolved-config (merge config-defaults config)]
    (map yaml/to-string
         (filter
          #(not (nil? %))
          (cm/concat-vec
           (ns/generate resolved-config)
           [(web/generate-content-pvc resolved-config)
            (web/generate-nginx-deployment resolved-config)
            (web/generate-nginx-configmap resolved-config)
            (web/generate-nginx-service resolved-config)
            (web/generate-content-pvc resolved-config)
            (web/generate-hash-state-pvc resolved-config)
            (web/generate-build-cron resolved-config)
            (web/generate-build-configmap resolved-config)]
           (web/generate-ingress resolved-config)
           (when (:contains? resolved-config :mon-cfg)
             (mon/generate-config)))))))

(defn-spec auth-objects cp/map-or-seq?
  [config config?
   auth auth?]
  (cm/concat-vec
   (map yaml/to-string
        (filter #(not (nil? %))
                (cm/concat-vec
                 (ns/generate config auth)
                 (when (:contains? config :mon-cfg)
                   (mon/generate-auth (:mon-cfg config) (:mon-auth auth))))))))
