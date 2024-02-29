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
   [dda.c4k-website.website.website-internal :as int]))

(s/def ::mon-cfg ::mon/mon-cfg)
(s/def ::mon-auth ::mon/mon-auth)
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
(s/def ::websites (s/coll-of websiteconfig?))
(s/def ::auth (s/coll-of websiteauth?))

(def config? (s/keys :req-un [::websites]
                     :opt-un [::issuer
                              ::volume-size
                              ::mon-cfg]))

(def auth? (s/keys :req-un [::auth]
                   :opt-un [::mon-auth]))

(def config-defaults {:issuer "staging"})


(def website-config-defaults {:build-cpu-request "500m"
                              :build-cpu-limit "1700m"
                              :build-memory-request "256Mi"
                              :build-memory-limit "512Mi"
                              :volume-size "3"
                              :redirects []})

(defn-spec sort-config map?
  [unsorted-config config?]
  (let [sorted-websites (into [] (sort-by :unique-name (unsorted-config :websites)))]
    (-> unsorted-config
        (assoc-in [:websites] sorted-websites))))

(defn-spec sort-auth map?
  [unsorted-auth auth?]
  (let [sorted-auth (into [] (sort-by :unique-name (unsorted-auth :auth)))]
    (-> unsorted-auth
        (assoc-in [:auth] sorted-auth))))

(defn-spec flatten-and-reduce-config map?
  [config config?]
  (let
   [first-entry (first (:websites config))]
    (conj first-entry
          (when (contains? config :issuer)
            {:issuer (config :issuer)})
          (when (contains? config :volume-size)
            {:volume-size (config :volume-size)}))))

(defn-spec flatten-and-reduce-auth map?
  [auth auth?]
  (-> auth :auth first))

(defn-spec generate-ingress seq?
  [config websiteconfig?]
  (let [name (int/replace-dots-by-minus (:unique-name config))
        final-config (merge website-config-defaults
                            {:service-name name
                             :service-port 80
                             :namespace name}
                            config)]
    (ing/generate-simple-ingress final-config)))

(defn-spec generate seq?
  [config config?
   auth auth?]
  (loop [config (sort-config config)
         sorted-auth (sort-auth auth)
         result []]

    (if (and (empty? (config :websites)) (empty? (sorted-auth :auth)))
      result
      (recur (->
              config
              (assoc-in  [:websites] (rest (config :websites))))
             (->
              auth
              (assoc-in  [:auth] (rest (sorted-auth :auth))))
             (let [final-config
                   (merge
                    website-config-defaults
                    (flatten-and-reduce-config config))
                   name (int/replace-dots-by-minus (:unique-name final-config))]
               (cm/concat-vec
                result
                (ns/generate (merge {:namespace name} final-config))
                [(int/generate-nginx-deployment final-config)
                 (int/generate-nginx-configmap final-config)
                 (int/generate-nginx-service final-config)
                 (int/generate-content-pvc final-config)
                 (int/generate-hash-state-pvc final-config)
                 (int/generate-build-cron final-config)
                 (int/generate-build-secret final-config
                                            (flatten-and-reduce-auth auth))]
                (generate-ingress final-config)))))))

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
