(ns dda.c4k-website.core
 (:require
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]
  [dda.c4k-website.website :as website]))

(defn k8s-objects [config]  
    (cm/concat-vec
     (map yaml/to-string
          [(website/generate-nginx-deployment)
           (website/generate-nginx-configmap config)
           (website/generate-nginx-service)
           (website/generate-website-content-volume config)
           (website/generate-ingress config)
           (website/generate-certificate config)
           ])))
