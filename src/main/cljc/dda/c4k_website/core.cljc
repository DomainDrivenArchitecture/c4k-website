(ns dda.c4k-website.core
 (:require
  [clojure.spec.alpha :as s]
  [dda.c4k-common.yaml :as yaml]
  [dda.c4k-common.common :as cm]
  [dda.c4k-website.website :as website]))

(def config-defaults {:issuer "staging"})

(def config? (s/keys :req-un [::website/fqdn
                              ::website/single
                              ::website/multi
                              ::website/fqdn1
                              ::website/fqdn2]
                     :opt-un [::website/issuer]))

(def auth? (s/keys  :req-un [::website/authtoken 
                             ::website/gitrepourl]))
(defn set-single-repo-url
  [config]
  (assoc config :gitrepourl (:singlegitrepourl config)))

(defn set-multi-fqdn ; Sets the first value of :multi to be the name giving fqdn
  [config]  
  (assoc config :fqdn (keyword ((keyword (first (:multi config))) config))) config)

(defn set-single-fqdn ; Sets the value of :single to be the name giving fqdn
  [config]
  (assoc config :fqdn ((keyword (:single config)) config)))


(defn k8s-objects [config]
  (cm/concat-vec
   (map yaml/to-string
        (filter #(not (nil? %))
                [; multi-case
                 (website/generate-nginx-deployment (set-multi-fqdn config))
                 (website/generate-multi-nginx-configmap config)
                 (website/generate-nginx-service (set-multi-fqdn config))
                 (website/generate-website-content-volume (set-multi-fqdn config))
                 (website/generate-multi-ingress config)
                 (website/generate-multi-certificate config)
                 (website/generate-website-build-cron (set-multi-fqdn config))
                 (website/generate-website-build-secret (set-multi-fqdn config))
                 ; single case
                 (website/generate-nginx-deployment (set-single-fqdn config))
                 (website/generate-single-nginx-configmap config)
                 (website/generate-nginx-service (set-single-fqdn config))
                 (website/generate-website-content-volume (set-single-fqdn config))
                 (website/generate-single-ingress config)
                 (website/generate-single-certificate config)
                 (website/generate-website-build-cron (set-single-repo-url (set-single-fqdn config)))
                 (website/generate-website-build-secret (set-single-repo-url (set-single-fqdn config)))]))))

; read config, 
; 
; when multi not empty
; call multi-functions and set value of key :fqdn to first value of key of list of :multi
; then call general functions with modified input
; if single empty, return nil for any single function
; else call single-functions and set value of key :fqdn to value of key of key :single
; then call general functions with modified input