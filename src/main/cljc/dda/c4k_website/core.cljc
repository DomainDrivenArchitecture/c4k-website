(ns dda.c4k-website.core
 (:require
  [clojure.spec.alpha :as s]
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]
  [dda.c4k-website.website :as website]))

(def config-defaults {:issuer "staging"})

(def config? (s/keys :req-un [::website/fqdn]
                     :opt-un [::website/issuer]))

(def auth? (s/keys  :req-un [::website/authtoken 
                             ::website/gitrepourl]))

(def vol? (s/keys :req-un [::website/volume-total-storage-size
                           ::website/number-of-websites]))

(defn k8s-objects [config]
  (cm/concat-vec
   (map yaml/to-string
        (filter #(not (nil? %))
                [(website/generate-nginx-deployment config)
                 (website/generate-nginx-configmap config)
                 (website/generate-nginx-service config)
                 (website/generate-website-content-volume config)
                 (website/generate-ingress config)
                 (website/generate-certificate config)
                 (website/generate-website-build-cron config)
                 (website/generate-website-build-secret config)]))))
