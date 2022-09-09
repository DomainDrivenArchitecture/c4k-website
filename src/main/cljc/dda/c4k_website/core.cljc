(ns dda.c4k-website.core
 (:require
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]
  [dda.c4k-website.website :as website]
  [dda.c4k-common.postgres :as postgres]))

(defn k8s-objects [config]
  (let [storage-class (if (contains? config :postgres-data-volume-path) :manual :local-path)]
    (cm/concat-vec
     (map yaml/to-string
          (filter #(not (nil? %))
                  [(postgres/generate-config {:postgres-size :2gb :db-name "website"})
                   (postgres/generate-secret config)
                   (when (contains? config :postgres-data-volume-path)
                     (postgres/generate-persistent-volume (select-keys config [:postgres-data-volume-path :pv-storage-size-gb])))
                   (postgres/generate-pvc {:pv-storage-size-gb 5
                                           :pvc-storage-class-name storage-class})
                   (postgres/generate-deployment {:postgres-image "postgres:14"
                                                  :postgres-size :2gb})
                   (postgres/generate-service)
                   (website/generate-deployment)
                   (website/generate-service)
                   (website/generate-service-ssh)                   
                   (website/generate-data-volume config)
                   (website/generate-appini-env config)
                   (website/generate-secrets config)
                   (website/generate-ingress config)
                   (website/generate-certificate config)])))))
