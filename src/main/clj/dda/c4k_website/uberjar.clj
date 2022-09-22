(ns dda.c4k-website.uberjar
  (:gen-class)
  (:require
   [dda.c4k-website.core :as core]
   [dda.c4k-website.website :as website]
   [dda.c4k-common.uberjar :as uberjar]))


(defn -main [& cmd-args]
  (uberjar/main-common "c4k-website" website/config? website/auth? website/config-defaults core/k8s-objects cmd-args))
