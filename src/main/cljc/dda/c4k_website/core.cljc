(ns dda.c4k-website.core
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.predicate :as pred]
   [dda.c4k-website.website :as website]))

(def config-defaults {:issuer "staging"
                      :volume-size "3"})

(def merged-config-and-auth? (s/and website/config? website/auth?))

(defn-spec sort-config pred/map-or-seq?
  [unsorted-config merged-config-and-auth?]
  (let [sorted-websites (into [] (sort-by :unique-name (unsorted-config :websites)))
        sorted-auth (into [] (sort-by :unique-name (unsorted-config :auth)))]
    (-> unsorted-config
        (assoc-in [:websites] sorted-websites)
        (assoc-in [:auth] sorted-auth))))

(defn-spec flatten-and-reduce-config  pred/map-or-seq?
  [config merged-config-and-auth?]
  (merge (-> config :websites first)
         (-> config :auth first)
         (when (contains? config :issuer)
           {:issuer (config :issuer)})
         (when (contains? config :volume-size)
           {:volume-size (config :volume-size)})))

(defn generate-configs [config]
  (loop [config (sort-config config)
         result []]

    (if (and (empty? (config :auth)) (empty? (config :websites)))
      result
      (recur (->
              config
              (assoc-in  [:websites] (rest (config :websites)))
              (assoc-in  [:auth] (rest (config :auth))))
             (conj result
                   (website/generate-nginx-deployment (flatten-and-reduce-config config))
                   (website/generate-nginx-configmap (flatten-and-reduce-config config))
                   (website/generate-nginx-service (flatten-and-reduce-config config))
                   (website/generate-website-content-volume (flatten-and-reduce-config config))
                   (website/generate-website-ingress (flatten-and-reduce-config config))
                   (website/generate-website-certificate (flatten-and-reduce-config config))
                   (website/generate-website-build-cron (flatten-and-reduce-config config))
                   (website/generate-website-initial-build-job (flatten-and-reduce-config config))
                   (website/generate-website-build-secret (flatten-and-reduce-config config)))))))

(defn k8s-objects [config]
  (cm/concat-vec
   (map yaml/to-string
        (filter #(not (nil? %))
                (generate-configs config)))))
