(ns dda.c4k-website.uberjar
  (:gen-class)
  (:require
   [dda.c4k-common.uberjar :as uberjar]
   [dda.c4k-website.website :as website]
   [dda.c4k-website.core :as core]))


(defn -main [& cmd-args]
  (uberjar/main-common
   "c4k-website"
   core/config?
   core/auth?
   website/config-defaults
   core/k8s-objects
   cmd-args))
