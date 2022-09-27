(ns dda.c4k-website.core
 (:require
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]
  [dda.c4k-website.website :as website]))

(defn k8s-objects [config]  
    (cm/concat-vec
     (map yaml/to-string
          [(website/generate-nginx-deployment config)
           (website/generate-nginx-configmap config)
           (website/generate-nginx-service config)
           (website/generate-website-content-volume config)
           (website/generate-ingress config)
           (website/generate-certificate config)
           (website/generate-website-build-cron config)
           (website/generate-website-build-deployment config)
           (website/generate-website-build-secret config)

           ])))
