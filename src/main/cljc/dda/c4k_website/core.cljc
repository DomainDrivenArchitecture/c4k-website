(ns dda.c4k-website.core
  (:require
   [clojure.spec.alpha :as s]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-website.website :as website]))

(def config-defaults {:issuer "staging"})

(defn flatten-and-reduce-config
  [config]
  (merge (-> config :websites first) (-> config :auth first) {:issuer (config :issuer)}))

(defn generate-configs [config]
  (loop [config config
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
                   (website/generate-website-http-ingress (flatten-and-reduce-config config))
                   (website/generate-website-https-ingress (flatten-and-reduce-config config))
                   (website/generate-website-certificate (flatten-and-reduce-config config))
                   (website/generate-website-build-cron (flatten-and-reduce-config config))
                   (website/generate-website-build-secret (flatten-and-reduce-config config)))))))

(defn k8s-objects [config]
  (cm/concat-vec
   (map yaml/to-string
        (filter #(not (nil? %))
                (generate-configs config)))))
