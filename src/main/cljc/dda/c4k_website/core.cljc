(ns dda.c4k-website.core
 (:require
  [clojure.spec.alpha :as s]
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]
  [dda.c4k-website.website :as website]))

(def config-defaults {:issuer "staging"})

(s/def ::websites vector?)
(s/def ::auth vector?)

(def config? (s/keys :req-un [::websites]
                     :opt-un [::website/issuer]))

(def auth? (s/keys  :req-un [::auth]))

(defn flatten-and-reduce-config
  [config]
  (merge (-> config :websites first) (-> config :auth first) {:issuer (config :issuer)}))

(defn find-needle [needle haystack]
  ;loop binds initial values once,
  ;then binds values from each recursion call
  (loop [needle needle
         maybe-here haystack
         not-here '()]

    (let [needle? (first maybe-here)]

      ;test for return or recur
      (if (or (= (str needle?) (str needle))
              (empty? maybe-here))

        ;return results
        [needle? maybe-here not-here]

        ;recur calls loop with new values
        (recur needle
               (rest maybe-here)
               (concat not-here (list (first maybe-here))))))))

(defn generate-configs [config]
  (loop [config config
         result []]

    (if (and (empty? (config :auth)) (empty? (config :websites)))
      result
      (recur (->
              config
              (assoc-in  [:websites] (rest (config :websites)))
              (assoc-in  [:auth] (rest (config :auth))))
             (merge result
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
                [(generate-configs config)]))))
