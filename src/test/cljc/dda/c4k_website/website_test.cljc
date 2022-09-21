(ns dda.c4k-website.website-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.test-helper :as th]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-website.website :as cut]))


(st/instrument `cut/generate-certificate)
(st/instrument `cut/generate-ingress)
(st/instrument `cut/generate-nginx-configmap)
(st/instrument `cut/generate-website-content-volume)

(deftest should-generate-certificate
  (is (= {:name-c2 "prod", :name-c1 "staging"}
         (th/map-diff (cut/generate-certificate {:fqdn "test.de"})
                      (cut/generate-certificate {:issuer "prod"                                                 
                                                 :fqdn "test.de"})))))

(deftest should-generate-ingress
  (is (= {:hosts-c1 "test.de",
          :hosts-c2 "test.com",
          :host-c1 "test.de", 
          :host-c2 "test.com"}
         (th/map-diff (cut/generate-ingress {:fqdn "test.de"
                                             })
                      (cut/generate-ingress {:fqdn "test.com"
                                             })))))

(deftest should-generate-nginx-configmap
  (is (= {:server_name-c1 "test.de",
          :server_name-c2 "test.com"}
         (th/map-diff (cut/generate-appini-env {:fqdn "test.de"                                                
                                                })
                      (cut/generate-appini-env {:fqdn "test.com"                                                
                                                })))))

(deftest should-generate-website-content-volume
  (is (= {:storage-c1 "2Gi",
          :storage-c2 "10Gi"}
         (th/map-diff (cut/generate-website-content-volume {:volume-total-storage-size 10
                                                            :number-of-websites 5})
                      (cut/generate-website-content-volume {:volume-total-storage-size 50
                                                            :number-of-websites 5})))))