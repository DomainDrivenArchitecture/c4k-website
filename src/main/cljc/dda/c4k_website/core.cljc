(ns dda.c4k-website.core
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.monitoring :as mon]
   [dda.c4k-website.website :as website]))

(def config-defaults {:issuer "staging"
                      :volume-size "3"})

(s/def ::mon-cfg mon/config?)
(s/def ::mon-auth mon/auth?)

(def config? (s/keys :req-un [::website/websites]
                     :opt-un [::website/issuer 
                              ::website/volume-size
                              ::mon-cfg]))

(def auth? (s/keys :req-un [::website/auth]
                   :opt-un [::mon-auth]))

(defn-spec sort-config cp/map-or-seq?
  [unsorted-config config?]
  (let [sorted-websites (into [] (sort-by :unique-name (unsorted-config :websites)))]
    (-> unsorted-config
        (assoc-in [:websites] sorted-websites))))

(defn-spec sort-auth cp/map-or-seq?
  [unsorted-auth auth?]
  (let [sorted-auth (into [] (sort-by :unique-name (unsorted-auth :auth)))]
    (-> unsorted-auth
        (assoc-in [:auth] sorted-auth))))

(defn-spec flatten-and-reduce-config  cp/map-or-seq?
  [config config?]
  (let
   [first-entry (first (:websites config))]
    (conj first-entry
           (when (contains? config :issuer)
             {:issuer (config :issuer)})
           (when (contains? config :volume-size)
             {:volume-size (config :volume-size)}))))

(defn-spec flatten-and-reduce-auth  cp/map-or-seq?
  [auth auth?]
  (-> auth :auth first))

(defn generate-configs [config auth]
  (loop [config (sort-config config)
         auth (sort-auth auth)
         result []]

    (if (and (empty? (config :websites)) (empty? (auth :auth)))
      result
      (recur (->
              config
              (assoc-in  [:websites] (rest (config :websites))))
             (->
              auth
              (assoc-in  [:auth] (rest (auth :auth))))
             (conj result
                   (website/generate-nginx-deployment (flatten-and-reduce-config config))
                   (website/generate-nginx-configmap (flatten-and-reduce-config config))
                   (website/generate-nginx-service (flatten-and-reduce-config config))
                   (website/generate-website-content-volume (flatten-and-reduce-config config))
                   (website/generate-hashfile-volume (flatten-and-reduce-config config))
                   (website/generate-website-ingress (flatten-and-reduce-config config))
                   (website/generate-website-certificate (flatten-and-reduce-config config))
                   (website/generate-website-build-cron (flatten-and-reduce-config config))
                   (website/generate-website-build-secret (flatten-and-reduce-config config) (flatten-and-reduce-auth auth)))))))

(defn-spec k8s-objects cp/map-or-seq?
  [config config?
   auth auth?]  
  (cm/concat-vec
   (map yaml/to-string
        (filter
         #(not (nil? %))
         (cm/concat-vec
          (generate-configs config auth)
          (when (:contains? config :mon-cfg)
            (mon/generate (:mon-cfg config) (:mon-auth auth))))))))
